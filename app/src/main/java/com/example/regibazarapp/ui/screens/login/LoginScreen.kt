package com.example.regibazarapp.ui.screens.login

import android.view.HapticFeedbackConstants
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import com.example.regibazarapp.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.regibazarapp.ui.components.ThemeToggleButton
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.theme.RegiBazar2026Tokens
import com.example.regibazarapp.ui.theme.regiBazar2026Palette

@Composable
fun LoginScreen(
    onDriverLoginSuccess: (String) -> Unit,
    onAdminLoginSuccess: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
    val palette = regiBazar2026Palette()
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val biometricPrompt = remember(activity) {
        activity?.let { fragmentActivity ->
            BiometricPrompt(
                fragmentActivity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        loginViewModel.onBiometricSuccess()
                    }
                }
            )
        }
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Entrar a Regi Bazar")
            .setSubtitle("Usa tu huella o rostro para continuar")
            .setNegativeButtonText("Usar contraseña")
            .build()
    }

    LaunchedEffect(Unit) {
        loginViewModel.triggerAutoLogin()
    }

    LaunchedEffect(uiState.showBiometricPrompt) {
        if (uiState.showBiometricPrompt) {
            biometricPrompt?.authenticate(promptInfo)
            loginViewModel.onBiometricPromptShown()
        }
    }

    LaunchedEffect(uiState.loginResult) {
        when (uiState.loginResult) {
            LoginResult.DRIVER_SUCCESS -> onAdminLoginSuccess()
            LoginResult.ADMIN_SUCCESS -> onAdminLoginSuccess()
            LoginResult.NONE -> Unit
        }
    }

    Scaffold(
        containerColor = palette.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(palette.background)
        ) {
            LoginBackgroundAccent(palette)

            ThemeToggleButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 82.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoginBrandBlock(palette)

                LoginCard(
                    uiState = uiState,
                    palette = palette,
                    onModeChange = { mode ->
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        loginViewModel.setLoginMode(mode)
                    },
                    onEmailChange = loginViewModel::onEmailChange,
                    onPasswordChange = loginViewModel::onPasswordChange,
                    onLogin = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        focusManager.clearFocus()
                        loginViewModel.login()
                    }
                )

                Text(
                    text = if (uiState.loginMode == LoginMode.ADMIN) {
                        "Administra rutas, cobros y operación desde el panel móvil."
                    } else {
                        "Entra para ver tus rutas asignadas y continuar el reparto."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun LoginBackgroundAccent(palette: RegiBazar2026Palette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.accentSoft,
                        palette.background
                    )
                )
            )
    )
}

@Composable
private fun LoginBrandBlock(palette: RegiBazar2026Palette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.size(84.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_regi),
                    contentDescription = "Logo Regi Bazar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Regi Bazar",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp
            ),
            color = palette.textPrimary
        )
        Text(
            "Operación de rutas",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary
        )
    }
}

@Composable
private fun LoginCard(
    uiState: LoginUiState,
    palette: RegiBazar2026Palette,
    onModeChange: (LoginMode) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LoginModeControl(
                selectedMode = uiState.loginMode,
                palette = palette,
                onModeChange = onModeChange
            )

            LoginInputs(
                email = uiState.email,
                password = uiState.password,
                palette = palette,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onDone = onLogin
            )

            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + slideInVertically(tween(160)) { -it / 3 },
                exit = fadeOut() + slideOutVertically(tween(140)) { -it / 3 }
            ) {
                LoginError(message = uiState.error.orEmpty(), palette = palette)
            }

            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !uiState.isLoading &&
                    uiState.email.isNotBlank() &&
                    (uiState.password.isNotBlank() || uiState.canUseBiometrics),
                shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color(0xFFF8FAFF),
                    disabledContainerColor = palette.accentSoft,
                    disabledContentColor = palette.textSecondary
                )
            ) {
                AnimatedContent(
                    targetState = uiState.isLoading,
                    transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                    label = "login-loading"
                ) { isLoading ->
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFF8FAFF),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (uiState.loginMode == LoginMode.ADMIN) {
                                "Entrar al panel"
                            } else {
                                "Entrar a reparto"
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (uiState.canUseBiometrics) {
                OutlinedButton(
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.32f))
                ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Entrar con huella", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LoginModeControl(
    selectedMode: LoginMode,
    palette: RegiBazar2026Palette,
    onModeChange: (LoginMode) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(palette.surfaceMuted, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        val indicatorWidth = maxWidth / 2
        Box(
            modifier = Modifier
                .offset(x = if (selectedMode == LoginMode.ADMIN) indicatorWidth else 0.dp)
                .width(indicatorWidth)
                .fillMaxHeight()
                .background(palette.surface, RoundedCornerShape(11.dp))
        )
        Row(Modifier.fillMaxSize()) {
            LoginModeButton(
                label = "Reparto",
                icon = Icons.Rounded.LocalShipping,
                selected = selectedMode == LoginMode.DRIVER,
                palette = palette,
                modifier = Modifier.weight(1f),
                onClick = { onModeChange(LoginMode.DRIVER) }
            )
            LoginModeButton(
                label = "Admin",
                icon = Icons.Rounded.AdminPanelSettings,
                selected = selectedMode == LoginMode.ADMIN,
                palette = palette,
                modifier = Modifier.weight(1f),
                onClick = { onModeChange(LoginMode.ADMIN) }
            )
        }
    }
}

@Composable
private fun LoginModeButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    palette: RegiBazar2026Palette,
    modifier: Modifier,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (selected) palette.textPrimary else palette.textSecondary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoginInputs(
    email: String,
    password: String,
    palette: RegiBazar2026Palette,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDone: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Correo electrónico") },
            leadingIcon = {
                Icon(Icons.Rounded.Email, contentDescription = null, tint = palette.accent)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
            colors = loginFieldColors(palette)
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contraseña") },
            leadingIcon = {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.accent)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = palette.textSecondary
                    )
                }
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
            colors = loginFieldColors(palette)
        )
    }
}

@Composable
private fun LoginError(message: String, palette: RegiBazar2026Palette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.errorSoft, RoundedCornerShape(13.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.VerifiedUser,
            contentDescription = null,
            tint = palette.error,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun loginFieldColors(palette: RegiBazar2026Palette) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = palette.surface,
    unfocusedContainerColor = palette.surface,
    focusedBorderColor = palette.accent,
    unfocusedBorderColor = palette.divider,
    focusedTextColor = palette.textPrimary,
    unfocusedTextColor = palette.textPrimary,
    focusedLabelColor = palette.accent,
    unfocusedLabelColor = palette.textSecondary,
    focusedPlaceholderColor = palette.textSecondary,
    unfocusedPlaceholderColor = palette.textSecondary,
    cursorColor = palette.accent
)
