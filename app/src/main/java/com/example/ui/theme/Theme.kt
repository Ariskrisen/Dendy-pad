package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DendyDarkColorScheme = darkColorScheme(
    primary = DendyCrimsonBright,
    onPrimary = Color.White,
    primaryContainer = DendyCrimsonDark,
    onPrimaryContainer = DendyIvoryLight,
    secondary = DendyAmber,
    onSecondary = Color(0xFF1E1700),
    secondaryContainer = Color(0xFF3E2E08),
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = DendyIvory,
    onTertiary = DendyCharcoal,
    background = DendySurfaceDark,
    onBackground = DendyIvoryLight,
    surface = DendySurfaceContainer,
    onSurface = DendyIvoryLight,
    surfaceVariant = DendySurfaceHigh,
    onSurfaceVariant = Color(0xFFC8C1B6),
    outline = DendyOutline,
)

@Composable
fun DendyPadTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DendyDarkColorScheme,
        typography = Typography,
        content = content,
    )
}
