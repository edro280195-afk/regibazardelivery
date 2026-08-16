package com.example.regibazarapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Tokens visuales del rediseño 2026.
 *
 * Se mantienen separados del tema anterior mientras las pantallas se migran una por una.
 */
object RegiBazar2026Tokens {
    val ScreenPadding = 16.dp
    val HeaderHorizontalPadding = 20.dp
    val TouchTarget = 48.dp
    val CardRadius = 18.dp
    val ControlRadius = 14.dp
    val TagRadius = 9.dp
}

@Immutable
data class RegiBazar2026Palette(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val accent: Color,
    val accentPressed: Color,
    val accentSoft: Color,
    val money: Color,
    val moneySoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val error: Color,
    val errorSoft: Color,
    val shadow: Color
)

private val Light2026Palette = RegiBazar2026Palette(
    background = Color(0xFFF1F4F9),
    surface = Color(0xFFFCFDFF),
    surfaceMuted = Color(0xFFE4E9F1),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF6B7280),
    divider = Color(0xFFEAEEF4),
    accent = Color(0xFF0B5FFF),
    accentPressed = Color(0xFF0A4FD6),
    accentSoft = Color(0xFFEEF3FF),
    money = Color(0xFFFF7A00),
    moneySoft = Color(0xFFFFF3E6),
    success = Color(0xFF16A34A),
    successSoft = Color(0xFFE7F6EC),
    warning = Color(0xFFB7791F),
    warningSoft = Color(0xFFFFF4E0),
    error = Color(0xFFDC2626),
    errorSoft = Color(0xFFFDECEC),
    shadow = Color(0x18101828)
)

private val Dark2026Palette = RegiBazar2026Palette(
    background = Color(0xFF0F151F),
    surface = Color(0xFF171F2B),
    surfaceMuted = Color(0xFF222D3C),
    textPrimary = Color(0xFFF3F6FB),
    textSecondary = Color(0xFFAEB8C8),
    divider = Color(0xFF2B3747),
    accent = Color(0xFF6E9DFF),
    accentPressed = Color(0xFF8AB0FF),
    accentSoft = Color(0xFF1D3159),
    money = Color(0xFFFFA14A),
    moneySoft = Color(0xFF4A2B12),
    success = Color(0xFF4BC878),
    successSoft = Color(0xFF183B27),
    warning = Color(0xFFF3B64F),
    warningSoft = Color(0xFF493415),
    error = Color(0xFFFF7070),
    errorSoft = Color(0xFF4C2024),
    shadow = Color(0x66070B11)
)

@Composable
fun regiBazar2026Palette(): RegiBazar2026Palette =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        Light2026Palette
    } else {
        Dark2026Palette
    }
