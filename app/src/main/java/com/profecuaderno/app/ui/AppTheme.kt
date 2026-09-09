package com.profecuaderno.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF7654A8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADFFC),
    onPrimaryContainer = Color(0xFF2D1A48),
    secondary = Color(0xFF50BDB3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F5F1),
    onSecondaryContainer = Color(0xFF123C39),
    tertiary = Color(0xFFE88DB5),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFDE0ED),
    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF2C2733),
    surface = Color(0xFFFFFEFC),
    onSurface = Color(0xFF2C2733),
    surfaceVariant = Color(0xFFF4EEF9),
    onSurfaceVariant = Color(0xFF5E5668),
    outline = Color(0xFFB7AFC1)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD8C3F6),
    secondary = Color(0xFF8FDDD4),
    tertiary = Color(0xFFF3AFCC)
)

@Composable
fun ProfeCuadernoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
