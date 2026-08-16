package com.example.regibazarapp.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.regibazarapp.data.TokenManager
import com.example.regibazarapp.data.models.AdminLoginRequest
import com.example.regibazarapp.data.models.RouteResponse
import com.example.regibazarapp.data.network.NetworkModule
import com.example.regibazarapp.service.RegiBazarFirebaseService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════
 *  Login ViewModel – Driver Token & Admin JWT
 * ═══════════════════════════════════════════════
 */

enum class LoginMode { DRIVER, ADMIN }
enum class LoginResult { NONE, DRIVER_SUCCESS, ADMIN_SUCCESS }

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loginMode: LoginMode = LoginMode.DRIVER,
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginResult: LoginResult = LoginResult.NONE,
    val routeData: RouteResponse? = null,
    val canUseBiometrics: Boolean = false,
    val showBiometricPrompt: Boolean = false,
    // Para navegación
    val driverToken: String = "",
    val adminName: String = ""
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Verificar credenciales guardadas y biometría
        viewModelScope.launch {
            val savedEmail = tokenManager.getEmail()
            val savedPassword = tokenManager.getPassword()
            val biometricsEnabled = tokenManager.useBiometrics()

            _uiState.update { it.copy(
                email = savedEmail ?: "",
                // password NO se pre-llena en la UI — solo se usa internamente tras biometría
                canUseBiometrics = biometricsEnabled && !savedEmail.isNullOrBlank() && !savedPassword.isNullOrBlank()
            ) }
        }
    }

    fun onTokenChange(newToken: String) {
        // No longer used for login, but might be kept for direct token entry if user wants fallback later
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun setLoginMode(mode: LoginMode) {
        _uiState.update { it.copy(loginMode = mode, error = null) }
    }

    fun toggleBiometrics(enabled: Boolean) {
        viewModelScope.launch {
            tokenManager.setUseBiometrics(enabled)
        }
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        
        if (email.isEmpty()) {
            _uiState.update { it.copy(error = "Ingresa tu correo") }
            return
        }

        // Si hay biometría activada y coinciden las credenciales guardadas, pedimos biometría
        if (_uiState.value.canUseBiometrics && email == tokenManager.getEmail()) {
            _uiState.update { it.copy(showBiometricPrompt = true) }
        } else {
            // Si no hay biometría o el correo es distinto, login tradicional
            if (password.isEmpty()) {
                _uiState.update { it.copy(error = "Ingresa tu contraseña") }
                return
            }
            performLogin(email, password)
        }
    }

    fun onBiometricPromptShown() {
        _uiState.update { it.copy(showBiometricPrompt = false) }
    }

    /** Llamar al abrir la pantalla — dispara biométrico automáticamente si hay credenciales guardadas */
    fun triggerAutoLogin() {
        val biometricsEnabled = tokenManager.useBiometrics()
        val savedEmail = tokenManager.getEmail()
        if (biometricsEnabled && !savedEmail.isNullOrBlank()) {
            _uiState.update { it.copy(showBiometricPrompt = true) }
        }
    }

    private fun performLogin(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Ambos modos usan auth/login ahora
                val response = NetworkModule.adminApi.login(AdminLoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    tokenManager.saveJwtToken(body.token)
                    tokenManager.saveCredentials(email, password)
                    NetworkModule.jwtToken = body.token

                    // Registrar token FCM con la API para recibir notificaciones push
                    RegiBazarFirebaseService.registerCurrentToken(getApplication())

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginResult = if (it.loginMode == LoginMode.ADMIN) LoginResult.ADMIN_SUCCESS else LoginResult.DRIVER_SUCCESS,
                            adminName = body.name,
                            canUseBiometrics = true // Habilitar para la siguiente
                        )
                    }
                    tokenManager.setUseBiometrics(true)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Credenciales incorrectas"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Error de conexión: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onBiometricSuccess() {
        val jwt = tokenManager.getJwtTokenSync()
        if (!jwt.isNullOrBlank()) {
            // JWT guardado válido — entrar sin llamada de red
            NetworkModule.jwtToken = jwt
            viewModelScope.launch {
                RegiBazarFirebaseService.registerCurrentToken(getApplication())
            }
            _uiState.update { it.copy(loginResult = LoginResult.ADMIN_SUCCESS) }
        } else {
            // Sin JWT — fallback a login completo con credenciales guardadas
            val email = tokenManager.getEmail()
            val password = tokenManager.getPassword()
            if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                performLogin(email, password)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Desregistrar token FCM para dejar de recibir notificaciones
            RegiBazarFirebaseService.unregisterToken(getApplication())
            tokenManager.clearDriverToken()
            tokenManager.clearJwtToken()
            NetworkModule.jwtToken = null
            _uiState.update { LoginUiState() }
        }
    }
}
