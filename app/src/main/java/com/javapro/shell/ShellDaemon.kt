package com.javapro.shell

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class ShellDaemon(
    private val onOutput: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var running = false

    fun start() {
        if (running) return
        running = true

        scope.launch {
            try {
                val builder = try {
                    ProcessBuilder("su")
                } catch (_: Exception) {
                    ProcessBuilder("sh")
                }

                process = builder.start()
                writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))

                onOutput("--- Shell Daemon Started ---")

                scope.launch {
                    process!!.inputStream.bufferedReader().forEachLine {
                        onOutput(it)
                    }
                }

                scope.launch {
                    process!!.errorStream.bufferedReader().forEachLine {
                        onError(it)
                    }
                }

            } catch (e: Exception) {
                onError("Daemon error: ${e.message}")
            }
        }
    }

    fun exec(cmd: String) {
        scope.launch {
            try {
                writer?.apply {
                    write(cmd)
                    write("\n")
                    flush()
                }
            } catch (e: Exception) {
                onError("Exec error: ${e.message}")
            }
        }
    }

    fun stop() {
        try {
            writer?.write("exit\n")
            writer?.flush()
            process?.destroy()
        } catch (_: Exception) {}
        running = false
    }

    fun restart() {
        stop()
        start()
    }
}