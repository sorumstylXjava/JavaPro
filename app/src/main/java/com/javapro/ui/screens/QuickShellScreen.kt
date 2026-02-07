package com.javapro.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.javapro.shell.ShellDaemon
import java.io.File

/* ===== COLORS (ASLI) ===== */
private val TerminalBg = Color(0xFF0D0D0D)
private val TerminalInputBg = Color(0xFF1E1E1E)
private val TerminalAccent = Color(0xFFA05A2C)
private val TerminalText = Color(0xFFEEEEEE)
private val TerminalError = Color(0xFFFF5555)
private val TerminalSystem = Color(0xFF00ADB5)

@Composable
fun QuickShellScreen() {

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }
    val logs = remember { mutableStateListOf<LogEntry>() }

    val daemon = remember {
        ShellDaemon(
            onOutput = { logs.add(LogEntry("OUT", it)) },
            onError = { logs.add(LogEntry("ERR", it)) }
        )
    }

    LaunchedEffect(Unit) {
        daemon.start()
    }

    DisposableEffect(Unit) {
        onDispose { daemon.stop() }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    Scaffold(
        containerColor = TerminalBg,
        floatingActionButton = {
            FloatingActionButton(
                containerColor = TerminalAccent,
                onClick = {
                    if (hasStoragePermission(context)) {
                        saveLogs(context, logs)
                    } else {
                        requestStoragePermission(context)
                    }
                }
            ) {
                Icon(Icons.Default.Save, null)
            }
        }
    ) { pad ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(14.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("QuickShell", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                Row {
                    IconButton(onClick = {
                        logs.clear()
                        daemon.restart()
                        logs.add(LogEntry("SYS", "--- Restarted ---"))
                    }) {
                        Icon(Icons.Default.Refresh, null)
                    }

                    IconButton(onClick = { logs.clear() }) {
                        Icon(Icons.Default.Delete, null)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Surface(
                color = TerminalInputBg,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(TerminalAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (input.isNotBlank()) {
                                    logs.add(LogEntry("IN", "$ $input"))
                                    daemon.exec(input)
                                    input = ""
                                }
                            }
                        )
                    )

                    IconButton(onClick = {
                        if (input.isNotBlank()) {
                            logs.add(LogEntry("IN", "$ $input"))
                            daemon.exec(input)
                            input = ""
                        }
                    }) {
                        Icon(Icons.Default.PlayArrow, null)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState
            ) {
                items(logs) { log ->
                    Text(
                        log.message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        clipboard.setText(AnnotatedString(log.message))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = when (log.type) {
                            "ERR" -> TerminalError
                            "SYS" -> TerminalSystem
                            "IN" -> TerminalAccent
                            else -> TerminalText
                        }
                    )
                }
            }
        }
    }
}

/* ===== MODEL ===== */
data class LogEntry(val type: String, val message: String)

/* ===== STORAGE ===== */
private fun saveLogs(context: Context, logs: List<LogEntry>) {
    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "QuickShell_${System.currentTimeMillis()}.log")
    file.writeText(logs.joinToString("\n") { it.message })
    Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
}

private fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun requestStoragePermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        )
    }
}