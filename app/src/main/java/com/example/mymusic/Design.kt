package com.example.mymusic

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---- Your color palette: change any of these to restyle the whole app ----
val Ink = Color(0xFF0E0F14)        // main background
val Panel = Color(0xFF181A22)      // cards and highlighted rows
val PanelHigh = Color(0xFF242733)  // mini player and small buttons
val Saffron = Color(0xFFFFB84D)    // accent color
val Lilac = Color(0xFFA99CFF)      // secondary color
val TextMain = Color(0xFFF2F0EA)   // main text
val TextDim = Color(0xFF9A9CA8)    // softer text

private val AppColors = darkColorScheme(
    primary = Saffron,
    onPrimary = Ink,
    secondary = Lilac,
    background = Ink,
    onBackground = TextMain,
    surface = Ink,
    onSurface = TextMain,
    surfaceVariant = Panel,
    onSurfaceVariant = TextDim
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, content = content)
}