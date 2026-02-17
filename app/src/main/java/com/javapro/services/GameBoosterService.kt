package com.javapro.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.javapro.R
import com.javapro.utils.GameListManager
import com.javapro.utils.GpuMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameBoosterService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val NOTIF_ID = 1

    companion object {
        private val _fpsFlow = MutableStateFlow(0)
        val fpsFlow = _fpsFlow.asStateFlow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, createNotification("Mode Performance on", "Waiting for game..."))
        startGameMonitoring()
        startFpsMonitoring()
    }

    private fun startFpsMonitoring() {
        serviceScope.launch {
            val found = GpuMonitor.findValidFpsNode()
            if (found) {
                while (isActive) {
                    val fps = GpuMonitor.getRealFps()
                    _fpsFlow.emit(fps)
                    delay(1000)
                }
            }
        }
    }

    private fun startGameMonitoring() {
        serviceScope.launch {
            var lastApp = ""
            while (isActive) {
                val games = GameListManager.getGameList(this@GameBoosterService)
                val topApp = getTopAppName()
                
                if (topApp != lastApp) {
                    if (topApp.isNotEmpty() && games.contains(topApp)) {
                        updateNotification("Mode Performance on", "Active: $topApp")
                    } else {
                        updateNotification("Mode Performance on", "Waiting for game...")
                    }
                    lastApp = topApp
                }
                delay(3000)
            }
        }
    }

    private fun updateNotification(title: String, msg: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID, createNotification(title, msg))
    }

    private fun createNotification(title: String, content: String): android.app.Notification {
        return NotificationCompat.Builder(this, "GameBoosterChannel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false) 
            .setAutoCancel(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "GameBoosterChannel", 
                "Game Booster Status", 
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun getTopAppName(): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "dumpsys window | grep mCurrentFocus"))
            val reader = process.inputStream.bufferedReader()
            val line = reader.readLine() ?: ""
            
            if (line.contains("/")) {
                val valPart = line.split(" ").lastOrNull() ?: ""
                if (valPart.contains("/")) {
                    val pkg = valPart.split("/")[0]
                    return pkg.substringAfterLast("{").trim()
                }
            }
            ""
        } catch (e: Exception) { "" }
    }

    override fun onDestroy() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIF_ID)
        serviceScope.cancel()
        super.onDestroy()
    }
}
