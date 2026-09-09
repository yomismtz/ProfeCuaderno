package com.profecuaderno.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AgendaThemeStyle(val key: String, val title: String, val subtitle: String) {
    GRAPHITE_BLUE("graphite_blue", "Grafito azul", "Grises, azules y negro"),
    SUNSET_GARDEN("sunset_garden", "Jardín solar", "Amarillo, rojo y verde"),
    BOLD_CLASSIC("bold_classic", "Clásico vivo", "Rojo, azul y anaranjado"),
    MINT_LAVENDER("mint_lavender", "Menta lavanda", "Morados, menta y turquesa");

    companion object {
        fun fromKey(key: String?): AgendaThemeStyle? = entries.firstOrNull { it.key == key }
    }
}

private val GraphiteBlue = lightColorScheme(
    primary = Color(0xFF234A73), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E7F5), onPrimaryContainer = Color(0xFF10273F),
    secondary = Color(0xFF5F6B78), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E9ED), onSecondaryContainer = Color(0xFF252B31),
    tertiary = Color(0xFF1D2733), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDE3EA),
    background = Color(0xFFF5F7F9), onBackground = Color(0xFF191D21),
    surface = Color(0xFFFCFDFE), onSurface = Color(0xFF191D21),
    surfaceVariant = Color(0xFFE8EDF2), onSurfaceVariant = Color(0xFF4D5965),
    outline = Color(0xFF89939D)
)

private val SunsetGarden = lightColorScheme(
    primary = Color(0xFFB93A32), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDD8), onPrimaryContainer = Color(0xFF4A0D09),
    secondary = Color(0xFF3F7B4E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF1DF), onSecondaryContainer = Color(0xFF173A20),
    tertiary = Color(0xFFD39A16), onTertiary = Color(0xFF2F2200),
    tertiaryContainer = Color(0xFFFFEDB8),
    background = Color(0xFFFFFBF2), onBackground = Color(0xFF29251D),
    surface = Color(0xFFFFFEF8), onSurface = Color(0xFF29251D),
    surfaceVariant = Color(0xFFF6EEDB), onSurfaceVariant = Color(0xFF625A49),
    outline = Color(0xFF9D927A)
)

private val BoldClassic = lightColorScheme(
    primary = Color(0xFF245C9A), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9FA), onPrimaryContainer = Color(0xFF102D50),
    secondary = Color(0xFFD65B2B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0D2), onSecondaryContainer = Color(0xFF5A1D08),
    tertiary = Color(0xFFB52F3A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDADD),
    background = Color(0xFFFFFAF6), onBackground = Color(0xFF292421),
    surface = Color(0xFFFFFEFC), onSurface = Color(0xFF292421),
    surfaceVariant = Color(0xFFF4E9E2), onSurfaceVariant = Color(0xFF625650),
    outline = Color(0xFF9E8D84)
)

private val MintLavender = lightColorScheme(
    primary = Color(0xFF7654A8), onPrimary = Color.White,
    primaryContainer = Color(0xFFEADFFC), onPrimaryContainer = Color(0xFF2D1A48),
    secondary = Color(0xFF50BDB3), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F5F1), onSecondaryContainer = Color(0xFF123C39),
    tertiary = Color(0xFF319DA5), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9F2F3),
    background = Color(0xFFFFFBF8), onBackground = Color(0xFF2C2733),
    surface = Color(0xFFFFFEFC), onSurface = Color(0xFF2C2733),
    surfaceVariant = Color(0xFFF4EEF9), onSurfaceVariant = Color(0xFF5E5668),
    outline = Color(0xFFB7AFC1)
)

fun agendaPaperColor(style: AgendaThemeStyle): Color = when (style) {
    AgendaThemeStyle.GRAPHITE_BLUE -> Color(0xFFF4F7FA)
    AgendaThemeStyle.SUNSET_GARDEN -> Color(0xFFFFFBF0)
    AgendaThemeStyle.BOLD_CLASSIC -> Color(0xFFFFFAF5)
    AgendaThemeStyle.MINT_LAVENDER -> Color(0xFFFFFDF9)
}

fun agendaRuleColor(style: AgendaThemeStyle): Color = when (style) {
    AgendaThemeStyle.GRAPHITE_BLUE -> Color(0xFFE3E9EF)
    AgendaThemeStyle.SUNSET_GARDEN -> Color(0xFFF2E8CF)
    AgendaThemeStyle.BOLD_CLASSIC -> Color(0xFFF0E4DD)
    AgendaThemeStyle.MINT_LAVENDER -> Color(0xFFF0EDF4)
}

@Composable
fun ProfeCuadernoTheme(
    style: AgendaThemeStyle = AgendaThemeStyle.MINT_LAVENDER,
    content: @Composable () -> Unit
) {
    val colors = when (style) {
        AgendaThemeStyle.GRAPHITE_BLUE -> GraphiteBlue
        AgendaThemeStyle.SUNSET_GARDEN -> SunsetGarden
        AgendaThemeStyle.BOLD_CLASSIC -> BoldClassic
        AgendaThemeStyle.MINT_LAVENDER -> MintLavender
    }
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography, content = content)
}
