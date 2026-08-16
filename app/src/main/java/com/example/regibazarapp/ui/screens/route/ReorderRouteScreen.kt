package com.example.regibazarapp.ui.screens.route

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.theme.RegiBazar2026Tokens
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import com.example.regibazarapp.ui.utils.MapUtils
import com.example.regibazarapp.ui.utils.formatRouteMoney
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class RouteStats(
    val distanceKm: Double,
    val estimatedMinutes: Int
)

private data class ReorderMapStop(
    val delivery: RouteDeliveryDto,
    val position: Int,
    val latLng: LatLng
)

@Composable
fun ReorderRouteDialog(
    deliveries: List<RouteDeliveryDto>,
    onConfirm: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = regiBazar2026Palette()
    val reorderable = remember(deliveries) {
        deliveries.filter { it.status == "Pending" || it.status == "InTransit" }
            .sortedBy { it.sortOrder }
            .toMutableStateList()
    }
    if (reorderable.isEmpty()) {
        onDismiss()
        return
    }

    val reorderHistory = remember { mutableStateListOf<List<RouteDeliveryDto>>() }
    val scope = rememberCoroutineScope()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var longPressIndex by remember { mutableStateOf<Int?>(null) }
    var showOptimizeModal by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastVisible by remember { mutableStateOf(false) }

    val reorderKey by remember {
        derivedStateOf { reorderable.joinToString("|") { it.deliveryId.toString() } }
    }
    val currentStats = remember(reorderKey) {
        computeRouteStats(reorderable)
    }

    fun snapshot() {
        reorderHistory.add(reorderable.toList())
        if (reorderHistory.size > 20) reorderHistory.removeAt(0)
    }

    fun showToast(message: String) {
        toastMessage = message
        toastVisible = true
        scope.launch {
            delay(4000)
            toastVisible = false
        }
    }

    fun swapStops(indexA: Int, indexB: Int) {
        if (indexA == indexB) return
        snapshot()
        val tmp = reorderable[indexA]
        reorderable[indexA] = reorderable[indexB]
        reorderable[indexB] = tmp
    }

    fun moveStopTo(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        snapshot()
        val item = reorderable.removeAt(fromIndex)
        reorderable.add(toIndex, item)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
            ReorderMapPreview(
                deliveries = reorderable,
                selectedIndex = selectedIndex,
                orderKey = reorderKey,
                modifier = Modifier.fillMaxSize()
            )

            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                ReorderTopBar(
                    palette = palette,
                    onClose = onDismiss,
                    onOptimize = { showOptimizeModal = true }
                )

                Spacer(Modifier.height(12.dp))

                ReorderStatsCard(
                    palette = palette,
                    stats = currentStats,
                    stopCount = reorderable.size
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = longPressIndex != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    val target = longPressIndex
                    if (target != null) {
                        val pos = (target + 1).toFloat() / reorderable.size
                        ReorderQuickActions(
                            palette = palette,
                            onDismiss = { longPressIndex = null },
                            canMoveUp = target > 0,
                            canMoveDown = target < reorderable.size - 1,
                            onMoveToStart = {
                                moveStopTo(target, 0)
                                longPressIndex = null
                                showToast("Movido al inicio")
                            },
                            onMoveUp = {
                                moveStopTo(target, target - 1)
                                longPressIndex = null
                                showToast("Subido una posición")
                            },
                            onMoveDown = {
                                moveStopTo(target, target + 1)
                                longPressIndex = null
                                showToast("Bajado una posición")
                            },
                            onMoveToEnd = {
                                moveStopTo(target, reorderable.size - 1)
                                longPressIndex = null
                                showToast("Movido al final")
                            },
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                        )
                    }
                }

                ReorderBottomSheet(
                    palette = palette,
                    stops = reorderable,
                    selectedIndex = selectedIndex,
                    onTap = { index ->
                        val current = selectedIndex
                        when {
                            current == null -> {
                                selectedIndex = index
                                showToast("Toca otra parada para intercambiar")
                            }
                            current == index -> {
                                selectedIndex = null
                                toastVisible = false
                            }
                            else -> {
                                val currentList: List<RouteDeliveryDto> = reorderable.toList()
                                val nameA = currentList[current!!].clientName
                                val nameB = currentList[index].clientName
                                swapStops(current, index)
                                selectedIndex = null
                                showToast("Intercambiaste ${shortName(nameA)} ↔ ${shortName(nameB)}")
                            }
                        }
                    },
                    onLongPress = { index ->
                        longPressIndex = index
                    },
                    onSave = {
                        val nonReorderableIds = deliveries
                            .filterNot { it.status == "Pending" || it.status == "InTransit" }
                            .sortedBy { it.sortOrder }
                            .map { it.deliveryId }
                        val fullOrderedIds = nonReorderableIds + reorderable.map { it.deliveryId }
                        onConfirm(fullOrderedIds)
                    }
                )
            }

            AnimatedVisibility(
                visible = toastVisible && toastMessage != null && longPressIndex == null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 180.dp, start = 16.dp, end = 16.dp)
            ) {
                val msg = toastMessage
                if (msg != null) {
                    ReorderToast(
                        palette = palette,
                        message = msg,
                        onUndo = if (reorderHistory.isNotEmpty()) {
                            {
                                val previous = reorderHistory.removeAt(reorderHistory.size - 1)
                                reorderable.clear()
                                reorderable.addAll(previous)
                                showToast("Cambio deshecho")
                            }
                        } else null
                    )
                }
            }

            if (showOptimizeModal) {
                val orderList: List<RouteDeliveryDto> = reorderable.toList()
                OptimizePreviewModal(
                    palette = palette,
                    currentStats = currentStats,
                    currentOrder = orderList,
                    onApply = { newOrder ->
                        snapshot()
                        reorderable.clear()
                        reorderable.addAll(newOrder)
                        showOptimizeModal = false
                        selectedIndex = null
                        showToast("Ruta optimizada aplicada")
                    },
                    onDismiss = { showOptimizeModal = false }
                )
            }
        }
}

@Composable
private fun ReorderTopBar(
    palette: RegiBazar2026Palette,
    onClose: () -> Unit,
    onOptimize: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onClose,
            shape = RoundedCornerShape(14.dp),
            color = palette.surface.copy(alpha = 0.95f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Cerrar",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.surface.copy(alpha = 0.95f),
            modifier = Modifier.height(44.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Reordenar ruta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(
            onClick = onOptimize,
            shape = RoundedCornerShape(14.dp),
            color = palette.accent,
            contentColor = Color(0xFFF8FAFF),
            modifier = Modifier.height(44.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Optimizar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ReorderStatsCard(
    palette: RegiBazar2026Palette,
    stats: RouteStats,
    stopCount: Int
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surface.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn(
                label = "Distancia",
                value = formatKm(stats.distanceKm),
                palette = palette,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(palette.divider)
            )
            StatColumn(
                label = "Tiempo est.",
                value = "${stats.estimatedMinutes} min",
                palette = palette,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(palette.divider)
            )
            StatColumn(
                label = "Paradas",
                value = stopCount.toString(),
                palette = palette,
                modifier = Modifier.weight(1f),
                accent = true
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    palette: RegiBazar2026Palette,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.8.sp
            ),
            color = palette.textSecondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (accent) palette.accent else palette.textPrimary
        )
    }
}

@Composable
private fun ReorderMapPreview(
    deliveries: List<RouteDeliveryDto>,
    selectedIndex: Int?,
    orderKey: String,
    modifier: Modifier = Modifier
) {
    val palette = regiBazar2026Palette()
    val context = LocalContext.current
    val isDark = palette.background.luminance() < 0.5f
    val locatedStops = remember(orderKey) {
        deliveries.mapIndexedNotNull { index, delivery ->
            val latitude = delivery.latitude
            val longitude = delivery.longitude
            if (latitude == null || longitude == null) {
                null
            } else {
                ReorderMapStop(
                    delivery = delivery,
                    position = index + 1,
                    latLng = LatLng(latitude, longitude)
                )
            }
        }
    }
    val initialPosition = locatedStops.firstOrNull()?.latLng ?: LatLng(25.6866, -100.3161)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, if (locatedStops.size > 1) 12f else 15f)
    }

    LaunchedEffect(orderKey, locatedStops.size) {
        when (locatedStops.size) {
            0 -> Unit
            1 -> cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(locatedStops.first().latLng, 15f),
                durationMs = 500
            )
            else -> {
                val boundsBuilder = LatLngBounds.builder()
                locatedStops.forEach { boundsBuilder.include(it.latLng) }
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 140),
                    durationMs = 700
                )
            }
        }
    }

    LaunchedEffect(selectedIndex, orderKey) {
        val stop = selectedIndex?.let { index -> locatedStops.firstOrNull { it.position == index + 1 } }
        if (stop != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(stop.latLng, 16f),
                durationMs = 450
            )
        }
    }

    var routePolylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    LaunchedEffect(orderKey) {
        val stopLatLngs = locatedStops.map { it.latLng }
        if (stopLatLngs.size > 1) {
            val roadPoints = com.example.regibazarapp.ui.utils.DirectionsManager.getMultiStopPolyline(stopLatLngs)
            routePolylinePoints = if (roadPoints.isNotEmpty()) roadPoints else stopLatLngs
        } else {
            routePolylinePoints = emptyList()
        }
    }

    Box(modifier = modifier.background(palette.background)) {
        if (locatedStops.isEmpty()) {
            ReorderMapEmptyState(palette = palette, modifier = Modifier.fillMaxSize())
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                contentPadding = PaddingValues(top = 158.dp, bottom = 420.dp, start = 12.dp, end = 12.dp),
                properties = MapProperties(
                    mapType = MapType.NORMAL,
                    mapStyleOptions = if (!isDark) MapStyleOptions(MapUtils.DAY_MAP_STYLE) else null
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    compassEnabled = true
                )
            ) {
                if (routePolylinePoints.size > 1) {
                    Polyline(
                        points = routePolylinePoints,
                        color = palette.accent,
                        width = 10f,
                        geodesic = true
                    )
                }

                locatedStops.forEach { stop ->
                    val isSelected = selectedIndex == stop.position - 1
                    val markerColor = when (stop.delivery.status) {
                        "InTransit" -> palette.warning
                        else -> palette.accent
                    }
                    val markerBitmap = MapUtils.createNumberedMarkerBitmap(
                        context = context,
                        number = stop.position,
                        color = markerColor.toArgb(),
                        highlight = isSelected
                    )
                    Marker(
                        state = MarkerState(stop.latLng),
                        title = "${stop.position}. ${stop.delivery.clientName}",
                        snippet = stop.delivery.displayAddress.orEmpty(),
                        icon = BitmapDescriptorFactory.fromBitmap(markerBitmap)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReorderMapEmptyState(
    palette: RegiBazar2026Palette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(palette.surfaceMuted),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Sin ubicaciones para mostrar en el mapa",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )
    }
}

@Composable
private fun ReorderMapMarker(
    position: Offset,
    number: Int,
    color: Color,
    isSelected: Boolean
) {
    val palette = regiBazar2026Palette()
    val markerSize = if (isSelected) 48.dp else 36.dp

    Box(
        modifier = Modifier
            .offset(
                x = (position.x - 18).dp,
                y = (position.y - 18).dp
            )
            .size(markerSize)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .background(
                        color = color.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color, CircleShape)
                .border(2.dp, palette.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isSelected) 15.sp else 13.sp
            )
        }
    }
}

@Composable
private fun ReorderBottomSheet(
    palette: RegiBazar2026Palette,
    stops: SnapshotStateList<RouteDeliveryDto>,
    selectedIndex: Int?,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    onSave: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = palette.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 10.dp)
                    .width(42.dp)
                    .height(5.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(palette.surfaceMuted, CircleShape)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hintText = when {
                    selectedIndex != null -> "Toca otra parada para intercambiar"
                    else -> "Toca 2 paradas para intercambiarlas"
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedIndex != null) palette.accentSoft else palette.surfaceMuted.copy(alpha = 0.6f)
                ) {
                    Text(
                        hintText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex != null) palette.accent else palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${stops.size} pendientes",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = stops,
                    key = { index, stop -> "${stop.deliveryId}-$index" }
                ) { index, stop ->
                    ReorderStopRow(
                        palette = palette,
                        delivery = stop,
                        position = index + 1,
                        isSelected = selectedIndex == index,
                        onTap = { onTap(index) },
                        onLongPress = { onLongPress(index) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color(0xFFF8FAFF)
                )
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Guardar orden", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ReorderStopRow(
    palette: RegiBazar2026Palette,
    delivery: RouteDeliveryDto,
    position: Int,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) palette.accent else Color.Transparent,
        animationSpec = tween(180),
        label = "stopBorder"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) palette.accentSoft else palette.surfaceMuted.copy(alpha = 0.55f),
        animationSpec = tween(180),
        label = "stopBackground"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) palette.accent else palette.surface,
                        CircleShape
                    )
                    .border(1.5.dp, palette.accent.copy(alpha = if (isSelected) 0f else 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    position.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) Color.White else palette.accent
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    delivery.clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    delivery.displayAddress ?: "Sin dirección",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            StopStatusBadge(palette = palette, delivery = delivery)
        }
    }
}

@Composable
private fun StopStatusBadge(
    palette: RegiBazar2026Palette,
    delivery: RouteDeliveryDto
) {
    val (label, bg, fg) = when {
        delivery.isTandaDelivery -> Triple("Tanda", palette.accentSoft, palette.accent)
        delivery.status == "InTransit" -> Triple("En ruta", palette.warningSoft, palette.warning)
        delivery.balanceDue > 0.01 && delivery.amountPaid < 0.01 ->
            Triple(formatRouteMoney(delivery.balanceDue), palette.moneySoft, palette.money)
        delivery.amountPaid >= delivery.balanceDue && delivery.balanceDue > 0.01 ->
            Triple("Pagado", palette.successSoft, palette.success)
        else -> Triple("Pendiente", palette.surfaceMuted, palette.textSecondary)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ReorderToast(
    palette: RegiBazar2026Palette,
    message: String,
    onUndo: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceMuted,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (onUndo != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    "DESHACER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onUndo() })
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ReorderQuickActions(
    palette: RegiBazar2026Palette,
    onDismiss: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveToStart: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        shadowElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            QuickActionButton(
                label = "Inicio",
                icon = Icons.Rounded.VerticalAlignTop,
                enabled = canMoveUp,
                onClick = onMoveToStart,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Subir",
                icon = Icons.Rounded.KeyboardArrowUp,
                enabled = canMoveUp,
                onClick = onMoveUp,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Bajar",
                icon = Icons.Rounded.KeyboardArrowDown,
                enabled = canMoveDown,
                onClick = onMoveDown,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Final",
                icon = Icons.Rounded.VerticalAlignBottom,
                enabled = canMoveDown,
                onClick = onMoveToEnd,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = regiBazar2026Palette()
    val color = if (enabled) palette.accent else palette.textSecondary.copy(alpha = 0.4f)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) palette.accentSoft else Color.Transparent,
        modifier = modifier.height(54.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun OptimizePreviewModal(
    palette: RegiBazar2026Palette,
    currentStats: RouteStats,
    currentOrder: List<RouteDeliveryDto>,
    onApply: (List<RouteDeliveryDto>) -> Unit,
    onDismiss: () -> Unit
) {
    val currentOrderKey = currentOrder.joinToString("|") { it.deliveryId.toString() }
    val optimizedOrder = remember(currentOrderKey) { optimizeNearestNeighbor(currentOrder) }
    val optimizedStats = remember(optimizedOrder) { computeRouteStats(optimizedOrder) }
    val savedKm = currentStats.distanceKm - optimizedStats.distanceKm
    val savedMin = currentStats.estimatedMinutes - optimizedStats.estimatedMinutes
    val isImprovement = savedKm > 0.05

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background.copy(alpha = 0.85f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(palette.accent, palette.accentPressed)
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (isImprovement) "Ruta optimizada" else "Ruta ya está cerca del óptimo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isImprovement)
                        "Reordenamos con la distancia más corta entre paradas."
                    else
                        "Los cambios posibles no generan ahorro significativo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )

                if (isImprovement) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.successSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "−${formatKm(savedKm)} · −$savedMin min",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.success
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "AHORRO ESTIMADO",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OptimizeDiffColumn(
                        label = "Actual",
                        distance = formatKm(currentStats.distanceKm),
                        time = "${currentStats.estimatedMinutes} min",
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        isCurrent = true
                    )
                    OptimizeDiffColumn(
                        label = "Optimizado",
                        distance = formatKm(optimizedStats.distanceKm),
                        time = "${optimizedStats.estimatedMinutes} min",
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        isCurrent = false
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.divider),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = palette.textSecondary
                        )
                    ) {
                        Text("Conservar actual", fontWeight = FontWeight.Bold)
                    }
                    androidx.compose.material3.Button(
                        onClick = { onApply(optimizedOrder) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = Color(0xFFF8FAFF)
                        )
                    ) {
                        Text("Aplicar ruta", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OptimizeDiffColumn(
    label: String,
    distance: String,
    time: String,
    palette: RegiBazar2026Palette,
    modifier: Modifier = Modifier,
    isCurrent: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.background,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.8.sp),
                fontWeight = FontWeight.Bold,
                color = palette.textSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                distance,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCurrent) palette.textSecondary else palette.success
            )
            Text(
                time,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) palette.textSecondary.copy(alpha = 0.7f) else palette.success.copy(alpha = 0.8f)
            )
        }
    }
}

private fun projectDeliveries(deliveries: List<RouteDeliveryDto>): List<Offset> {
    val located = deliveries.filter { it.latitude != null && it.longitude != null }
    if (located.isEmpty()) return emptyList()
    val lats = located.map { it.latitude!! }
    val lngs = located.map { it.longitude!! }
    val minLat = lats.min()
    val maxLat = lats.max()
    val minLng = lngs.min()
    val maxLng = lngs.max()
    val rangeLat = max(0.0001, maxLat - minLat)
    val rangeLng = max(0.0001, maxLng - minLng)
    val targetW = 360f
    val targetH = 520f
    val scaleX = targetW / rangeLng.toFloat()
    val scaleY = targetH / rangeLat.toFloat()
    val scale = min(scaleX, scaleY) * 0.85f
    val offsetX = (targetW - rangeLng.toFloat() * scale) / 2f
    val offsetY = (targetH - rangeLat.toFloat() * scale) / 2f

    return deliveries.map { d ->
        if (d.latitude == null || d.longitude == null) {
            Offset(0f, 0f)
        } else {
            val x = ((d.longitude - minLng) * scale + offsetX).toFloat() + 8f
            val y = (targetH - ((d.latitude - minLat) * scale + offsetY)).toFloat() + 80f
            Offset(x, y)
        }
    }
}

private fun computeRouteStats(deliveries: List<RouteDeliveryDto>): RouteStats {
    val located = deliveries.filter { it.latitude != null && it.longitude != null }
    if (located.size < 2) return RouteStats(0.0, located.size * 5)
    var totalMeters = 0.0
    for (i in 0 until located.size - 1) {
        val a = located[i]
        val b = located[i + 1]
        val results = FloatArray(1)
        Location.distanceBetween(
            a.latitude!!, a.longitude!!,
            b.latitude!!, b.longitude!!,
            results
        )
        totalMeters += results[0]
    }
    val distanceKm = totalMeters / 1000.0
    val drivingMinutes = (distanceKm / 25.0 * 60).toInt()
    val stopMinutes = located.size * 5
    return RouteStats(distanceKm, drivingMinutes + stopMinutes)
}

private fun optimizeNearestNeighbor(deliveries: List<RouteDeliveryDto>): List<RouteDeliveryDto> {
    val located = deliveries.filter { it.latitude != null && it.longitude != null }
    val unlocated = deliveries.filter { it.latitude == null || it.longitude == null }
    if (located.size < 2) return deliveries

    val remaining = located.toMutableList()
    val result = mutableListOf<RouteDeliveryDto>()
    result.add(remaining.removeAt(0))
    val results = FloatArray(1)

    while (remaining.isNotEmpty()) {
        val current = result.last()
        var bestIndex = 0
        var bestDistance = Float.MAX_VALUE
        remaining.forEachIndexed { index, candidate ->
            Location.distanceBetween(
                current.latitude!!, current.longitude!!,
                candidate.latitude!!, candidate.longitude!!,
                results
            )
            if (results[0] < bestDistance) {
                bestDistance = results[0]
                bestIndex = index
            }
        }
        result.add(remaining.removeAt(bestIndex))
    }
    return result + unlocated
}

private fun formatKm(km: Double): String =
    if (km < 1.0) "${(km * 1000).toInt()} m" else String.format("%.1f km", km)

private fun shortName(name: String): String {
    val parts = name.trim().split(Regex("\\s+"))
    return if (parts.size >= 2) "${parts[0]} ${parts[1].first()}." else name
}
