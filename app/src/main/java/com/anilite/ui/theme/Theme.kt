package com.anilite.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFB39DDB)
val Purple40 = Color(0xFF7C4DFF)
val Background = Color(0xFF0A0A0F)
val Surface = Color(0xFF12121A)
val SurfaceVariant = Color(0xFF1C1C28)
val CardBg = Color(0xFF16161F)

private val DarkColorScheme = darkColorScheme(
    primary = Purple40,
    secondary = Purple80,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0C0)
)

@Composable
fun AnidakuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
