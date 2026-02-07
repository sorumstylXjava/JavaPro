package com.javapro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.javapro.utils.TweakExecutor
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onLoadingFinished: () -> Unit) {
    // State untuk menyimpan hasil cek root (opsional, bisa ditampilkan saat loading)
    var isRootedState by remember { mutableStateOf<Boolean?>(null) }

    // Logika "Loading" berjalan di sini
    LaunchedEffect(Unit) {
        // 1. (Opsional) Cek Root di background saat loading
        // Ini membuat waktu tunggu terasa berguna
        val rootCheck = TweakExecutor.checkRoot()
        isRootedState = rootCheck
        
        // 2. Tambahkan delay buatan agar logo terlihat beberapa detik.
        // Ubah angkanya (misal 2000L untuk 2 detik) sesuai kebutuhan.
        delay(3000L) 

        // 3. Panggil fungsi callback bahwa loading selesai
        onLoadingFinished()
    }

    // --- DESAIN UI SPLASH SCREEN ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Menggunakan warna background dari tema aplikasi (gelap/terang otomatis)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Teks JavaPro Besar
            Text(
                text = "JavaPro",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary // Warna utama aplikasi
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animasi Loading Putar
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.tertiary, // Warna aksen loading
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // (Opsional) Teks kecil status di bawah loading
            Text(
                text = if (isRootedState == null) "Checking system..." else "System ready!",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
