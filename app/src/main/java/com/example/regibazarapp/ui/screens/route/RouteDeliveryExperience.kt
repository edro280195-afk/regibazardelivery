package com.example.regibazarapp.ui.screens.route

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.regibazarapp.data.models.PaymentInputDto
import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.service.CamiState
import com.example.regibazarapp.ui.components.DeliveryBottomSheetContent
import com.example.regibazarapp.ui.components.DeliverySuccessOverlay
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.theme.RegiBazar2026Tokens
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import com.example.regibazarapp.ui.utils.MapUtils
import com.example.regibazarapp.ui.utils.formatRouteMoney
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDeliveryExperience(
    uiState: RouteUiState,
    driverToken: String,
    locationPermissionGranted: Boolean,
    isDark: Boolean,
    cameraPositionState: CameraPositionState,
    sheetState: BottomSheetScaffoldState,
    showDeliverySuccess: Boolean,
    onBack: () -> Unit,
    onDeliver: (List<PaymentInputDto>) -> Unit,
    onFail: () -> Unit,
    onTakePhoto: () -> Unit,
    onChatDelivery: () -> Unit,
    onChatAdmin: () -> Unit,
    onNavigate: () -> Unit,
    onSignatureRequested: () -> Unit,
    onMarkInTransit: () -> Unit,
    onScanCompleted: () -> Unit,
    onExpense: () -> Unit,
    onReorder: () -> Unit,
    onStartRoute: () -> Unit,
    onPauseRoute: () -> Unit,
    onLoadPackages: () -> Unit,
    onToggleCami: () -> Unit,
    onCenterMap: () -> Unit,
    onDismissOrderChanged: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val palette = regiBazar2026Palette()
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    val currentDelivery = uiState.currentDelivery
    val allPackagesCount = remember(uiState.routeData) {
        uiState.routeData?.deliveries
            ?.sumOf { delivery -> delivery.packages?.size ?: 0 }
            ?: 0
    }

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetContainerColor = palette.surface,
        sheetContentColor = palette.textPrimary,
        sheetPeekHeight = 330.dp,
        sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        sheetDragHandle = null,
        sheetContent = {
            DeliveryBottomSheetContent(
                delivery = currentDelivery,
                driverToken = driverToken,
                totalStops = uiState.totalCount,
                onDeliver = onDeliver,
                onFail = onFail,
                onTakePhoto = onTakePhoto,
                onChat = onChatDelivery,
                onNavigate = onNavigate,
                onSignatureRequested = onSignatureRequested,
                hasSignature = uiState.capturedSignature != null,
                hasPhoto = uiState.capturedPhoto != null,
                hasNewChatMessage = uiState.hasNewChatMessage,
                isDelivering = uiState.isDelivering,
                onMarkInTransit = onMarkInTransit,
                onScanCompleted = onScanCompleted,
                onExpense = onExpense
            )
        },
        containerColor = palette.background
    ) {
        Box(Modifier.fillMaxSize()) {
            DriverRouteMap(
                uiState = uiState,
                currentDelivery = currentDelivery,
                locationPermissionGranted = locationPermissionGranted,
                isDark = isDark,
                mapType = mapType,
                cameraPositionState = cameraPositionState,
                palette = palette,
                context = context
            )

            DriverProgressHeader(
                deliveredCount = uiState.deliveredCount,
                failedCount = uiState.failedCount,
                totalCount = uiState.totalCount,
                totalCollected = uiState.totalCollected,
                isRouteStarted = uiState.isRouteStarted,
                isLocationPaused = uiState.isLocationPaused,
                palette = palette,
                onBack = onBack,
                onPause = onPauseRoute
            )

            RouteContextBanner(
                uiState = uiState,
                palette = palette,
                onDismissOrderChanged = onDismissOrderChanged,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 104.dp, start = 14.dp, end = 70.dp)
            )

            val hasContextBanner = uiState.proximityAlert ||
                uiState.orderChangedAlert ||
                uiState.isLocationPaused ||
                uiState.isOffline ||
                uiState.pendingActionsCount > 0
            uiState.routeInfo
                ?.takeIf { uiState.isRouteStarted && !hasContextBanner }
                ?.let { info ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 104.dp, start = 14.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = palette.surface.copy(alpha = 0.96f),
                    contentColor = palette.textPrimary,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            info.durationText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.accent
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            info.distanceText,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 350.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                RouteMapButton(
                    icon = Icons.Rounded.Layers,
                    contentDescription = "Cambiar tipo de mapa",
                    palette = palette,
                    onClick = {
                        mapType = if (mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
                    }
                )
                RouteMapButton(
                    icon = Icons.Rounded.MyLocation,
                    contentDescription = "Centrar mapa",
                    palette = palette,
                    onClick = onCenterMap
                )
                RouteMapButton(
                    icon = Icons.Rounded.SwapVert,
                    contentDescription = "Reordenar paradas",
                    palette = palette,
                    onClick = onReorder
                )
                Box {
                    RouteMapButton(
                        icon = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "Chat con administración",
                        palette = palette,
                        onClick = onChatAdmin
                    )
                    if (uiState.hasNewChatMessage) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(10.dp)
                                .background(palette.error, CircleShape)
                        )
                    }
                }
                RouteMapButton(
                    icon = if (
                        uiState.camiState == CamiState.LISTENING ||
                        uiState.camiState == CamiState.PROCESSING
                    ) {
                        Icons.Rounded.MicOff
                    } else {
                        Icons.Rounded.AutoAwesome
                    },
                    contentDescription = "C.A.M.I.",
                    palette = palette,
                    active = uiState.camiState != CamiState.IDLE,
                    onClick = onToggleCami
                )
            }

            if (!uiState.isRouteStarted && uiState.routeData != null) {
                StartRoutePanel(
                    packageCount = allPackagesCount,
                    palette = palette,
                    onLoadPackages = onLoadPackages,
                    onStartRoute = onStartRoute,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 18.dp, end = 70.dp, bottom = 350.dp)
                )
            }

            if (uiState.isRouteStarted) {
                CamiStatusCard(
                    state = uiState.camiState,
                    transcript = uiState.camiTranscript,
                    response = uiState.camiResponse,
                    palette = palette,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 18.dp, end = 70.dp, bottom = 350.dp)
                )
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 350.dp),
                    containerColor = palette.error,
                    contentColor = Color(0xFFF8FAFF),
                    dismissAction = {
                        TextButton(onClick = onClearError) {
                            Text("Cerrar", color = Color(0xFFF8FAFF))
                        }
                    }
                ) {
                    Text(error, style = MaterialTheme.typography.bodySmall)
                }
            }

            DeliverySuccessOverlay(
                visible = showDeliverySuccess,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun DriverRouteMap(
    uiState: RouteUiState,
    currentDelivery: RouteDeliveryDto?,
    locationPermissionGranted: Boolean,
    isDark: Boolean,
    mapType: MapType,
    cameraPositionState: CameraPositionState,
    palette: RegiBazar2026Palette,
    context: Context
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        contentPadding = PaddingValues(top = 124.dp, bottom = 330.dp),
        properties = MapProperties(
            isMyLocationEnabled = locationPermissionGranted,
            mapType = mapType,
            mapStyleOptions = if (!isDark && mapType == MapType.NORMAL) {
                MapStyleOptions(MapUtils.DAY_MAP_STYLE)
            } else {
                null
            }
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = true
        )
    ) {
        uiState.routeData?.deliveries?.forEach { delivery ->
            val latitude = delivery.latitude
            val longitude = delivery.longitude
            if (latitude != null && longitude != null) {
                val markerColor = when (delivery.status) {
                    "Delivered" -> palette.success
                    "NotDelivered" -> palette.error
                    "InTransit" -> palette.accent
                    else -> palette.accent
                }
                val marker = MapUtils.createNumberedMarkerBitmap(
                    context = context,
                    number = delivery.sortOrder,
                    color = markerColor.toArgb(),
                    highlight = delivery.deliveryId == currentDelivery?.deliveryId
                )
                Marker(
                    state = MarkerState(LatLng(latitude, longitude)),
                    title = "${delivery.sortOrder}. ${delivery.clientName}",
                    snippet = delivery.displayAddress.orEmpty(),
                    icon = BitmapDescriptorFactory.fromBitmap(marker)
                )
            }
        }

        if (uiState.polylinePoints.size > 1) {
            Polyline(
                points = uiState.polylinePoints,
                color = palette.accent,
                width = 13f,
                geodesic = false
            )
        }
    }
}

@Composable
private fun DriverProgressHeader(
    deliveredCount: Int,
    failedCount: Int,
    totalCount: Int,
    totalCollected: Double,
    isRouteStarted: Boolean,
    isLocationPaused: Boolean,
    palette: RegiBazar2026Palette,
    onBack: () -> Unit,
    onPause: () -> Unit
) {
    val completed = completedStops(deliveredCount, failedCount, totalCount)
    val progress = routeProgressFraction(deliveredCount, failedCount, totalCount)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RouteMapButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Regresar",
            palette = palette,
            onClick = onBack
        )
        Spacer(Modifier.width(8.dp))

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            color = palette.surface.copy(alpha = 0.97f),
            shadowElevation = 4.dp
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "COMPLETADAS",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = palette.textSecondary
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                completed.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary
                            )
                            Text(
                                "/$totalCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(palette.divider)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "COBRADO",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = palette.textSecondary
                        )
                        Text(
                            formatRouteMoney(totalCollected),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.money,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(5.dp),
                    color = palette.accent,
                    trackColor = palette.divider,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }

        if (isRouteStarted) {
            Spacer(Modifier.width(8.dp))
            RouteMapButton(
                icon = if (isLocationPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (isLocationPaused) "Reanudar ubicación" else "Pausar ubicación",
                palette = palette,
                danger = !isLocationPaused,
                onClick = onPause
            )
        }
    }
}

@Composable
private fun RouteContextBanner(
    uiState: RouteUiState,
    palette: RegiBazar2026Palette,
    onDismissOrderChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content = when {
        uiState.proximityAlert -> Triple(
            "Llegaste a ${uiState.proximityDeliveryName}",
            Icons.Rounded.MyLocation,
            palette.success
        )
        uiState.orderChangedAlert -> Triple(
            "${uiState.orderChangedClientName}: pedido actualizado",
            Icons.Rounded.Refresh,
            palette.warning
        )
        uiState.isLocationPaused -> Triple(
            "Ubicación pausada: la ruta sigue activa",
            Icons.Rounded.Pause,
            palette.warning
        )
        uiState.isOffline -> Triple(
            "Sin internet: las acciones se guardarán",
            Icons.Rounded.CloudOff,
            palette.error
        )
        uiState.pendingActionsCount > 0 -> Triple(
            "${uiState.pendingActionsCount} por sincronizar",
            Icons.Rounded.CloudDone,
            palette.warning
        )
        else -> null
    }

    AnimatedVisibility(
        visible = content != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        content?.let { (message, icon, color) ->
            Surface(
                modifier = Modifier.then(
                    if (uiState.orderChangedAlert) {
                        Modifier.clickable(onClick = onDismissOrderChanged)
                    } else {
                        Modifier
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                color = palette.surface.copy(alpha = 0.97f),
                contentColor = palette.textPrimary,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StartRoutePanel(
    packageCount: Int,
    palette: RegiBazar2026Palette,
    onLoadPackages: () -> Unit,
    onStartRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (packageCount > 0) {
            Surface(
                onClick = onLoadPackages,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = palette.surface,
                contentColor = palette.accent,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Inventory2, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cargar $packageCount bolsas", fontWeight = FontWeight.Bold)
                }
            }
        }
        Button(
            onClick = onStartRoute,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accent,
                contentColor = Color(0xFFF8FAFF)
            )
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Iniciar ruta", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun CamiStatusCard(
    state: CamiState,
    transcript: String?,
    response: String?,
    palette: RegiBazar2026Palette,
    modifier: Modifier = Modifier
) {
    val visible = state != CamiState.IDLE || transcript != null || response != null
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.surface.copy(alpha = 0.98f),
            shadowElevation = 4.dp
        ) {
            Column(Modifier.padding(13.dp)) {
                Text(
                    text = when (state) {
                        CamiState.LISTENING -> "C.A.M.I. está escuchando"
                        CamiState.PROCESSING -> "C.A.M.I. está procesando"
                        CamiState.SPEAKING -> "C.A.M.I. está respondiendo"
                        else -> "C.A.M.I."
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.accent
                )
                Text(
                    text = response ?: transcript.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RouteMapButton(
    icon: ImageVector,
    contentDescription: String,
    palette: RegiBazar2026Palette,
    active: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val tint = when {
        danger -> palette.error
        active -> palette.accent
        else -> palette.textPrimary
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget),
        shape = RoundedCornerShape(14.dp),
        color = if (active) palette.accentSoft else palette.surface.copy(alpha = 0.97f),
        contentColor = tint,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun DriverRouteLoadingScreen() {
    val palette = regiBazar2026Palette()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.surfaceMuted)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(palette.surface, RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp)
                    .background(palette.surface, RoundedCornerShape(20.dp))
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    palette.surface,
                    RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(42.dp)
                    .height(5.dp)
                    .background(palette.surfaceMuted, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(26.dp)
                    .background(palette.surfaceMuted, CircleShape)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(30.dp)
                    .background(palette.surfaceMuted, RoundedCornerShape(9.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(palette.surfaceMuted, RoundedCornerShape(16.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(palette.surfaceMuted, RoundedCornerShape(16.dp))
            )
        }
    }
}
