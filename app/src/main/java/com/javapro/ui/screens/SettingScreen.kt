package com.javapro.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.javapro.utils.PreferenceManager
import com.javapro.utils.TweakExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingScreen(pref: PreferenceManager, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- STATE MANAGEMENT ---
    
    // 1. Observe Bahasa secara Real-time
    val currentLang by pref.languageFlow.collectAsState(initial = "en")

    // 2. Observe Data Permanen Lainnya
    val isDark by pref.darkModeFlow.collectAsState()
    val isBootActive by pref.bootApplyFlow.collectAsState()
    val savedScale by pref.scaleValFlow.collectAsState()
    val savedRed by pref.redValFlow.collectAsState()
    val savedGreen by pref.greenValFlow.collectAsState()
    val savedBlue by pref.blueValFlow.collectAsState()
    val savedSat by pref.satValFlow.collectAsState()

    // 3. State Lokal UI untuk Slider
    var sliderScale by remember { mutableFloatStateOf(savedScale) }
    var redUI by remember { mutableFloatStateOf(savedRed) }
    var greenUI by remember { mutableFloatStateOf(savedGreen) }
    var blueUI by remember { mutableFloatStateOf(savedBlue) }
    var satUI by remember { mutableFloatStateOf(savedSat) }

    // Sinkronisasi data awal saat layar dibuka
    LaunchedEffect(savedScale) { sliderScale = savedScale }
    LaunchedEffect(savedRed, savedGreen, savedBlue, savedSat) {
        redUI = savedRed; greenUI = savedGreen; blueUI = savedBlue; satUI = savedSat
    }

    // --- LOGIKA REAL-TIME APPLY (Saturasi & RGB) ---
    LaunchedEffect(redUI, greenUI, blueUI, satUI) {
        delay(300) 
        if (redUI != 0f && satUI != 0f) { 
            TweakExecutor.applyColorModifier(redUI, greenUI, blueUI, satUI)
            pref.setRGB(redUI, greenUI, blueUI)
            pref.setSat(satUI)
        }
    }

    // --- WARNA & TEMA ---
    val cardBgColor = Color(0xFF1E1E1E) 
    val accentYellow = Color(0xFFCDBD76) 
    val buttonBrown = Color(0xFF5D4037)
    val buttonRed = Color(0xFFB71C1C) 

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. HEADER & DARK MODE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentLang == "id") "Mode Gelap" else "Dark Mode",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(
                checked = isDark,
                onCheckedChange = { pref.setDarkMode(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentYellow
                )
            )
        }

        // --- 2. DOWNSCALE RESOLUTION ---
        Text(
            text = if (currentLang == "id") "Resolusi Layar" else "Downscale Resolution",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = accentYellow,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport, 
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Slider(
                        value = sliderScale,
                        onValueChange = { sliderScale = it },
                        valueRange = 0.5f..1.0f,
                        steps = 9,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = accentYellow,
                            activeTrackColor = accentYellow,
                            inactiveTrackColor = Color.DarkGray
                        )
                    )
                    
                    Text(
                        text = "${(sliderScale * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentYellow
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        sliderScale = 1.0f
                        scope.launch {
                            TweakExecutor.resetResolution() 
                            pref.setScale(1.0f) 
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(if (currentLang == "id") "Reset Resolusi" else "Reset Resolution")
                }

                TextButton(
                    onClick = {
                        scope.launch {
                            TweakExecutor.applyGlobalResolution(context, sliderScale)
                            pref.setScale(sliderScale)
                            Toast.makeText(context, if (currentLang=="id") "Resolusi Diterapkan" else "Resolution Applied", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentLang == "id") "Terapkan Resolusi" else "Apply Resolution", color = accentYellow)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- 3. SCREEN MODIFIER (Real-time Apply) ---
        Text(
            "Screen Modifier",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = accentYellow,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                CustomColorRow("Red", redUI, Color.Red) { redUI = it }
                CustomColorRow("Green", greenUI, Color.Green) { greenUI = it }
                CustomColorRow("Blue", blueUI, Color.Blue) { blueUI = it }
                
                Spacer(Modifier.height(8.dp))
                // PERBAIKAN: Menggunakan HorizontalDivider (Bukan Divider)
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                
                CustomColorRow("Saturation", satUI, accentYellow, range = 2000f) { satUI = it }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Apply on Boot", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            if(currentLang=="id") "Otomatis terapkan saat reboot" else "Auto-apply after reboot", 
                            color = Color.Gray, fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isBootActive,
                        onCheckedChange = { pref.setBootApply(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.Gray
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        redUI = 1000f; greenUI = 1000f; blueUI = 1000f; satUI = 1000f
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Reset Colors")
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        // --- 4. SUPPORT & COMMUNITY ---
        Text(
            if (currentLang == "id") "Dukungan & Komunitas" else "Support & Community",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFFEDB9B9),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sociabuzz.com/javakids/tribe"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonBrown),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Support My Project", color = Color.White, fontSize = 14.sp)
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Java_diks"))
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Telegram", color = Color.White) }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Java_nih_deks"))
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Report Bug", color = Color.White) }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { navController.navigate("credits") },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.People, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Credits & Developer", color = Color.White)
        }
        
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val newLang = if (currentLang == "id") "en" else "id"
                scope.launch {
                    pref.setLanguage(newLang) 
                }
            },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (currentLang == "id") "Switch to English" else "Ganti ke Indonesia",
                color = Color.White
            )
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                scope.launch {
                    sliderScale = 1.0f
                    redUI = 1000f; greenUI = 1000f; blueUI = 1000f; satUI = 1000f
                    pref.setScale(1.0f)
                    pref.setRGB(1000f, 1000f, 1000f)
                    pref.setSat(1000f)
                    pref.setBootApply(false)
                    TweakExecutor.resetResolution() 
                    TweakExecutor.applyColorModifier(1000f, 1000f, 1000f, 1000f) 
                    
                    Toast.makeText(context, if(currentLang=="id") "Semua Pengaturan Direset" else "All Settings Reset", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonRed),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (currentLang == "id") "Reset Semua Pengaturan" else "Reset All Settings", 
                color = Color.White, 
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun CustomColorRow(
    label: String, 
    value: Float, 
    color: Color, 
    range: Float = 1000f,
    onValueChange: (Float) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            color = Color.White, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp),
            fontSize = 14.sp
        )
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color.DarkGray
            )
        )
        
        Text(
            text = "${value.toInt()}",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold
        )
    }
}
