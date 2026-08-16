package com.example.regibazarapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Preferencia de tema del usuario.
 * SYSTEM = sigue el modo del teléfono; LIGHT = diurno; DARK = nocturno.
 */
enum class ThemePreference { SYSTEM, LIGHT, DARK }

// DataStore propio para preferencias de UI (separado del de tokens).
private val Context.uiDataStore: DataStore<Preferences> by preferencesDataStore(name = "regi_bazar_ui_prefs")

class ThemeManager(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_preference")
    }

    /** Flow reactivo de la preferencia de tema (default SYSTEM). */
    val themePreference: Flow<ThemePreference> = context.uiDataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "light" -> ThemePreference.LIGHT
            "dark" -> ThemePreference.DARK
            else -> ThemePreference.SYSTEM
        }
    }

    suspend fun setTheme(pref: ThemePreference) {
        context.uiDataStore.edit { it[THEME_KEY] = pref.name.lowercase() }
    }
}
