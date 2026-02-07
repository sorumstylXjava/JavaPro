package com.javapro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.javapro.utils.GameListManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(darkTheme: Boolean) {
    val context = LocalContext.current
    // State untuk daftar game agar UI update otomatis saat ditambah/hapus
    var gameList by remember { mutableStateOf(GameListManager.getGameList(context)) }
    
    // State untuk Dialog
    var showDialog by remember { mutableStateOf(false) }
    var newPkg by remember { mutableStateOf("") }

    val bgColor = if (darkTheme) Color(0xFF0D0D0D) else Color.White
    val textColor = if (darkTheme) Color.White else Color.Black
    val cardColor = if (darkTheme) Color(0xFF1E1E1E) else Color(0xFFF0F0F0)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFFA05A2C), // Warna oranye khas XianTian
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Game")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Game List Manager",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Masukkan package name game",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gameList) { pkg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pkg,
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(onClick = {
                                GameListManager.removeGame(context, pkg)
                                gameList = GameListManager.getGameList(context) // Refresh UI
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Tambah Game
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tambah Package Game") },
            text = {
                Column {
                    Text("Contoh: com.mobile.legends", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPkg,
                        onValueChange = { newPkg = it },
                        label = { Text("Package Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPkg.isNotBlank()) {
                        GameListManager.addGame(context, newPkg.trim())
                        gameList = GameListManager.getGameList(context) // Refresh UI
                        newPkg = ""
                        showDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Batal") }
            }
        )
    }
}
