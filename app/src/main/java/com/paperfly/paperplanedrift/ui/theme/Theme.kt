package com.paperfly.paperplanedrift.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PaperColorScheme = lightColorScheme(
    primary = Color(0xFFE8965A),
    onPrimary = Color.White,
    secondary = Color(0xFF7FA8C9),
    onSecondary = Color.White,
    background = Color(0xFFFDF6EC),
    onBackground = Color(0xFF4A4132),
    surface = Color(0xFFFFFDF4),
    onSurface = Color(0xFF4A4132),
    surfaceVariant = Color(0xFFF4ECD8),
    onSurfaceVariant = Color(0xFF6B6252),
)

@Composable
fun PaperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaperColorScheme,
        content = content,
    )
}
