package com.orbitx.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class OrbitScreen(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Profiles("Profiles", Icons.Filled.Person),
    Versions("Versions", Icons.Filled.List),
    Controls("Controls", Icons.Filled.Build),
    Settings("Settings", Icons.Filled.Settings),
}

/**
 * App shell. Navigation state is plain Compose state rather than a nav library:
 * there are five flat destinations with no deep linking or back-stack requirements.
 */
@Composable
fun RootScaffold() {
    var screen by remember { mutableStateOf(OrbitScreen.Home) }

    Scaffold(
        containerColor = OrbitBg,
        bottomBar = {
            NavigationBar(containerColor = OrbitSurface, tonalElevation = 0.dp) {
                OrbitScreen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = screen == item,
                        onClick = { screen = item },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrbitMint,
                            selectedTextColor = OrbitMint,
                            indicatorColor = OrbitSurfaceHi,
                            unselectedIconColor = OrbitMuted,
                            unselectedTextColor = OrbitMuted,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (screen) {
                OrbitScreen.Home -> HomeScreen(onNavigate = { screen = it })
                OrbitScreen.Profiles -> ProfilesScreen()
                OrbitScreen.Versions -> VersionsScreen()
                OrbitScreen.Controls -> ControlsScreen()
                OrbitScreen.Settings -> SettingsScreen()
            }
        }
    }
}
