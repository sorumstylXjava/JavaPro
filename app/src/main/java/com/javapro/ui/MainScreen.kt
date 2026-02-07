package com.javapro.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import com.javapro.ui.screens.*
import com.javapro.utils.PreferenceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(prefManager: PreferenceManager) {
    val navController = rememberNavController()
    
    // Observer global untuk bahasa dan tema
    val lang by prefManager.languageFlow.collectAsState("en")
    val darkTheme by prefManager.darkModeFlow.collectAsState(initial = true)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val items = listOf(
                    Triple("home", Icons.Default.Home, if (lang == "id") "Beranda" else "Home"),
                    Triple("gamelist", Icons.Default.List, if (lang == "id") "Game" else "Gamelist"),
                    Triple("tweaks", Icons.Default.Build, if (lang == "id") "Fitur" else "Tweaks"),
                    Triple("terminal", Icons.Default.Terminal, if (lang == "id") "QuickShell" else "QuickShell"),
                    Triple("settings", Icons.Default.Settings, if (lang == "id") "Pengaturan" else "Settings")
                )

                items.forEach { (route, icon, label) ->
                    NavigationBarItem(
                        icon = { Icon(icon, null) },
                        label = { 
                            Text(
                                text = label,
                                fontSize = 10.sp, 
                                maxLines = 1,      
                                softWrap = false,  
                                overflow = TextOverflow.Ellipsis 
                            )
                        },
                        alwaysShowLabel = false, 
                        selected = currentRoute == route,
                        onClick = {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding) 
        ) {
            composable("home") { 
                HomeScreen(prefManager, lang, navController) 
            }
            
            composable("app_profiles") {
                AppProfileScreen(navController, lang)
            }
            
            composable("gamelist") {
                GameListScreen(darkTheme)
            }
            
            composable("tweaks") { 
                TweakScreen(darkTheme, lang) 
            }
            
            composable("terminal") {
                QuickShellScreen() 
            }
            
            // PERBAIKAN UTAMA DISINI: Menghapus parameter 'lang'
            composable("settings") { 
                SettingScreen(prefManager, navController) 
            }
            
            composable("credits") {
                CreditsScreen(navController)
            }
        }
    }
}
