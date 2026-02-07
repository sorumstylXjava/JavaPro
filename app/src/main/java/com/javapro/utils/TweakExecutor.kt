package com.javapro.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object TweakExecutor {

    // --- FUNGSI SYSTEM & ROOT CHECK ---
    
    fun checkRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine()?.contains("uid=0") == true
        } catch (e: Exception) { false }
    }

    suspend fun execute(command: String) = withContext(Dispatchers.IO) {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", command)).waitFor()
        } catch (e: Exception) { }
    }

    // [DIPERBAIKI] Fungsi ini dimurnikan agar output bersih untuk dibaca FpsService
    suspend fun executeWithOutput(command: String): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            // Baca output baris per baris
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
            // Kita HAPUS bagian append exit code agar string murni data saja
            output.toString()
        } catch (e: Exception) { "" }
    }

    fun getDeviceInfo(context: Context): Map<String, String> {
        val metrics = context.resources.displayMetrics
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        
        val chipset = if (Build.HARDWARE != "unknown") Build.HARDWARE else Build.BOARD
        val kernel = System.getProperty("os.version") ?: "Unknown"

        return mapOf(
            "Model" to (Build.MODEL ?: "Unknown"),
            "Android" to "Android ${Build.VERSION.RELEASE}",
            "Chipset" to chipset.uppercase(),
            "Kernel" to "Kernel $kernel",
            "RAM" to "${(memInfo.totalMem / (1024 * 1024 * 1024)) + 1} GB",
            "Battery" to "$level%"
        )
    }

    // --- FITUR RESOLUSI (TIDAK DIUBAH) ---

    suspend fun resetResolution() {
        execute("wm size reset")
        execute("wm density reset")
    }

    suspend fun applyGlobalResolution(context: Context, scale: Float) {
        if (scale >= 0.99f) {
            resetResolution()
        } else {
            val metrics = DisplayMetrics()
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.getRealMetrics(metrics)
            
            val originalW = metrics.widthPixels
            val originalH = metrics.heightPixels
            val originalDpi = metrics.densityDpi

            val targetW = (originalW * scale).toInt()
            val targetH = (originalH * scale).toInt()
            
            execute("wm size ${targetW}x${targetH}")
            execute("wm density $originalDpi") 
        }
    }

    // --- FITUR WARNA / KCAL (TIDAK DIUBAH) ---

    suspend fun applyColorModifier(rUI: Float, gUI: Float, bUI: Float, satUI: Float) {
        val r = (rUI / 1000f * 255).toInt()
        val g = (gUI / 1000f * 255).toInt()
        val b = (bUI / 1000f * 255).toInt()
        val sat = satUI / 1000f 

        // Saturation via SurfaceFlinger (Android 9/10/11)
        execute("service call SurfaceFlinger 1022 f $sat")
        
        // KCAL Kernel Support
        val rgbString = "$r $g $b"
        execute("echo \"$rgbString\" > /sys/module/msm_drm/parameters/kcal_rgb")
        execute("echo \"$rgbString\" > /sys/devices/platform/kcal_ctrl.0/kcal")
        execute("echo 1 > /sys/devices/platform/kcal_ctrl.0/kcal_enable")
    }
}
