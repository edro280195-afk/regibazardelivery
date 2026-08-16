package com.example.regibazarapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.regibazarapp.data.ThemePreference

/**
 * Controlador de tema accesible desde cualquier composable vía [LocalThemeController].
 * Permite leer la preferencia/estado actual y cambiarla (persistido en DataStore).
 */
data class ThemeController(
    val preference: ThemePreference,
    val isDark: Boolean,
    val setPreference: (ThemePreference) -> Unit
) {
    /** Alterna rápido entre claro y oscuro (ignora SYSTEM). */
    fun toggle() = setPreference(if (isDark) ThemePreference.LIGHT else ThemePreference.DARK)
}

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("LocalThemeController no fue provisto. Envuelve el contenido en MainActivity.")
}
