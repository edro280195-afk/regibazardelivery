package com.example.regibazarapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * ═══════════════════════════════════════════════
 *  TEMA PREMIUM – Material 3 (claro + oscuro)
 *
 *  • Oscuro: refinado para la noche.
 *  • Claro/diurno: legible bajo el sol al entregar.
 *  Cambia por sistema o por preferencia manual.
 * ═══════════════════════════════════════════════
 */

private val RegiBazarDarkScheme = darkColorScheme(
    primary = BrandAccent,
    onPrimary = InkOnAccent,
    primaryContainer = BrandAccentSoft,
    onPrimaryContainer = BrandAccent,

    secondary = ActionPrimary,
    onSecondary = InkOnAction,
    secondaryContainer = ActionPrimarySoft,
    onSecondaryContainer = ActionPrimary,

    tertiary = StatusInTransit,
    onTertiary = InkOnAccent,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    error = StatusFailed,
    onError = InkOnAccent,

    outline = GlassBorder,
    outlineVariant = GlassWhite
)

private val RegiBazarLightScheme = lightColorScheme(
    primary = BrandAccent,
    onPrimary = InkOnAccent,
    primaryContainer = BrandAccentSoft,
    onPrimaryContainer = BrandAccentPressed,

    secondary = ActionPrimary,
    onSecondary = InkOnAction,
    secondaryContainer = ActionPrimarySoft,
    onSecondaryContainer = ActionPrimaryPressed,

    tertiary = StatusInTransitLight,
    onTertiary = InkOnAccent,

    background = LightBackground,
    onBackground = TextPrimaryLight,

    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,

    error = StatusFailedLight,
    onError = InkOnAccent,

    outline = GlassLightBorder,
    outlineVariant = GlassLightOverlay
)

@Composable
fun RegiBazarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RegiBazarDarkScheme else RegiBazarLightScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Barras transparentes: el contenido edge-to-edge pinta detrás.
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                // En claro: iconos oscuros (legibles). En oscuro: iconos claros.
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RegiBazarTypography,
        content = content
    )
}
