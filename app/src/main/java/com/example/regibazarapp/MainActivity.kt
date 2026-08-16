package com.example.regibazarapp

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.regibazarapp.data.ThemeManager
import com.example.regibazarapp.data.ThemePreference
import com.example.regibazarapp.navigation.AppNavigation
import com.example.regibazarapp.navigation.Routes
import com.example.regibazarapp.ui.theme.LocalThemeController
import com.example.regibazarapp.ui.theme.RegiBazarTheme
import com.example.regibazarapp.ui.theme.ThemeController
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════
 *  MAIN ACTIVITY – Punto de entrada
 *
 *  Maneja deep links desde notificaciones:
 *  si la notificación incluye driver_token_deeplink,
 *  navega directamente al RouteScreen del chofer.
 * ═══════════════════════════════════════════════
 */
class MainActivity : FragmentActivity() {

    private var pendingDeepLinkToken by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Leer deep link del intent inicial (app lanzada desde notificación en frío)
        pendingDeepLinkToken = intent.getStringExtra("driver_token_deeplink")

        setContent {
            val themeManager = remember { ThemeManager(applicationContext) }
            val scope = rememberCoroutineScope()
            val preference by themeManager.themePreference.collectAsState(initial = ThemePreference.SYSTEM)

            val systemDark = isSystemInDarkTheme()
            val isDark = when (preference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> systemDark
            }

            val controller = ThemeController(
                preference = preference,
                isDark = isDark,
                setPreference = { newPref -> scope.launch { themeManager.setTheme(newPref) } }
            )

            CompositionLocalProvider(LocalThemeController provides controller) {
                RegiBazarTheme(darkTheme = isDark) {
                    val navController = rememberNavController()

                    LaunchedEffect(pendingDeepLinkToken) {
                        val token = pendingDeepLinkToken?.takeIf { it.isNotBlank() }
                        if (token != null) {
                            navController.navigate(Routes.routeWithToken(token)) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                                launchSingleTop = true
                            }
                            pendingDeepLinkToken = null
                        }
                    }

                    AppNavigation(
                        navController = navController,
                        startDestination = Routes.LOGIN
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App ya estaba en primer plano o segundo plano: actualizar intent
        setIntent(intent)
        pendingDeepLinkToken = intent.getStringExtra("driver_token_deeplink")
        // El driver ya estará viendo su RouteScreen — solo se trae la app al frente
        // gracias a FLAG_ACTIVITY_SINGLE_TOP. No se necesita navegación adicional.
    }
}
