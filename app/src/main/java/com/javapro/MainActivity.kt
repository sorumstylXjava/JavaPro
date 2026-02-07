package com.javapro

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // TAMBAHKAN INI
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.javapro.ui.MainScreen
import com.javapro.ui.screens.SplashScreen
import com.javapro.ui.theme.JavaProTheme
import com.javapro.utils.PreferenceManager
import com.javapro.utils.TweakManager

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Izin Diperlukan", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // AKTIFKAN MODE FULL SCREEN (EDGE-TO-EDGE)
        enableEdgeToEdge()
        
        super.onCreate(savedInstanceState)
        
        // PENGATURAN AGAR KONTEN MASUK KE AREA NOTCH (PONI)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Inisialisasi TweakManager
        TweakManager.init(this)

        // Cek Izin Notifikasi & Usage Stats
        checkNotificationPermission()

        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // Cek Mode Performa Terakhir
        val isPerfModeActive = TweakManager.getPrefs(this).getBoolean(TweakManager.KEY_PERFORMANCE_MODE, false)
        if (isPerfModeActive) {
            TweakManager.setPerformanceMode(this, true)
        }

        setContent {
            val prefManager = remember { PreferenceManager(this) }
            val fpsEnabled by prefManager.fpsEnabledFlow.collectAsState(initial = false)

            LaunchedEffect(fpsEnabled) {
                if (fpsEnabled) {
                    if (Settings.canDrawOverlays(this@MainActivity)) {
                        startService(Intent(this@MainActivity, FpsService::class.java))
                    } else {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                        prefManager.setFpsEnabled(false)
                        Toast.makeText(this@MainActivity, "Izinkan Overlay untuk menampilkan FPS", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    stopService(Intent(this@MainActivity, FpsService::class.java))
                }
            }

            JavaProTheme(prefManager = prefManager) {
                Surface {
                    val mainNavController = rememberNavController()

                    NavHost(navController = mainNavController, startDestination = "splash_route") {
                        composable("splash_route") {
                            SplashScreen(
                                onLoadingFinished = {
                                    mainNavController.navigate("main_route") {
                                        popUpTo("splash_route") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("main_route") {
                            MainScreen(prefManager)
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
