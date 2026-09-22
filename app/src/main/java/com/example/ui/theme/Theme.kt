package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HackerColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = Color.Black,
    primaryContainer = CyberCardBorder,
    onPrimaryContainer = CyberGreen,
    secondary = CyberCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003840),
    onSecondaryContainer = CyberCyan,
    tertiary = CyberMagenta,
    onTertiary = Color.White,
    background = CyberDarkBg,
    onBackground = TextPrimary,
    surface = CyberCardBg,
    onSurface = TextPrimary,
    surfaceVariant = CyberCardBorder,
    onSurfaceVariant = TextSecondary,
    error = CyberRed,
    onError = Color.Black
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = HackerColorScheme,
        typography = Typography,
        content = content
    )
}

