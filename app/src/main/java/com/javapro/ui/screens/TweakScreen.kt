package com.javapro.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.javapro.utils.TweakManager
import com.javapro.utils.TweakExecutor

@Composable
fun TweakScreen(darkTheme: Boolean, lang: String) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Cek Root & State Performance Mode secara real-time
    val isRooted = remember { TweakExecutor.checkRoot() }
    val isPerfModeActive by TweakManager.isPerformanceActive.collectAsState()

    val bgColor = if (darkTheme) Color(0xFF0D0D0D) else Color.White
    val textColor = if (darkTheme) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // --- HEADER ---
        Text(
            text = if (lang == "id") "Pusat Kendali Tweak" else "Tweak Control Center",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- WARNING JIKA TIDAK ROOT ---
        if (!isRooted) {
             Card(
                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), 
                 modifier = Modifier.fillMaxWidth().padding(bottom=16.dp)
             ) {
                 Text(
                     text = if(lang=="id") "Akses Root tidak ditemukan. Semua tweak dinonaktifkan." else "Root Access Not Found. All tweaks disabled.",
                     modifier = Modifier.padding(16.dp), 
                     color = MaterialTheme.colorScheme.onErrorContainer,
                     fontWeight = FontWeight.Bold
                 )
             }
        }

        // ============================================
        // KATEGORI: PERFORMANCE (PERFORMA)
        // ============================================
        TweakCategoryTitle(if (lang == "id") "Performa & CPU" else "Performance & CPU", textColor)

        TweakSwitch(context, "perf_gpu", darkTheme, isRooted, isPerfModeActive,
            if(lang=="id") "Paksa Rendering GPU" else "Force GPU Rendering",
            if(lang=="id") "Alihkan beban UI dari CPU ke GPU" else "Offload UI load from CPU to GPU")

        TweakSwitch(context, "perf_anim", darkTheme, isRooted, isPerfModeActive,
            if(lang=="id") "Matikan Animasi Sistem" else "Disable System Animations",
            if(lang=="id") "Membuat UI terasa instan" else "Makes UI feel instant")

        TweakSwitch(context, "perf_dex2oat", darkTheme, isRooted, isPerfModeActive,
            if(lang=="id") "Dex2Oat Speed" else "Dex2Oat Speed",
            if(lang=="id") "Percepat instalasi & buka aplikasi" else "Faster app install & launch")
            
        TweakSwitch(context, "perf_ram", darkTheme, isRooted, isPerfModeActive,
            if(lang=="id") "Optimasi Manajemen RAM" else "RAM Management Opt",
            if(lang=="id") "Atur low memory killer (LMK)" else "Adjust low memory killer (LMK)")

        Spacer(Modifier.height(16.dp))

        // ============================================
        // KATEGORI: GAMING
        // ============================================
        TweakCategoryTitle(if (lang == "id") "Mode Gaming" else "Gaming Mode", textColor)

        TweakSwitch(context, "game_touch", darkTheme, isRooted, isPerfModeActive,
            if(lang=="id") "Responsivitas Sentuhan" else "Touch Responsiveness",
            if(lang=="id") "Kurangi delay sentuh layar" else "Reduce screen touch delay")

        // KHUSUS: game_fps tidak masuk dalam PERFORMANCE_TWEAKS (di TweakManager) agar bisa dikontrol manual
        TweakSwitch(context, "game_fps", darkTheme, isRooted, isPerfModeActive,
            if(lang=="id") "Unlock High FPS" else "Unlock High FPS",
            if(lang=="id") "Simulasi Samsung Z Fold5 (Butuh Reboot)" else "Simulate Samsung Z Fold5 (Needs Reboot)")
            
        TweakSwitch(context, "game_overlay", darkTheme, isRooted, isPerfModeActive,
            if(lang=="id") "Matikan HW Overlays" else "Disable HW Overlays",
            if(lang=="id") "Komposisi layar via GPU selalu" else "Always use GPU for screen composition")
        
        TweakSwitch(context, "game_thermal", darkTheme, isRooted, isPerfModeActive,
            if(lang=="id") "Matikan Thermal" else "Disable Thermal",
            if(lang=="id") "Hapus batasan suhu (Resiko Panas!)" else "Remove temp limits (Heat Risk!)")

        Spacer(Modifier.height(16.dp))

        // ============================================
        // KATEGORI: BATERAI (BATTERY)
        // ============================================
        TweakCategoryTitle(if (lang == "id") "Baterai & Daya" else "Battery & Power", textColor)

        TweakSwitch(context, "bat_doze", darkTheme, isRooted, isPerfModeActive,
            if(lang=="id") "Doze Agresif" else "Aggressive Doze",
            if(lang=="id") "Deep sleep lebih cepat saat layar mati" else "Faster deep sleep when screen off")

        Spacer(Modifier.height(32.dp))
        
        // Footer Space
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun TweakCategoryTitle(title: String, color: Color) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = color.copy(alpha = 0.8f), 
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun TweakSwitch(
    context: Context,
    key: String, 
    darkTheme: Boolean,
    isRooted: Boolean,         
    isPerfModeActive: Boolean, 
    title: String,
    description: String
) {
    // Mengecek apakah tweak ini dikendalikan otomatis oleh "Performance Mode"
    // Pastikan di TweakManager.kt, "game_fps" sudah dihapus dari daftar PERFORMANCE_TWEAKS
    val isPerfTweak = TweakManager.PERFORMANCE_TWEAKS.contains(key)

    var isChecked by remember { 
        mutableStateOf(TweakManager.isTweakEnabled(context, key)) 
    }

    // Update state switch secara reaktif
    LaunchedEffect(isPerfModeActive) {
        if (isPerfTweak) {
            isChecked = if (isPerfModeActive) true else TweakManager.isTweakEnabled(context, key)
        } else {
            // Jika bukan tweak otomatis (seperti game_fps), tetap gunakan pref user
            isChecked = TweakManager.isTweakEnabled(context, key)
        }
    }

    // Tombol aktif jika:
    // 1. Root tersedia
    // 2. Tweak ini tidak sedang dikunci oleh Performance Mode
    val isLockedByPerf = isPerfModeActive && isPerfTweak
    val isEnabled = isRooted && !isLockedByPerf

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) Color(0xFF1E1E1E) else Color(0xFFF0F0F0)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) 
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = if (darkTheme) Color.White else Color.Black,
                    fontSize = 15.sp
                )
                Text(
                    text = description,
                    color = if (darkTheme) Color.Gray else Color.DarkGray,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
                // Label bantuan jika tweak dikunci oleh sistem
                if (isLockedByPerf) {
                    Text(
                        text = "Auto-Enabled by Performance Mode", 
                        color = Color(0xFFA05A2C), 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Switch(
                checked = isChecked,
                enabled = isEnabled,
                onCheckedChange = { newVal ->
                    isChecked = newVal
                    TweakManager.setTweakState(context, key, newVal)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFA05A2C), 
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = if (darkTheme) Color.Black else Color.LightGray,
                    disabledCheckedTrackColor = Color(0xFF804823),
                    disabledCheckedThumbColor = Color.LightGray
                )
            )
        }
    }
}
