package com.example.regibazarapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * ══════════════════════════════════════════════════════════════
 *  Token Manager – Almacenamiento seguro de credenciales
 *
 *  • Driver token → DataStore (lectura reactiva frecuente)
 *  • JWT admin    → EncryptedSharedPreferences (AES256-GCM,
 *                   respaldado por Android Keystore)
 * ══════════════════════════════════════════════════════════════
 */

// DataStore para el token de ruta del chofer
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "regi_bazar_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val DRIVER_TOKEN_KEY = stringPreferencesKey("driver_token")
        private const val JWT_KEY = "jwt_token"
        private const val EMAIL_KEY = "user_email"
        private const val PASSWORD_KEY = "user_password"
        private const val USE_BIOMETRICS_KEY = "use_biometrics"
        private const val SECURE_PREFS_NAME = "regi_bazar_secure_prefs"
    }

    // ─── EncryptedSharedPreferences (JWT) ────────────────────────

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ─── Driver Token (DataStore) ─────────────────────────────────

    /** Flow reactivo del token de ruta del chofer */
    val driverToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DRIVER_TOKEN_KEY]
    }

    suspend fun saveDriverToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[DRIVER_TOKEN_KEY] = token
        }
    }

    suspend fun getToken(): String? = withContext(Dispatchers.IO) {
        context.dataStore.data.map { it[DRIVER_TOKEN_KEY] }.firstOrNull()
    }

    suspend fun clearDriverToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(DRIVER_TOKEN_KEY)
        }
    }

    // ─── JWT Token (EncryptedSharedPreferences) ───────────────────

    /** Flow del JWT — emite el valor actual una vez */
    val jwtToken: Flow<String?> = flow {
        emit(withContext(Dispatchers.IO) { encryptedPrefs.getString(JWT_KEY, null) })
    }

    suspend fun saveJwtToken(token: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(JWT_KEY, token).apply()
    }

    suspend fun clearJwtToken() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().remove(JWT_KEY).apply()
    }

    /** Lectura síncrona del JWT (para el interceptor de NetworkModule) */
    fun getJwtTokenSync(): String? =
        encryptedPrefs.getString(JWT_KEY, null)

    // ─── Credenciales Encripadas & Biometría ─────────────────────

    suspend fun saveCredentials(email: String, password: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().run {
            putString(EMAIL_KEY, email)
            putString(PASSWORD_KEY, password)
            apply()
        }
    }

    fun getEmail(): String? = encryptedPrefs.getString(EMAIL_KEY, null)
    fun getPassword(): String? = encryptedPrefs.getString(PASSWORD_KEY, null)

    fun clearCredentials() = encryptedPrefs.edit().run {
        remove(EMAIL_KEY)
        remove(PASSWORD_KEY)
        apply()
    }

    suspend fun setUseBiometrics(use: Boolean) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putBoolean(USE_BIOMETRICS_KEY, use).apply()
    }

    fun useBiometrics(): Boolean = encryptedPrefs.getBoolean(USE_BIOMETRICS_KEY, false)
}
