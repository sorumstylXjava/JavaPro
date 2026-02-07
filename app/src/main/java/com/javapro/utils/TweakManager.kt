package com.javapro.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.javapro.services.GameBoosterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object TweakManager {
    private const val PREF_NAME = "JavaPro_Tweaks"
    const val KEY_PERFORMANCE_MODE = "perf_mode"

    // game_fps dihapus dari sini agar TIDAK otomatis aktif saat Mode Performance dinyalakan
    val PERFORMANCE_TWEAKS = listOf(
        "perf_gpu", "perf_anim", "perf_dex2oat", "perf_ram",
        "game_touch", "game_overlay", "game_thermal"
    )

    private val _isPerformanceActive = MutableStateFlow(false)
    val isPerformanceActive: StateFlow<Boolean> = _isPerformanceActive

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        _isPerformanceActive.value = getPrefs(context).getBoolean(KEY_PERFORMANCE_MODE, false)
    }

    fun setTweakState(context: Context, key: String, isEnabled: Boolean) {
        getPrefs(context).edit().putBoolean(key, isEnabled).apply()
        // Jika mode performa aktif, tweak dalam list tidak bisa diubah manual
        if (_isPerformanceActive.value && PERFORMANCE_TWEAKS.contains(key)) {
            return 
        }
        applyTweakLogic(key, isEnabled)
    }

    fun isTweakEnabled(context: Context, key: String): Boolean {
        if (_isPerformanceActive.value && PERFORMANCE_TWEAKS.contains(key)) return true
        return getPrefs(context).getBoolean(key, false)
    }

    fun setPerformanceMode(context: Context, active: Boolean) {
        _isPerformanceActive.value = active
        getPrefs(context).edit().putBoolean(KEY_PERFORMANCE_MODE, active).apply()

        val serviceIntent = Intent(context, GameBoosterService::class.java)
        val scope = CoroutineScope(Dispatchers.IO)

        if (active) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            scope.launch {
                TweakExecutor.execute("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                TweakExecutor.execute("dumpsys deviceidle disable") 
                TweakExecutor.execute("settings put global low_power 0")
                PERFORMANCE_TWEAKS.forEach { key -> applyTweakLogic(key, true) }
            }
        } else {
            context.stopService(serviceIntent)

            scope.launch {
                TweakExecutor.execute("echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                TweakExecutor.execute("dumpsys deviceidle enable")
                PERFORMANCE_TWEAKS.forEach { key ->
                    val userPref = getPrefs(context).getBoolean(key, false)
                    applyTweakLogic(key, userPref)
                }
            }
        }
    }

    fun applyTweakLogic(key: String, enable: Boolean) {
        val cmd = when (key) {
            "perf_gpu" -> if (enable) "setprop debug.composition.type gpu" else "setprop debug.composition.type c2d"
            
            "perf_anim" -> if (enable) 
                "settings put global window_animation_scale 0; settings put global transition_animation_scale 0; settings put global animator_duration_scale 0" 
                else 
                "settings put global window_animation_scale 1; settings put global transition_animation_scale 1; settings put global animator_duration_scale 1"
            
            "perf_dex2oat" -> if (enable) "setprop dalvik.vm.dex2oat-filter speed" else "setprop dalvik.vm.dex2oat-filter speed-profile"
            
            "perf_ram" -> if (enable) "settings put global low_power 0" else "" 
            
            "game_fps" -> if (enable) {
                val props = listOf(
                    "ro.product.brand samsung",
                    "ro.product.manufacturer samsung",
                    "ro.product.model SM-F9460",
                    "ro.product.odm.model SM-F9460",
                    "ro.product.system.model SM-F9460",
                    "ro.product.vendor.model SM-F9460",
                    "ro.product.system_ext.model SM-F9460",
                    "ro.product.vendor.cert SM-F9460",
                    "ro.product.Aliases SM-F9460",
                    "ro.build.tf.modelnumber SM-F9460",
                    "ro.product.device q5q",
                    "ro.build.product q5q",
                    "ro.build.flavor q5q-user",
                    "ro.build.description \"q5q-user 13 TP1A.220624.014 F9460XXU1AWD1 release-keys\"",
                    "ro.product.name \"Galaxy Z Fold5\"",
                    "ro.product.odm.name \"Galaxy Z Fold5\"",
                    "ro.product.vendor.name \"Galaxy Z Fold5\"",
                    "ro.product.system_ext.name \"Galaxy Z Fold5\"",
                    "ro.product.system.name \"Galaxy Z Fold5\"",
                    "ro.product.product.name \"Galaxy Z Fold5\"",
                    "ro.product.marketname \"Galaxy Z Fold5\"",
                    "ro.product.odm.marketname \"Galaxy Z Fold5\"",
                    "ro.product.product.marketname \"Galaxy Z Fold5\"",
                    "ro.product.system.marketname \"Galaxy Z Fold5\"",
                    "ro.product.system_ext.marketname \"Galaxy Z Fold5\"",
                    "ro.product.vendor.marketname \"Galaxy Z Fold5\"",
                    "ro.soc.manufacturer Qualcomm",
                    "ro.soc.model SM8650",
                    "ro.product.board SM8650",
                    "ro.board.platform kalama",
                    "sys.fps_unlock_allowed 120",
                    "persist.sys.pinner.enabled true"
                )
                props.joinToString("; ") { "resetprop $it" }
            } else ""
            
            "game_touch" -> if (enable) "settings put system pointer_speed 7" else "settings put system pointer_speed 1"
            
            // --- FIX: LOGIKA BARU UNTUK SEMUA JENIS THERMAL (MIUI, MTK, SNAPDRAGON) ---
            "game_thermal" -> {
                val services = listOf(
                    "mi_thermald",
                    "vendor.thermal-hal-2-0.mtk",
                    "vendor.thermal.hal",
                    "thermal",
                    "thermal-managers",
                    "thermal_manager",
                    "thermal_mnt_hal_service",
                    "thermal-engine",
                    "thermalloadalgod",
                    "thermalservice",
                    "thermal-hal",
                    "vendor.thermal-symlinks",
                    "android.thermal-hal",
                    "vendor.thermal-hal", // duplicate generic handle
                    "vendor-thermal-hal-1-0",
                    "vendor.thermal-hal-1-0",
                    "vendor.thermal-hal-2-0"
                )

                if (enable) {
                    // STOP THERMAL: Stop service & Set property 'stopped'
                    val cmdBuilder = StringBuilder()
                    
                    // Loop untuk mematikan semua service yang terdaftar
                    services.forEach { s ->
                        cmdBuilder.append("stop $s; setprop init.svc.$s stopped; ")
                    }
                    
                    // Tambahan command spesifik dari request user
                    cmdBuilder.append("setprop dalvik.vm.dexopt.thermal-cutoff 0; ")
                    cmdBuilder.append("echo N > /sys/module/msm_thermal/parameters/enabled")
                    
                    cmdBuilder.toString()
                } else {
                    // RESTORE THERMAL: Start service & Restore property
                    val cmdBuilder = StringBuilder()
                    
                    services.forEach { s ->
                        cmdBuilder.append("start $s; ")
                    }
                    
                    // Restore dexopt thermal
                    cmdBuilder.append("setprop dalvik.vm.dexopt.thermal-cutoff 1; ")
                    cmdBuilder.append("echo Y > /sys/module/msm_thermal/parameters/enabled")
                    
                    cmdBuilder.toString()
                }
            }
            
            "game_overlay" -> if (enable) "service call SurfaceFlinger 1008 i32 1" else "service call SurfaceFlinger 1008 i32 0"
            
            "bat_doze" -> if (enable) "dumpsys deviceidle force-idle" else "dumpsys deviceidle unforce"
            
            else -> ""
        }
        
        if (cmd.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch { TweakExecutor.execute(cmd) }
        }
    }

    fun resetAllTweaks(context: Context) {
        val editor = getPrefs(context).edit()
        PERFORMANCE_TWEAKS.forEach { key ->
            editor.putBoolean(key, false)
            applyTweakLogic(key, false)
        }
        editor.putBoolean("game_fps", false)
        applyTweakLogic("game_fps", false)

        editor.putBoolean("bat_doze", false)
        applyTweakLogic("bat_doze", false)
        
        editor.putBoolean(KEY_PERFORMANCE_MODE, false).apply()
        _isPerformanceActive.value = false
        context.stopService(Intent(context, GameBoosterService::class.java))
    }
}
