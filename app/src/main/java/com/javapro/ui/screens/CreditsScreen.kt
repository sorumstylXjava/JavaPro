package com.javapro.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.javapro.R

// Data Class Sederhana untuk Kontributor
data class Contributor(
    val name: String,
    val username: String, // Username telegram tanpa @
    val imageRes: Int // ID Gambar dari drawable
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(navController: NavController) {
    val context = LocalContext.current

    // --- DAFTAR KONTRIBUTOR ---
    // Ganti R.drawable.xxx dengan nama file foto yang kamu masukkan ke folder drawable
    val contributors = listOf(
        Contributor("Anomaly Arc", "anomaly_arc", R.drawable.profile_anomaly),
        Contributor("Fahrezone", "fahrezone", R.drawable.profile_fahrez),
        Contributor("Kanagawa Yamada", "KanagawaYamadaVTeacher", R.drawable.profile_kanagawa),
        Contributor("Diky", "Nekotor1999", R.drawable.profile_diky)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Credits", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            
            // ==========================================
            // --- BAGIAN KHUSUS DEVELOPER (BARU) ---
            // ==========================================
            Text(
                text = "Developer",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                // Menggunakan warna container secondary agar terlihat beda/spesial
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // FOTO PROFIL DEVELOPER
                    Box(
                        modifier = Modifier
                            .size(70.dp) // Sedikit lebih besar dari user biasa
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.onSecondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // PENTING: Pastikan ada file 'profile_developer' di folder drawable 
                        // atau ganti dengan R.drawable.profile_kanagawa (sebagai contoh sementara)
                        Image(
                            painter = painterResource(id = R.drawable.profile_developer), // Ganti ini dengan foto dev
                            contentDescription = "Developer",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    // NAMA DEVELOPER (Tanpa Username/Tombol)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Java_nih_deks",
                            style = MaterialTheme.typography.titleLarge, // Font lebih besar
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Main Developer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // --- PEMBATAS / DIVIDER ---
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(24.dp))
            // ==========================================


            // --- HEADER UCAPAN TERIMAKASIH (EXISTING) ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Special Thanks",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Terima kasih sebesar-besarnya kepada para kontributor hebat yang telah membantu pengembangan project ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // --- LIST ORANG (EXISTING) ---
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(contributors) { person ->
                    ContributorItem(person) {
                        // Aksi saat tombol ditekan: Buka Telegram
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/${person.username}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback jika tidak ada browser/telegram
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContributorItem(data: Contributor, onLinkClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FOTO PROFIL BULAT
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = data.imageRes),
                    contentDescription = data.name,
                    contentScale = ContentScale.Crop, // Agar foto terpotong rapi memenuhi lingkaran
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(16.dp))

            // NAMA & TOMBOL
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                
                // Tombol Username Kecil
                Button(
                    onClick = onLinkClick,
                    shape = RoundedCornerShape(50), // Bulat lonjong
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = "@${data.username}", fontSize = 12.sp)
                }
            }
        }
    }
}
