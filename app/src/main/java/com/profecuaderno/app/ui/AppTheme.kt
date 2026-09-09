package com.profecuaderno.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF7654A8),
    secondary = Color(0xFF78C9B8),
    tertiary = Color(0xFF4DB7BD),
    background = Color(0xFFFFFDF8),
    surface = Color(0xFFFFFDF8),
    surfaceVariant = Color(0xFFF2EEF8),
    outline = Color(0xFF847C8F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCBB6F2),
    secondary = Color(0xFF9ADBCB),
    tertiary = Color(0xFF83D7DA)
)

@Composable
fun ProfeCuadernoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
