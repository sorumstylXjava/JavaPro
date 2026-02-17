package com.javapro.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.javapro.R
import com.javapro.services.GameBoosterService
import com.javapro.utils.AppProfileManager
import com.javapro.utils.PreferenceManager
import com.javapro.utils.TweakExecutor
import com.javapro.utils.TweakManager

@Composable
fun HomeScreen(prefManager: PreferenceManager, lang: String, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isRooted = remember { TweakExecutor.checkRoot() }
    val info = remember { TweakExecutor.getDeviceInfo(context) }
    
    val isPerfModeActive by TweakManager.isPerformanceActive.collectAsState()
    val fpsEnabled by prefManager.fpsEnabledFlow.collectAsState(initial = false)
    val realFps by GameBoosterService.fpsFlow.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(180.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.banner),
                contentDescription = "Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(20.dp))

        Card(
            onClick = { navController.navigate("app_profiles") },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Apps, null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        if (lang == "id") "Profil Per Aplikasi" else "App Profiles",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        if (lang == "id") "Atur performa khusus tiap aplikasi" else "Set custom performance per app",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            if (lang == "id") "Informasi Perangkat" else "Device Info",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoItem(Icons.Default.PhoneAndroid, "Model", info["Model"] ?: "Unknown", Modifier.weight(1f))
                    InfoItem(Icons.Default.Android, "Android", info["Android"] ?: "Unknown", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoItem(Icons.Default.Memory, "Chipset", info["Chipset"] ?: "Unknown", Modifier.weight(1f))
                    InfoItem(Icons.Default.DeveloperBoard, "Kernel", info["Kernel"] ?: "Unknown", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoItem(Icons.Default.SdStorage, "RAM", info["RAM"] ?: "-- GB", Modifier.weight(1f))
                    InfoItem(Icons.Default.BatteryFull, "Battery", info["Battery"] ?: "--%", Modifier.weight(1f))
                }
                Divider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isRooted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isRooted) Color.Green else Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isRooted) "Root Access Granted" else "No Root Access",
                        fontWeight = FontWeight.Bold,
                        color = if (isRooted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("FPS Monitor (Real)", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (!fpsEnabled) "Disabled" else if (realFps > 0) "$realFps FPS" else "Scanning...",
                            fontSize = 14.sp,
                            color = if (realFps >= 50) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = fpsEnabled,
                    onCheckedChange = { isChecked ->
                        prefManager.setFpsEnabled(isChecked)
                        val intent = Intent(context, GameBoosterService::class.java).apply {
                            putExtra("ACTION_TYPE", if (isChecked) "START_FPS" else "STOP_FPS")
                        }
                        if (isChecked) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            Toast.makeText(context, "FPS Monitor On", Toast.LENGTH_SHORT).show()
                        } else {
                            context.startService(intent)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            if (lang == "id") "Mode Cepat" else "Quick Modes",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val unifiedButtonColors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Button(
                onClick = {
                    TweakManager.setPerformanceMode(context, true)
                    AppProfileManager.applyProfileTweak(context, "performance")
                    Toast.makeText(context, "Mode Performance", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                enabled = isRooted,
                colors = unifiedButtonColors,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Performance", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = {
                    TweakManager.setPerformanceMode(context, false)
                    AppProfileManager.applyProfileTweak(context, "balance")
                    Toast.makeText(context, "Mode Balance (Normal)", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                enabled = isRooted,
                shape = RoundedCornerShape(12.dp),
                colors = unifiedButtonColors,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Balance", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = {
                    TweakManager.setPerformanceMode(context, false)
                    AppProfileManager.applyProfileTweak(context, "powersave")
                    Toast.makeText(context, "Mode Battery Save", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                enabled = isRooted,
                shape = RoundedCornerShape(12.dp),
                colors = unifiedButtonColors,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Save Battery", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun InfoItem(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label, 
                fontSize = 10.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value, 
                fontWeight = FontWeight.Bold, 
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
