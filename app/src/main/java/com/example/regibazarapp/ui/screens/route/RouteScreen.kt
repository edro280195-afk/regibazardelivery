package com.example.regibazarapp.ui.screens.route

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.service.CamiState
import com.example.regibazarapp.service.LocationForegroundService
import com.example.regibazarapp.ui.components.*
import com.example.regibazarapp.ui.theme.*
import com.example.regibazarapp.ui.utils.MapUtils
import com.example.regibazarapp.ui.utils.formatRouteMoney
import coil.compose.AsyncImage
import com.example.regibazarapp.BuildConfig
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

/**
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * ROUTE SCREEN â€“ EL HUD DEL REPARTIDOR ðŸš€
 * v2: CAMI voz, SignalR indicator, polilÃ­nea, gastos, proximidad,
 * cola offline indicator, resumen de ruta y Auto-Arranque.
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    driverToken: String,
    onNavigateToChat: (deliveryId: Int, clientName: String) -> Unit = { _, _ -> },
    onNavigateToSummary: () -> Unit = {},
    onSessionExpired: () -> Unit = {},
    onBack: () -> Unit = {},
    routeViewModel: RouteViewModel = viewModel()
) {
    val uiState by routeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // â”€â”€ Dialogs â”€â”€
    var showFailDialog by remember { mutableStateOf(false) }
    var showReorderDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showRouteCompleteDialog by remember { mutableStateOf(false) }
    var showLoadScanner by remember { mutableStateOf(false) }

    // â•â•â• PERMISOS EN RUNTIME â•â•â•
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    var recordAudioPermissionGranted by remember { mutableStateOf(false) }
    var backgroundLocationGranted by remember { mutableStateOf(false) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    val mapLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationPermissionGranted) {
            // Arrancamos la ruta y el motor de inmediato
            routeViewModel.startRoute()
            LocationForegroundService.start(context, driverToken)
            
            // Pedimos permisos de background si es necesario
            if (!backgroundLocationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                showBackgroundLocationDialog = true
            }
        } else {
            Toast.makeText(context, "Se necesitan permisos de ubicaciÃ³n para GPS", Toast.LENGTH_LONG).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        recordAudioPermissionGranted = granted
        if (!granted) {
            Toast.makeText(context, "El micrÃ³fono es necesario para C.A.M.I.", Toast.LENGTH_LONG).show()
        }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        backgroundLocationGranted = granted
    }

    fun safeStartRoute() {
        if (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            routeViewModel.startRoute()
            LocationForegroundService.start(context, driverToken)
        }
    }

    // Verificar y pedir permisos al entrar
    LaunchedEffect(Unit) {
        locationPermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        notificationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        if (!locationPermissionGranted) {
            mapLocationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        recordAudioPermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        backgroundLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        cameraPermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ðŸ”¥ EL AUTO-ARRANQUE DEL MOTOR ðŸ”¥
    // Si la app se reinicia y la ruta YA estaba activa, prendemos el GPS automÃ¡ticamente.
    LaunchedEffect(uiState.isRouteStarted, uiState.isLocationPaused, locationPermissionGranted) {
        if (uiState.isRouteStarted && !uiState.isLocationPaused) {
            if (locationPermissionGranted) {
                // Si ya tiene permisos y la ruta estÃ¡ activa, Â¡Arranca el motor!
                LocationForegroundService.start(context, driverToken)
            } else {
                // Si la ruta estÃ¡ activa pero le faltan permisos, pÃ­delos.
                safeStartRoute()
            }
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            routeViewModel.startLocationMonitoring()
        }
    }

    LaunchedEffect(driverToken) { routeViewModel.initialize(driverToken) }

    // CelebraciÃ³n Lottie al completar una entrega (cuando sube el contador de entregadas)
    var showDeliverySuccess by remember { mutableStateOf(false) }
    var prevDeliveredCount by remember { mutableStateOf(uiState.deliveredCount) }
    LaunchedEffect(uiState.deliveredCount) {
        if (uiState.deliveredCount > prevDeliveredCount) {
            showDeliverySuccess = true
            kotlinx.coroutines.delay(1800)
            showDeliverySuccess = false
        }
        prevDeliveredCount = uiState.deliveredCount
    }

    // Mostrar diÃ¡logo cuando la ruta se completa (en lugar de navegar inmediatamente)
    LaunchedEffect(uiState.isRouteComplete) {
        if (uiState.isRouteComplete) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showRouteCompleteDialog = true
        }
    }

    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) {
            onSessionExpired()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        routeViewModel.onPhotoCaptured(bitmap)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (granted) cameraLauncher.launch(null)
        else Toast.makeText(context, "Se necesita permiso de cÃ¡mara para tomar evidencias", Toast.LENGTH_LONG).show()
    }

    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    )

    val currentDelivery = uiState.currentDelivery
    // Usar la ubicaciÃ³n del conductor como base si no hay entrega con coordenadas
    val defaultPos = if (LocationForegroundService.hasLocation)
        LatLng(LocationForegroundService.lastLat, LocationForegroundService.lastLng)
    else LatLng(25.6866, -100.3161) // Nuevo LeÃ³n/Tamaulipas default

    val targetPos = if (currentDelivery?.latitude != null && currentDelivery.longitude != null)
        LatLng(currentDelivery.latitude, currentDelivery.longitude)
    else defaultPos

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetPos, 14f)
    }

    // Centrar automÃ¡ticamente cuando la ubicaciÃ³n del conductor estÃ© disponible por primera vez
    LaunchedEffect(LocationForegroundService.hasLocation) {
        if (LocationForegroundService.hasLocation && currentDelivery?.latitude == null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(LocationForegroundService.lastLat, LocationForegroundService.lastLng), 
                    15f
                )
            )
        }
    }

    LaunchedEffect(currentDelivery?.deliveryId) {
        if (currentDelivery?.latitude != null && currentDelivery.longitude != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(currentDelivery.latitude, currentDelivery.longitude), 15f),
                durationMs = 1000
            )
        }
    }

    // â”€â”€ LOADING â”€â”€
    if (uiState.isLoading) {
        DriverRouteLoadingScreen()
        return
    }

    // â”€â”€ DIALOGS â”€â”€
    if (showFailDialog) {
        FailReasonDialog(
            onConfirm = { reason ->
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                routeViewModel.failCurrent(reason)
                showFailDialog = false
            },
            onDismiss = { showFailDialog = false }
        )
    }

    if (showReorderDialog) {
        val deliveries = uiState.routeData?.deliveries ?: emptyList()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f)
        ) {
            ReorderRouteDialog(
                deliveries = deliveries,
                onConfirm = { orderedIds ->
                    routeViewModel.reorderDeliveries(orderedIds)
                    showReorderDialog = false
                },
                onDismiss = { showReorderDialog = false }
            )
        }
    }

    if (showExpenseDialog) {
        ExpenseDialog(
            driverToken = driverToken,
            onDismiss = { showExpenseDialog = false },
            onExpenseAdded = { showExpenseDialog = false }
        )
    }

    // ─── Firma Digital ───
    if (showSignatureDialog) {
        SignatureDialog(
            clientName = uiState.currentDelivery?.clientName ?: "",
            onConfirm = { result ->
                routeViewModel.onSignatureCaptured(
                    CapturedSignature(
                        bitmap = result.bitmap,
                        svg = result.svg,
                        signedByName = result.signedByName
                    )
                )
                showSignatureDialog = false
            },
            onDismiss = { showSignatureDialog = false }
        )
    }

    // ─── Diálogo de Ruta Completada ───
    if (showRouteCompleteDialog) {
        val palette = regiBazar2026Palette()
        AlertDialog(
            onDismissRequest = {},
            containerColor = palette.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = CircleShape,
                        color = palette.successSoft,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎉", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "¡Ruta completada!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "${uiState.deliveredCount} entregadas  ·  ${uiState.failedCount} fallidas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Total cobrado: ${formatRouteMoney(uiState.totalCollected)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.money,
                        textAlign = TextAlign.Center
                    )
                    // Duración de ruta
                    uiState.routeDurationMinutes?.let { mins ->
                        HorizontalDivider(color = palette.divider)
                        val durText = if (mins >= 60) "${mins / 60} h ${mins % 60} min" else "$mins min"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("⏱ Duración total", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            Text(durText, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = palette.textPrimary)
                        }
                    }
                    // Métricas de tiempo en puerta
                    uiState.avgDoorTimeSecs?.let { avg ->
                        HorizontalDivider(color = palette.divider)
                        Text(
                            "TIEMPO EN PUERTA",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Promedio", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            Text(formatDoorTime(avg), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = palette.accent)
                        }
                        uiState.fastestDoorTimeSecs?.let { fast ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("⚡ Más rápida", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                                Text(formatDoorTime(fast), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = palette.success)
                            }
                        }
                        uiState.slowestDoorTimeSecs?.let { slow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🐢 Más lenta", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                                Text(formatDoorTime(slow), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = palette.warning)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRouteCompleteDialog = false
                        onNavigateToSummary()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver estadísticas", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        )
    }

    // â”€â”€ DiÃ¡logo: Ruta eliminada por admin mientras estaba activa â”€â”€
    if (uiState.routeDeletedByAdmin) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = DarkCard,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("ðŸš«", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ruta cancelada",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = StatusFailed,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    uiState.routeDeletedMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { onSessionExpired() },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusFailed, contentColor = InkOnAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Entendido", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        )
    }

    if (showBackgroundLocationDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationDialog = false },
            containerColor = DarkCard,
            title = {
                Text(
                    "UbicaciÃ³n en segundo plano",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    "Para que el GPS funcione mientras usas otras apps o con la pantalla apagada, necesitamos permiso de ubicaciÃ³n 'Permitir siempre'.\n\nEsto es necesario para el seguimiento continuo durante tu ruta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackgroundLocationDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandAccent)
                ) { Text("Permitir siempre") }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundLocationDialog = false }) {
                    Text("Ahora no", color = TextMuted)
                }
            }
        )
    }

    // Alerta de chat nuevo
    if (uiState.hasNewChatMessage) {
        LaunchedEffect(Unit) {
            // Limpiar despuÃ©s de 5s
            kotlinx.coroutines.delay(5000)
            routeViewModel.dismissChatAlert()
        }
    }

    // Modo reparto 2026. La rama anterior permanece como respaldo durante la migraciÃ³n.
    val useRouteExperience2026 = remember { true }
    if (useRouteExperience2026) {
        val allRoutePackages = remember(uiState.routeData) {
            uiState.routeData?.deliveries?.flatMap { it.packages.orEmpty() }.orEmpty()
        }

        if (showLoadScanner) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    showLoadScanner = false
                    routeViewModel.loadRoute()
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false
                )
            ) {
                PackageScannerOverlay(
                    packages = allRoutePackages,
                    driverToken = driverToken,
                    action = "Load",
                    onDismiss = {
                        showLoadScanner = false
                        routeViewModel.loadRoute()
                    }
                )
            }
        }

        RouteDeliveryExperience(
            uiState = uiState,
            driverToken = driverToken,
            locationPermissionGranted = locationPermissionGranted,
            isDark = isDark,
            cameraPositionState = cameraPositionState,
            sheetState = sheetState,
            showDeliverySuccess = showDeliverySuccess,
            onBack = onBack,
            onDeliver = { payments ->
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                routeViewModel.deliverCurrent(payments)
            },
            onFail = { showFailDialog = true },
            onTakePhoto = {
                if (cameraPermissionGranted) {
                    cameraLauncher.launch(null)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onChatDelivery = {
                currentDelivery?.let { delivery ->
                    onNavigateToChat(delivery.deliveryId, delivery.clientName)
                }
            },
            onChatAdmin = {
                routeViewModel.dismissChatAlert()
                onNavigateToChat(0, "Admin")
            },
            onNavigate = {
                currentDelivery?.let { delivery ->
                    openNavigation(context, delivery.latitude, delivery.longitude)
                }
            },
            onSignatureRequested = { showSignatureDialog = true },
            onMarkInTransit = routeViewModel::markCurrentInTransit,
            onScanCompleted = routeViewModel::loadRoute,
            onExpense = { showExpenseDialog = true },
            onReorder = { showReorderDialog = true },
            onStartRoute = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                safeStartRoute()
            },
            onPauseRoute = {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                if (uiState.isLocationPaused) {
                    routeViewModel.resumeLocationTracking()
                } else {
                    routeViewModel.pauseRoute()
                    LocationForegroundService.stop(context)
                }
            },
            onLoadPackages = { showLoadScanner = true },
            onToggleCami = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                if (recordAudioPermissionGranted) {
                    routeViewModel.toggleCami()
                } else {
                    recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onCenterMap = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                val center = if (LocationForegroundService.hasLocation) {
                    LatLng(
                        LocationForegroundService.lastLat,
                        LocationForegroundService.lastLng
                    )
                } else {
                    currentDelivery?.let { delivery ->
                        val latitude = delivery.latitude
                        val longitude = delivery.longitude
                        if (latitude != null && longitude != null) {
                            LatLng(latitude, longitude)
                        } else {
                            null
                        }
                    }
                }
                if (center == null) {
                    Toast.makeText(
                        context,
                        "UbicaciÃ³n GPS no disponible",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(center, 16f),
                            durationMs = 500
                        )
                    }
                }
            },
            onDismissOrderChanged = routeViewModel::dismissOrderChangedAlert,
            onClearError = routeViewModel::clearError
        )
    } else {
    // â”€â”€ MAIN HUD ANTERIOR â”€â”€
    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetContainerColor = DarkCard,
        sheetContentColor = TextPrimary,
        sheetPeekHeight = 280.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetDragHandle = null,
        sheetContent = {
            DeliveryBottomSheetContent(
                delivery = uiState.currentDelivery,
                driverToken = driverToken,
                onDeliver = { payments ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    routeViewModel.deliverCurrent(payments)
                },
                onFail = { showFailDialog = true },
                onTakePhoto = {
                    if (cameraPermissionGranted) cameraLauncher.launch(null)
                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onChat = {
                    currentDelivery?.let { onNavigateToChat(it.deliveryId, it.clientName) }
                },
                onNavigate = {
                    currentDelivery?.let { openNavigation(context, it.latitude, it.longitude) }
                },
                onSignatureRequested = { showSignatureDialog = true },
                hasSignature = uiState.capturedSignature != null,
                isDelivering = uiState.isDelivering,
                onMarkInTransit = { routeViewModel.markCurrentInTransit() },
                onScanCompleted = { routeViewModel.loadRoute() }
            )
        },
        containerColor = DarkBackground
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {

            // â•â•â• LAYER 1: Google Maps â•â•â•
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionGranted,
                    mapType = MapType.NORMAL,
                    // Estilo diurno claro y legible al sol (de noche se usa el mapa por defecto).
                    mapStyleOptions = if (!isDark) MapStyleOptions(MapUtils.DAY_MAP_STYLE) else null
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    compassEnabled = true
                )
            ) {
                // Marcadores numerados segÃºn el orden de entrega
                uiState.routeData?.deliveries?.forEach { delivery ->
                    if (delivery.latitude != null && delivery.longitude != null) {
                        val color = android.graphics.Color.parseColor(
                            when (delivery.status) {
                                "Delivered" -> "#416F5A"
                                "NotDelivered" -> "#A53E3A"
                                "InTransit" -> "#3F6383"
                                else -> "#8F3D56"
                            }
                        )

                        val markerBitmap = MapUtils.createNumberedMarkerBitmap(
                            context = context,
                            number = delivery.sortOrder,
                            color = color,
                            highlight = delivery.deliveryId == currentDelivery?.deliveryId
                        )

                        Marker(
                            state = MarkerState(LatLng(delivery.latitude, delivery.longitude)),
                            title = "${delivery.sortOrder}. ${delivery.clientName}",
                            snippet = delivery.displayAddress ?: "",
                            icon = BitmapDescriptorFactory.fromBitmap(markerBitmap)
                        )
                    }
                }

                // PolilÃ­nea de ruta
                if (uiState.polylinePoints.size > 1) {
                    Polyline(
                        points = uiState.polylinePoints,
                        color = BrandAccent,
                        width = 14f,
                        geodesic = false
                    )
                }
            }

            // â•â•â• LAYER 2: Dynamic Island â•â•â•
            DynamicIsland(
                deliveredCount = uiState.deliveredCount,
                totalCount = uiState.totalCount,
                totalCollected = uiState.totalCollected,
                isCamiListening = uiState.camiState == CamiState.LISTENING || uiState.camiState == CamiState.PROCESSING,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // â•â•â• LAYER 3: SignalR + Offline indicators (top-right) â•â•â•
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cola offline indicator
                if (uiState.pendingActionsCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(StatusPending.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = InkOnAccent, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${uiState.pendingActionsCount}", style = MaterialTheme.typography.labelSmall, color = InkOnAccent)
                        }
                    }
                }

                // SignalR dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isSignalRConnected) ActionPrimary else TextMuted)
                )
            }

            // â•â•â• LAYER 4: ETA / Distancia overlay â•â•â•
            uiState.routeInfo?.let { info ->
                if (uiState.isRouteStarted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 110.dp, start = 12.dp)
                            .background(DarkCard, RoundedCornerShape(10.dp))
                            .border(1.dp, GlassBorder.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(info.durationText, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = ActionPrimary)
                            Text(info.distanceText, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            }

            // â•â•â• LAYER 5: Next Destination Overlay â•â•â•
            if (currentDelivery != null && uiState.isRouteStarted) {
                GlassCard(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 150.dp)
                        .padding(horizontal = 20.dp),
                    cornerRadius = 16.dp,
                    backgroundAlpha = 1.0f,
                    containerColor = DarkCard
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(BrandAccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${currentDelivery.sortOrder}", style = MaterialTheme.typography.labelLarge, color = BrandAccent, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Siguiente destino", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                currentDelivery.displayAddress ?: currentDelivery.clientName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            openNavigation(context, currentDelivery.latitude, currentDelivery.longitude)
                        }) {
                            Icon(Icons.Filled.Navigation, contentDescription = "Navegar", tint = ActionPrimary)
                        }
                    }
                }
            }

            // â•â•â• LAYER 6: CAMI Overlay â•â•â•
            CamiOverlay(
                camiState = uiState.camiState,
                transcript = uiState.camiTranscript,
                response = uiState.camiResponse,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 300.dp)
                    .padding(horizontal = 24.dp)
            )

            // â•â•â• LAYER 7: Start Route Button â•â•â•
            if (!uiState.isRouteStarted && uiState.routeData != null) {
                val allRoutePackages = remember(uiState.routeData) {
                    uiState.routeData?.deliveries?.flatMap { it.packages ?: emptyList() } ?: emptyList()
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 290.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // BotÃ³n "Cargar bolsas" â€” solo si la ruta tiene paquetes con QR
                    if (allRoutePackages.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showLoadScanner = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ActionPrimary),
                            border = BorderStroke(1.5.dp, ActionPrimary.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Outlined.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "CARGAR BOLSAS (${allRoutePackages.size})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            safeStartRoute()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAccent, contentColor = InkOnAccent),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("INICIAR RUTA", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp))
                    }
                }

                // Scanner overlay â€” carga de bolsas al camiÃ³n
                if (showLoadScanner) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = {
                            showLoadScanner = false
                            routeViewModel.loadRoute()
                        },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        PackageScannerOverlay(
                            packages = allRoutePackages,
                            driverToken = driverToken,
                            action = "Load",
                            onDismiss = {
                                showLoadScanner = false
                                routeViewModel.loadRoute()
                            }
                        )
                    }
                }
            }

            // â•â•â• LAYER 8: FABs â•â•â•
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pausar (Kill Switch)
                if (uiState.isRouteStarted) {
                    GlassFab(
                        icon = if (uiState.isLocationPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (uiState.isLocationPaused) "Reanudar ubicaciÃ³n" else "Pausar ubicaciÃ³n",
                        tint = StatusFailed // Usando rojo/fallo para advertencia
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        if (uiState.isLocationPaused) {
                            routeViewModel.resumeLocationTracking()
                        } else {
                            routeViewModel.pauseRoute()
                            LocationForegroundService.stop(context)
                        }
                    }
                }

                // C.A.M.I. â€” con animaciÃ³n pulsante cuando escucha
                val camiPulse by rememberInfiniteTransition(label = "cami_pulse").animateFloat(
                    initialValue = 1f, targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label = "cami_scale"
                )
                val isActive = uiState.camiState == CamiState.LISTENING || uiState.camiState == CamiState.PROCESSING
                Box(modifier = Modifier.scale(if (isActive) camiPulse else 1f)) {
                    GlassFab(
                        icon = if (isActive) Icons.Filled.MicOff else Icons.Filled.AutoAwesome,
                        contentDescription = "C.A.M.I.",
                        tint = if (isActive) BrandAccent else TextSecondary
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        if (recordAudioPermissionGranted) {
                            routeViewModel.toggleCami()
                        } else {
                            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }

                // Chat Admin (con badge si hay mensaje nuevo)
                Box {
                    GlassFab(
                        icon = Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = "Chat Admin",
                        tint = if (uiState.hasNewChatMessage) BrandAccent else StatusInTransit
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        routeViewModel.dismissChatAlert()
                        onNavigateToChat(0, "Admin")
                    }
                    if (uiState.hasNewChatMessage) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(BrandAccent)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                // Gastos
                GlassFab(icon = Icons.Outlined.Receipt, contentDescription = "Gastos", tint = DepositColor) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    showExpenseDialog = true
                }

                // Reordenar
                GlassFab(icon = Icons.Outlined.SwapVert, contentDescription = "Reordenar", tint = StatusPending) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    showReorderDialog = true
                }

                // Centrar mapa
                GlassFab(icon = Icons.Filled.MyLocation, contentDescription = "Centrar", tint = TextSecondary) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    if (LocationForegroundService.hasLocation) {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(LocationForegroundService.lastLat, LocationForegroundService.lastLng),
                                    16f
                                )
                            )
                        }
                    } else {
                        // Fallback a la entrega actual si no hay GPS aÃºn
                        currentDelivery?.let {
                            if (it.latitude != null && it.longitude != null) {
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
                                    )
                                }
                            } else {
                                Toast.makeText(context, "UbicaciÃ³n GPS no disponible", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            Toast.makeText(context, "UbicaciÃ³n GPS no disponible", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // â•â•â• LAYER 9b: Proximidad Banner con Street View â•â•â•
            AnimatedVisibility(
                visible = uiState.proximityAlert,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 86.dp)
                    .padding(horizontal = 16.dp),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                val proximityDelivery = uiState.currentDelivery
                val hasCoords = proximityDelivery?.latitude != null && proximityDelivery.longitude != null
                val streetViewUrl = if (hasCoords && BuildConfig.MAPS_API_KEY.isNotBlank()) {
                    "https://maps.googleapis.com/maps/api/streetview" +
                        "?size=600x200" +
                        "&location=${proximityDelivery!!.latitude},${proximityDelivery.longitude}" +
                        "&fov=80&pitch=0" +
                        "&key=${BuildConfig.MAPS_API_KEY}"
                } else null

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .border(1.5.dp, ActionPrimary.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                ) {
                    // Street View image
                    if (streetViewUrl != null) {
                        AsyncImage(
                            model = streetViewUrl,
                            contentDescription = "Vista de la calle",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    // Info row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ActionPrimary)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Â¡Llegando con ${uiState.proximityDeliveryName}!",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = DarkBackground
                            )
                            if (!proximityDelivery?.displayAddress.isNullOrBlank()) {
                                Text(
                                    proximityDelivery!!.displayAddress!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkBackground.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if ((proximityDelivery?.balanceDue ?: 0.0) > 0) {
                            Text(
                                "$${String.format("%,.0f", proximityDelivery!!.balanceDue)}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = DarkBackground
                            )
                        }
                    }
                }
            }

            // â•â•â• LAYER 9c: Pedido modificado por admin â•â•â•
            AnimatedVisibility(
                visible = uiState.orderChangedAlert && !uiState.proximityAlert,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 86.dp)
                    .padding(horizontal = 16.dp),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StatusPending)
                        .clickable { routeViewModel.dismissOrderChangedAlert() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = InkOnAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "âš ï¸ Pedido actualizado por admin",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = InkOnAccent
                        )
                        val balanceText = if (uiState.orderChangedBalanceDue <= 0)
                            "${uiState.orderChangedClientName} Â· Saldo: PAGADO"
                        else
                            "${uiState.orderChangedClientName} Â· Cobrar: $${String.format("%,.0f", uiState.orderChangedBalanceDue)}"
                        Text(
                            balanceText,
                            style = MaterialTheme.typography.labelMedium,
                            color = InkOnAccent.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Cerrar",
                        tint = InkOnAccent.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // â•â•â• LAYER 9a: Sin internet banner â•â•â•
            AnimatedVisibility(
                visible = uiState.isOffline && !uiState.proximityAlert,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 86.dp),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .background(StatusFailed.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.WifiOff,
                            contentDescription = null,
                            tint = InkOnAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Sin internet â€” acciones se guardarÃ¡n",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkOnAccent
                        )
                    }
                }
            }

            // â•â•â• LAYER 9: Offline pending banner â•â•â•
            AnimatedVisibility(
                visible = uiState.pendingActionsCount > 0,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 110.dp),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .background(StatusPending.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${uiState.pendingActionsCount} acciÃ³n(es) pendiente(s) â€” sin internet",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkOnAccent
                    )
                }
            }

            // â•â•â• LAYER 10: Indicadores de evidencia (foto + firma) â•â•â•
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (uiState.capturedPhoto != null) {
                    Box(
                        modifier = Modifier
                            .background(ActionPrimary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Foto lista", style = MaterialTheme.typography.labelSmall, color = DarkBackground)
                        }
                    }
                }
                if (uiState.capturedSignature != null) {
                    Box(
                        modifier = Modifier
                            .background(BrandAccent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Draw, contentDescription = null, tint = InkOnAccent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Firma lista", style = MaterialTheme.typography.labelSmall, color = InkOnAccent)
                        }
                    }
                }
            }

            // â•â•â• LAYER 11: Error Snackbar â•â•â•
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 296.dp)
                        .padding(horizontal = 16.dp),
                    containerColor = StatusFailed.copy(alpha = 0.9f),
                    contentColor = InkOnAccent,
                    dismissAction = {
                        IconButton(onClick = { routeViewModel.clearError() }) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = InkOnAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                ) {
                    Text(error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // â•â•â• LAYER 12: CelebraciÃ³n Lottie de entrega â•â•â•
            DeliverySuccessOverlay(
                visible = showDeliverySuccess,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
    }
}

// â”€â”€â”€ CAMI OVERLAY â”€â”€â”€

@Composable
private fun CamiOverlay(
    camiState: CamiState,
    transcript: String?,
    response: String?,
    modifier: Modifier = Modifier
) {
    val visible = camiState == CamiState.LISTENING || camiState == CamiState.PROCESSING ||
            camiState == CamiState.SPEAKING || response != null || transcript != null

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        GlassCard(cornerRadius = 16.dp, backgroundAlpha = 0.18f) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("âœ¦ C.A.M.I.", style = MaterialTheme.typography.labelLarge, color = BrandAccent, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (camiState) {
                            CamiState.LISTENING -> "Escuchando..."
                            CamiState.PROCESSING -> "Procesando..."
                            CamiState.SPEAKING -> "Respondiendo..."
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                if (transcript != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "\"$transcript\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                if (response != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        response,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                if (camiState == CamiState.LISTENING || camiState == CamiState.PROCESSING) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = BrandAccent,
                        trackColor = BrandAccentSoft
                    )
                }
            }
        }
    }
}

// â”€â”€â”€ DIALOGS â”€â”€â”€

@Composable
private fun FailReasonDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val palette = regiBazar2026Palette()
    var customReason by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("") }
    val reasons = listOf("Cliente no disponible", "DirecciÃ³n incorrecta", "Pedido rechazado por cliente", "Domicilio cerrado", "Zona de riesgo", "Otro")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = {
            Text(
                "RazÃ³n de falla",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason }
                            .background(
                                if (selectedReason == reason) palette.accentSoft else Color.Transparent
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = palette.accent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            reason,
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (selectedReason == "Otro") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Especifica la razÃ³n") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.divider,
                            cursorColor = palette.accent,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reason = if (selectedReason == "Otro") customReason.ifEmpty { "Otro" } else selectedReason
                    if (reason.isNotEmpty()) onConfirm(reason)
                },
                enabled = selectedReason.isNotEmpty() && (selectedReason != "Otro" || customReason.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.error,
                    contentColor = Color(0xFFF8FAFF)
                )
            ) { Text("Reportar falla") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = palette.textSecondary)
            }
        }
    )
}

// â”€â”€â”€ GLASS FAB â”€â”€â”€
@Composable
private fun GlassFab(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, tint: Color, onClick: () -> Unit) {
    GlassCard(cornerRadius = 16.dp, backgroundAlpha = 0.80f, contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

// â”€â”€â”€ HELPERS â”€â”€â”€
private fun formatDoorTime(secs: Long): String = when {
    secs < 60 -> "${secs}s"
    secs < 3600 -> "${secs / 60}min ${secs % 60}s"
    else -> "${secs / 3600}h ${(secs % 3600) / 60}min"
}

private fun openNavigation(context: Context, lat: Double?, lng: Double?) {
    if (lat == null || lng == null) {
        Toast.makeText(context, "El destino no tiene coordenadas GPS vÃ¡lidas", Toast.LENGTH_SHORT).show()
        return
    }
    val intentGoogle = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng")).setPackage("com.google.android.apps.maps")
    val intentWaze = Intent(Intent.ACTION_VIEW, Uri.parse("waze://?ll=$lat,$lng&navigate=yes")).setPackage("com.waze")
    try { context.startActivity(intentGoogle) } catch (e: Exception) {
        try { context.startActivity(intentWaze) } catch (e2: Exception) {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng"))) } catch (e3: Exception) {
                Toast.makeText(context, "No hay apps de mapas instaladas", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

