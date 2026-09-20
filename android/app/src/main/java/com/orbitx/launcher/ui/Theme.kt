package com.orbitx.launcher.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// OrbitX palette: near-black background, mint accent, violet secondary.
val OrbitBg = Color(0xFF07080C)
val OrbitSurface = Color(0xFF0E1017)
val OrbitSurfaceHi = Color(0xFF161A24)
val OrbitMint = Color(0xFF3DDC97)
val OrbitViolet = Color(0xFF7C5CFF)
val OrbitText = Color(0xFFE7EAF3)
val OrbitMuted = Color(0xFF8E96AC)
val OrbitError = Color(0xFFFF6B6B)

private val OrbitScheme = darkColorScheme(
    primary = OrbitMint,
    onPrimary = Color(0xFF04150E),
    secondary = OrbitViolet,
    onSecondary = Color(0xFF0B0714),
    background = OrbitBg,
    onBackground = OrbitText,
    surface = OrbitSurface,
    onSurface = OrbitText,
    surfaceVariant = OrbitSurfaceHi,
    onSurfaceVariant = OrbitMuted,
    error = OrbitError,
)

@Composable
fun OrbitXTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OrbitScheme, content = content)
}
