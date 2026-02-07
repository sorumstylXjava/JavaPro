package com.javapro.utils

import java.io.DataOutputStream
import java.io.IOException

object RootUtils {

    // Fungsi sederhana untuk menjalankan 1 perintah Root
    fun execute(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su") // Meminta akses Root
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Fungsi untuk menjalankan BANYAK perintah sekaligus (List)
    fun execute(commands: List<String>): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            for (cmd in commands) {
                os.writeBytes("$cmd\n") 
            }
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            true
        } catch (e: Exception) {
            false
        }
    }
}
            