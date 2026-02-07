package com.javapro.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val profile: String 
)

object AppProfileManager {
    private const val PREF_PROFILE = "App_Profiles"

    fun getAppProfile(context: Context, packageName: String): String {
        val prefs = context.getSharedPreferences(PREF_PROFILE, Context.MODE_PRIVATE)
        return prefs.getString(packageName, "balance") ?: "balance"
    }

    fun setAppProfile(context: Context, packageName: String, mode: String) {
        val prefs = context.getSharedPreferences(PREF_PROFILE, Context.MODE_PRIVATE)
        prefs.edit().putString(packageName, mode).apply()
        applyProfileTweak(context, mode) 
    }

    /**
     * PUSAT LOGIKA: Jawa Profile + Thermal + Hard Lock Frequency
     * FIXED: Menggunakan shell execution yang lebih kuat agar tidak di-reset sistem
     */
    fun applyProfileTweak(context: Context, mode: String) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val cmds = mutableListOf<String>()

            when (mode) {
                "performance" -> {
                    // --- 1. STOP THERMAL ENGINE (Langkah Pertama) ---
                    cmds.addAll(getThermalStopCommands())

                    // --- 2. CPU GOVERNOR & FREQUENCY LOCK (Anti-Drop) ---
                    // Menggunakan 'sh -c' agar loop for dan wildcard '*' diproses oleh shell
                    cmds.add("sh -c 'for cpu in /sys/devices/system/cpu/cpu*/cpufreq; do " +
                            "echo performance > \$cpu/scaling_governor; " +
                            "cat \$cpu/cpuinfo_max_freq > \$cpu/scaling_max_freq; " +
                            "cat \$cpu/cpuinfo_max_freq > \$cpu/scaling_min_freq; " +
                            "done'")

                    // --- 3. GPU PERFORMANCE LOCK ---
                    cmds.add("sh -c 'echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor'")
                    cmds.add("sh -c 'echo 0 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel'") // Force Max Power (0 = Max)
                    cmds.add("sh -c 'echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on'")
                    cmds.add("sh -c 'echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on'")

                    // --- 4. UCLAMP TUNING (Priority Processing) ---
                    cmds.add("echo 100 > /dev/cpuctl/foreground/cpu.uclamp.min")
                    cmds.add("echo 100 > /dev/cpuctl/foreground/cpu.uclamp.max")
                    cmds.add("echo 1024 > /dev/cpuctl/foreground/cpu.shares")

                    // Virtual Memory (Ultra Low Latency)
                    cmds.add("echo 0 > /proc/sys/vm/swappiness") 
                    cmds.add("echo 10 > /proc/sys/vm/vfs_cache_pressure")
                    cmds.add("echo 1 > /proc/sys/kernel/sched_child_runs_first")
                    cmds.add("echo 0 > /proc/sys/kernel/sched_autogroup_enabled")

                    // --- 5. SYSTEM TWEAKS ---
                    cmds.add("dumpsys deviceidle disable") 
                    cmds.add("echo 3 > /proc/sys/vm/drop_caches") 
                }
                
                "powersave" -> {
                    cmds.addAll(getThermalStartCommands())
                    cmds.add("sh -c 'for cpu in /sys/devices/system/cpu/cpu*/cpufreq; do " +
                            "echo powersave > \$cpu/scaling_governor; " +
                            "cat \$cpu/cpuinfo_min_freq > \$cpu/scaling_min_freq; " +
                            "done'")

                    cmds.add("echo 10 > /dev/cpuctl/foreground/cpu.uclamp.max")
                    cmds.add("echo 100 > /proc/sys/vm/swappiness")
                    cmds.add("dumpsys deviceidle force-idle")
                }
                
                else -> { // "balance"
                    cmds.addAll(getThermalStartCommands())
                    cmds.add("sh -c 'for cpu in /sys/devices/system/cpu/cpu*/cpufreq; do " +
                            "echo schedutil > \$cpu/scaling_governor; " +
                            "cat \$cpu/cpuinfo_min_freq > \$cpu/scaling_min_freq; " +
                            "done'")

                    cmds.add("echo 40 > /proc/sys/vm/swappiness")
                    cmds.add("dumpsys deviceidle enable")
                }
            }

            // Eksekusi semua perintah
            cmds.forEach { cmd ->
                TweakExecutor.execute(cmd)
            }
        }
    }

    private fun getThermalStopCommands(): List<String> {
        return listOf(
            "stop thermal-engine",
            "stop thermald",
            "setprop init.svc.vendor.thermal-hal-2-0.mtk stopped",
            "setprop init.svc.vendor.thermal-hal-1-0 stopped",
            "setprop init.svc.thermal-engine stopped",
            "setprop ctl.stop thermal-engine",
            "setprop vendor.thermal.mode.disable 1"
        )
    }

    private fun getThermalStartCommands(): List<String> {
        return listOf(
            "start thermal-engine",
            "start thermald",
            "setprop vendor.thermal.mode.disable 0",
            "setprop ctl.start thermal-engine"
        )
    }

    /**
     * FIX: Fungsi loading aplikasi yang lebih fleksibel
     * - Menampilkan semua aplikasi user (bukan sistem)
     * - Menampilkan aplikasi sistem yang memiliki launcher icon
     * - Kompatibel dengan berbagai versi Android
     */
    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val appList = mutableListOf<AppInfo>()

        try {
            // Ambil semua packages dengan flag 0 (lebih cepat)
            val packages = pm.getInstalledPackages(0)

            for (pkg in packages) {
                try {
                    val appInfo = pkg.applicationInfo ?: continue
                    
                    // Cek apakah aplikasi sistem
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    
                    // Filter: Tampilkan aplikasi user ATAU aplikasi sistem yang punya launcher
                    val hasLauncherIntent = pm.getLaunchIntentForPackage(pkg.packageName) != null
                    
                    // Tampilkan jika:
                    // 1. Bukan aplikasi sistem (user app) ATAU
                    // 2. Aplikasi sistem yang punya launcher icon
                    if (!isSystemApp || hasLauncherIntent) {
                        val label = try {
                            appInfo.loadLabel(pm).toString()
                        } catch (e: Exception) {
                            pkg.packageName // Fallback ke package name
                        }
                        
                        val icon = try {
                            appInfo.loadIcon(pm)
                        } catch (e: Exception) {
                            pm.defaultActivityIcon // Fallback ke icon default
                        }
                        
                        val currentProfile = getAppProfile(context, pkg.packageName)

                        appList.add(AppInfo(label, pkg.packageName, icon, currentProfile))
                    }
                } catch (e: Exception) {
                    // Skip aplikasi yang error saat di-load
                    continue
                }
            }
        } catch (e: Exception) {
            // Jika terjadi error, kembalikan list kosong
            return@withContext emptyList()
        }

        // Sort berdasarkan nama (A-Z)
        appList.sortedBy { it.name.lowercase() }
    }
}
