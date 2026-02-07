package com.javapro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.javapro.utils.AppInfo
import com.javapro.utils.AppProfileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfileScreen(navController: NavController, lang: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var searchQuery by remember { mutableStateOf("") }
    var appList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Function untuk load/refresh aplikasi
    fun loadApps() {
        scope.launch {
            isLoading = true
            try {
                val apps = withContext(Dispatchers.IO) {
                    AppProfileManager.getInstalledApps(context)
                }
                appList = apps
                isLoading = false
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        }
    }

    // Load Apps saat pertama kali
    LaunchedEffect(Unit) {
        loadApps()
    }

    // Filter Logic
    val filteredList = remember(searchQuery, appList) {
        if (searchQuery.isEmpty()) {
            appList
        } else {
            appList.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.packageName.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (lang == "id") "Daftar Aplikasi" else "App List") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // TOMBOL REFRESH di pojok kanan atas
                    IconButton(
                        onClick = {
                            if (!isLoading) {
                                loadApps()
                                Toast.makeText(
                                    context, 
                                    if (lang == "id") "Memperbarui..." else "Refreshing...", 
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (lang == "id") "Perbarui" else "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(if (lang == "id") "Cari aplikasi..." else "Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // --- CONTENT ---
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(if (lang == "id") "Memuat aplikasi..." else "Loading apps...")
                    }
                }
            } else if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) {
                            if (lang == "id") "Tidak ada aplikasi" else "No apps found"
                        } else {
                            if (lang == "id") "Pencarian tidak ditemukan" else "No search results"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Tampilkan jumlah aplikasi
                Text(
                    text = if (lang == "id") {
                        "${filteredList.size} aplikasi"
                    } else {
                        "${filteredList.size} apps"
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filteredList,
                        key = { it.packageName }
                    ) { app ->
                        AppItemCard(
                            app = app,
                            lang = lang,
                            onClick = {
                                selectedApp = app
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG PILIH PROFILE ---
    if (showDialog && selectedApp != null) {
        val app = selectedApp!!
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { 
                Column {
                    Text(app.name, maxLines = 1)
                    Text(
                        text = app.packageName, 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }
            },
            text = {
                Column {
                    Text(
                        if (lang == "id") "Pilih Mode:" else "Select Mode:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val modes = listOf("balance", "performance", "powersave")
                    
                    modes.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        // Set Profile
                                        AppProfileManager.setAppProfile(context, app.packageName, mode)
                                        
                                        // Update List Lokal
                                        appList = appList.map { 
                                            if (it.packageName == app.packageName) {
                                                it.copy(profile = mode)
                                            } else {
                                                it
                                            }
                                        }
                                        
                                        val message = if (lang == "id") {
                                            "$mode diterapkan untuk ${app.name}"
                                        } else {
                                            "$mode applied for ${app.name}"
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        showDialog = false
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = app.profile == mode, 
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(mode.uppercase(), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { 
                    Text(if (lang == "id") "Tutup" else "Close") 
                }
            }
        )
    }
}

@Composable
fun AppItemCard(
    app: AppInfo,
    lang: String = "en",
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon dengan error handling yang lebih baik
            val iconBitmap = remember(app.packageName) {
                try {
                    app.icon.toBitmap(64, 64).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }

            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap, 
                    contentDescription = null, 
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                // Fallback icon
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.name.take(1).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Text(
                    text = app.packageName, 
                    fontSize = 11.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Badge Profile
                val (label, color) = when(app.profile) {
                    "performance" -> {
                        val text = if (lang == "id") "PERFORMA" else "PERFORMANCE"
                        text to MaterialTheme.colorScheme.error
                    }
                    "powersave" -> {
                        val text = if (lang == "id") "HEMAT" else "POWERSAVE"
                        text to MaterialTheme.colorScheme.secondary
                    }
                    else -> {
                        val text = if (lang == "id") "SEIMBANG" else "BALANCE"
                        text to MaterialTheme.colorScheme.primary
                    }
                }
                
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = label, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
