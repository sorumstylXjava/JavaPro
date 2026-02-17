package com.javapro.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    // Fungsi internal sebagai pengganti executeCommand yang error
    private fun runShell(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine() ?: ""
            process.destroy()
            output
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun findValidFpsNode(): Boolean {
        return withContext(Dispatchers.IO) {
            for (path in FPS_NODES) {
                val check = runShell("if [ -f $path ]; then echo exists; fi")
                if (check.contains("exists")) {
                    activeFpsNode = path
                    return@withContext true
                }
            }
            return@withContext false
        }
    }

    suspend fun getRealFps(): Int {
        val node = activeFpsNode ?: return 0
        return withContext(Dispatchers.IO) {
            val rawValue = runShell("cat $node")
            parseFpsOutput(rawValue)
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
