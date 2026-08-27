package com.rajk2007.kino.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KinoBlack = Color(0xFF080808)
val KinoSurface = Color(0xFF121212)
val KinoSurface2 = Color(0xFF1A1A1A)
val KinoGold = Color(0xFFE7A86B)
val KinoMuted = Color(0xFFA4A09C)
val KinoPurple = Color(0xFF8B7CFF)

@Composable
fun KinoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = KinoBlack, surface = KinoSurface, surfaceVariant = KinoSurface2,
            primary = KinoGold, onPrimary = KinoBlack, onBackground = Color.White,
            onSurface = Color.White, onSurfaceVariant = KinoMuted
        ),
        content = content
    )
}
