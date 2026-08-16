package com.example.regibazarapp.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.regibazarapp.ui.screens.chat.ChatScreen
import com.example.regibazarapp.ui.screens.inventory.InventoryScreen
import com.example.regibazarapp.ui.screens.adminroute.AdminRouteScreen
import com.example.regibazarapp.ui.screens.login.LoginScreen
import com.example.regibazarapp.ui.screens.route.RouteScreen
import com.example.regibazarapp.ui.screens.routebuilder.RouteBuilderScreen
import com.example.regibazarapp.ui.screens.routes.RoutesListScreen
import com.example.regibazarapp.ui.screens.summary.RouteSummaryScreen
import com.example.regibazarapp.ui.screens.summary.RouteSummaryViewModel

/**
 * ═══════════════════════════════════════════════
 *  App Navigation – Composable NavHost
 *
 *  Routes:
 *    "login"                          → LoginScreen
 *    "route/{token}"                  → RouteScreen (HUD Chofer)
 *    "routes_list"                    → RoutesListScreen (Panel Admin)
 *    "chat/{token}/{deliveryId}"      → ChatScreen
 *    "route_summary/{token}"          → RouteSummaryScreen
 * ═══════════════════════════════════════════════
 */

object Routes {
    const val LOGIN = "login"
    const val ROUTE = "route/{token}"
    const val ROUTES_LIST = "routes_list"
    const val ADMIN_ROUTE = "admin_route/{routeId}"
    const val ROUTE_BUILDER = "route_builder?routeId={routeId}"
    const val CHAT = "chat/{token}/{deliveryId}"
    const val ROUTE_SUMMARY = "route_summary/{token}"
    const val INVENTORY = "inventory"

    fun routeWithToken(token: String) = "route/${encodeSegment(token)}"
    fun adminRoute(routeId: Int) = "admin_route/$routeId"
    fun routeBuilder(routeId: Int? = null) = "route_builder?routeId=${routeId ?: -1}"
    fun chat(token: String, deliveryId: Int = 0) = "chat/${encodeSegment(token)}/$deliveryId"
    fun routeSummary(token: String) = "route_summary/${encodeSegment(token)}"

    fun decodeSegment(value: String): String = Uri.decode(value)

    private fun encodeSegment(value: String): String = Uri.encode(value)
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Routes.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ── Login Screen ──
        composable(Routes.LOGIN) {
            LoginScreen(
                onDriverLoginSuccess = { token ->
                    navController.navigate(Routes.routeWithToken(token)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onAdminLoginSuccess = {
                    navController.navigate(Routes.ROUTES_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // ── Route Screen (HUD Chofer) ──
        composable(
            route = Routes.ROUTE,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token")
                ?.let(Routes::decodeSegment)
                ?: ""
            RouteScreen(
                driverToken = token,
                onNavigateToChat = { deliveryId, _ ->
                    navController.navigate(Routes.chat(token, deliveryId))
                },
                onNavigateToSummary = {
                    navController.navigate(Routes.routeSummary(token))
                },
                onSessionExpired = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Routes List (Admin) ──
        composable(
            route = Routes.ROUTES_LIST,
            exitTransition = {
                if (targetState.destination.route == Routes.ADMIN_ROUTE) {
                    slideOutHorizontally(tween(240)) { -it / 5 } +
                        fadeOut(tween(160))
                } else {
                    null
                }
            },
            popEnterTransition = {
                if (initialState.destination.route == Routes.ADMIN_ROUTE) {
                    slideInHorizontally(tween(240)) { -it / 5 } +
                        fadeIn(tween(180))
                } else {
                    null
                }
            }
        ) {
            RoutesListScreen(
                onManageRoute = { routeId ->
                    navController.navigate(Routes.adminRoute(routeId))
                },
                onOpenDriver = { driverToken ->
                    navController.navigate(Routes.routeWithToken(driverToken))
                },
                onCreateRoute = {
                    navController.navigate(Routes.routeBuilder())
                },
                onOpenInventory = {
                    navController.navigate(Routes.INVENTORY)
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ROUTES_LIST) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.INVENTORY) {
            InventoryScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.ADMIN_ROUTE,
            arguments = listOf(navArgument("routeId") { type = NavType.IntType }),
            enterTransition = {
                if (initialState.destination.route == Routes.ROUTES_LIST) {
                    slideInHorizontally(tween(240)) { it } +
                        fadeIn(tween(180))
                } else {
                    null
                }
            },
            popExitTransition = {
                if (targetState.destination.route == Routes.ROUTES_LIST) {
                    slideOutHorizontally(tween(220)) { it } +
                        fadeOut(tween(150))
                } else {
                    null
                }
            }
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getInt("routeId") ?: return@composable
            AdminRouteScreen(
                routeId = routeId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.routeBuilder(id)) },
                onOpenDriver = { token -> navController.navigate(Routes.routeWithToken(token)) },
                onDeleted = {
                    navController.navigate(Routes.ROUTES_LIST) {
                        popUpTo(Routes.ROUTES_LIST) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.ROUTE_BUILDER,
            arguments = listOf(
                navArgument("routeId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val rawRouteId = backStackEntry.arguments?.getInt("routeId") ?: -1
            RouteBuilderScreen(
                routeId = rawRouteId.takeIf { it > 0 },
                onBack = { navController.popBackStack() },
                onRouteSaved = { savedRouteId ->
                    navController.navigate(Routes.adminRoute(savedRouteId)) {
                        popUpTo(Routes.ADMIN_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Chat Screen ──
        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("token") { type = NavType.StringType },
                navArgument("deliveryId") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token")
                ?.let(Routes::decodeSegment)
                ?: ""
            val deliveryId = backStackEntry.arguments?.getInt("deliveryId") ?: 0
            ChatScreen(
                driverToken = token,
                deliveryId = deliveryId,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Route Summary Screen ──
        composable(
            route = Routes.ROUTE_SUMMARY,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token")
                ?.let(Routes::decodeSegment)
                ?: ""
            val summaryViewModel: RouteSummaryViewModel = viewModel()
            LaunchedEffect(token) {
                summaryViewModel.initialize(token)
            }
            val uiState by summaryViewModel.uiState.collectAsStateWithLifecycle()

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.error.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> RouteSummaryScreen(
                    routeName = uiState.routeName,
                    deliveredCount = uiState.deliveredCount,
                    failedCount = uiState.failedCount,
                    totalCount = uiState.totalCount,
                    totalCollected = uiState.totalCollected,
                    collectedByCash = uiState.collectedByCash,
                    collectedByTransfer = uiState.collectedByTransfer,
                    collectedByDeposit = uiState.collectedByDeposit,
                    startedAt = uiState.startedAt,
                    deliveries = uiState.deliveries,
                    fastestDoorTimeSecs = uiState.fastestDoorTimeSecs,
                    slowestDoorTimeSecs = uiState.slowestDoorTimeSecs,
                    onFinish = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
