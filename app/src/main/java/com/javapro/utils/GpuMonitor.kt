package com.javapro.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

object GpuMonitor {

    private val FPS_NODES = listOf(
        "/sys/class/drm/sde-crtc-0/measured_fps",    
        "/sys/class/graphics/fb0/measured_fps",      
        "/sys/class/video/fps_info",                 
        "/sys/devices/platform/k3_fb/fps_info",      
        "/sys/kernel/debug/fps_log"                
    )

    private var activeFpsNode: String? = null

    suspend fun findValidFpsNode(): Boolean {
        return withContext(Dispatchers.IO) {
            for (path in FPS_NODES) {
                val exists = RootUtils.executeCommand("test -e $path && echo exists")
                if (exists.contains("exists")) {
                    activeFpsNode = path
                    Log.d("JavaPro_FPS", "FPS Node Found: $path")
                    return@withContext true
                }
            }
            Log.e("JavaPro_FPS", "No hardware FPS node found!")
            return@withContext false
        }
    }

    suspend fun getRealFps(): Int {
        val node = activeFpsNode ?: return 0
        
        return withContext(Dispatchers.IO) {
            try {
                val rawValue = RootUtils.executeCommand("cat $node").trim()
                
                parseFpsOutput(rawValue)
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    private fun parseFpsOutput(text: String): Int {
        if (text.isEmpty()) return 0
        val cleanText = text.replace(Regex("[^0-9.]"), "")
        return try {
            cleanText.toFloat().toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }
}
