package com.example.regibazarapp.ui.screens.routebuilder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PersonPinCircle
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.regibazarapp.data.models.AvailableTandaDto
import com.example.regibazarapp.data.models.OrderSummaryDto
import com.example.regibazarapp.data.models.PreviewRouteResponse
import com.example.regibazarapp.data.models.PreviewStopDto
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.theme.RegiBazar2026Tokens
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import com.example.regibazarapp.ui.utils.DirectionsManager
import com.example.regibazarapp.ui.utils.MapUtils
import com.example.regibazarapp.ui.utils.filterAvailableTandas
import com.example.regibazarapp.ui.utils.filterRouteOrders
import com.example.regibazarapp.ui.utils.formatRouteDistance
import com.example.regibazarapp.ui.utils.formatRouteDuration
import com.example.regibazarapp.ui.utils.formatRouteMoney
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RouteBuilderScreen(
    routeId: Int?,
    onBack: () -> Unit,
    onRouteSaved: (Int) -> Unit,
    viewModel: RouteBuilderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = regiBazar2026Palette()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val selectionSummary = remember(uiState) { routeBuilderSelectionSummary(uiState) }
    var editingAddressFor by remember { mutableStateOf<OrderSummaryDto?>(null) }
    var editingTandaFor by remember { mutableStateOf<AvailableTandaDto?>(null) }
    var showCamiDialog by remember { mutableStateOf(false) }

    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime qué pedidos quieres en la ruta")
        }
    }
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val command = result.data
            ?.getStringArrayListExtra(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
        if (!command.isNullOrBlank()) {
            showCamiDialog = false
            viewModel.applyCamiSelection(command)
        }
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) speechLauncher.launch(speechIntent)
    }

    LaunchedEffect(routeId) {
        viewModel.initialize(routeId)
    }

    LaunchedEffect(uiState.error, uiState.message) {
        val feedback = uiState.error ?: uiState.message
        if (!feedback.isNullOrBlank()) {
            snackbarHostState.showSnackbar(feedback)
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(uiState.savedRouteId) {
        val savedRouteId = uiState.savedRouteId ?: return@LaunchedEffect
        viewModel.consumeSavedRoute()
        onRouteSaved(savedRouteId)
    }

    editingAddressFor?.let { order ->
        AddressCaptureDialog(
            dialogKey = "order-${order.id}",
            clientName = order.clientName,
            currentAddress = order.effectiveAddress,
            currentInstructions = order.deliveryInstructions,
            isSaving = uiState.isGeocoding,
            palette = palette,
            onDismiss = { editingAddressFor = null },
            onSave = { address, instructions ->
                viewModel.saveAddress(order.id, address, instructions)
                editingAddressFor = null
            }
        )
    }

    editingTandaFor?.let { tanda ->
        AddressCaptureDialog(
            dialogKey = "tanda-${tanda.tandaParticipantId}",
            clientName = tanda.clientName,
            currentAddress = tanda.clientAddress,
            currentInstructions = tanda.deliveryInstructions,
            isSaving = uiState.isGeocoding,
            palette = palette,
            onDismiss = { editingTandaFor = null },
            onSave = { address, instructions ->
                viewModel.saveTandaAddress(tanda.tandaParticipantId, address, instructions)
                editingTandaFor = null
            }
        )
    }

    if (showCamiDialog) {
        CamiSelectionDialog(
            initialCommand = uiState.aiCommand,
            isProcessing = uiState.isApplyingAi,
            palette = palette,
            onDismiss = { showCamiDialog = false },
            onListen = {
                if (
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    speechLauncher.launch(speechIntent)
                } else {
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onApply = { command ->
                showCamiDialog = false
                viewModel.applyCamiSelection(command)
            }
        )
    }

    var showStartFundDialog by remember { mutableStateOf(false) }

    if (showStartFundDialog) {
        com.example.regibazarapp.ui.components.StartRouteFundDialog(
            initialExchangeRate = 18.0,
            onDismiss = {
                showStartFundDialog = false
                viewModel.saveRoute(null)
            },
            onConfirmStart = { req ->
                showStartFundDialog = false
                viewModel.saveRoute(req)
            }
        )
    }

    if (uiState.packageRequirements.isNotEmpty()) {
        PackageRequirementsDialog(
            requirements = uiState.packageRequirements,
            isSaving = uiState.isSavingPackages,
            palette = palette,
            onDismiss = viewModel::dismissPackageRequirements,
            onConfirm = { counts ->
                viewModel.confirmPackageRequirements(counts)
                if (routeId == null) {
                    showStartFundDialog = true
                }
            }
        )
    }

    Scaffold(
        topBar = {
            RouteBuilderHeader(
                routeId = routeId,
                palette = palette,
                selectedCount = selectionSummary.selectedStops,
                isPreviewing = uiState.preview != null,
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!uiState.isLoading && selectionSummary.selectedStops > 0) {
                BuilderBottomBar(
                    routeId = routeId,
                    summary = selectionSummary,
                    previewReady = uiState.preview != null,
                    isPreviewing = uiState.isPreviewing,
                    isSaving = uiState.isSaving || uiState.isSavingPackages,
                    palette = palette,
                    onPreview = viewModel::previewRoute,
                    onSave = {
                        if (routeId == null) {
                            showStartFundDialog = true
                        } else {
                            viewModel.saveRoute()
                        }
                    }
                )
            }
        },
        containerColor = palette.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(palette.background)
        ) {
            when {
                uiState.isLoading -> RouteBuilderLoadingState(palette)
                uiState.preview != null -> {
                    RoutePreviewContent(
                        preview = requireNotNull(uiState.preview),
                        routeId = routeId,
                        palette = palette,
                        onClose = viewModel::closePreview
                    )
                }
                else -> {
                    val filteredOrders = remember(uiState.orders, uiState.searchQuery) {
                        filterRouteOrders(uiState.orders, uiState.searchQuery)
                    }
                    val filteredTandas = remember(uiState.tandas, uiState.searchQuery) {
                        filterAvailableTandas(uiState.tandas, uiState.searchQuery)
                    }

                    RouteBuilderSelectionContent(
                        uiState = uiState,
                        selectionSummary = selectionSummary,
                        filteredOrders = filteredOrders,
                        filteredTandas = filteredTandas,
                        palette = palette,
                        onSearch = viewModel::updateSearch,
                        onShowTandas = viewModel::showTandas,
                        onSelectAll = viewModel::selectAllOrdersWithCoordinates,
                        onClear = viewModel::clearSelection,
                        onGeocode = viewModel::bulkGeocodeSelected,
                        onOpenCami = { showCamiDialog = true },
                        onToggleOrder = viewModel::toggleOrder,
                        onToggleTanda = viewModel::toggleTanda,
                        onEditOrderAddress = { editingAddressFor = it },
                        onEditTandaAddress = { editingTandaFor = it }
                    )
                }
            }

            if (uiState.isPreviewing || uiState.isSaving || uiState.isSavingPackages || uiState.isGeocoding || uiState.isApplyingAi) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = palette.accent,
                    trackColor = palette.accentSoft
                )
            }
        }
    }
}

@Composable
private fun RouteBuilderHeader(
    routeId: Int?,
    palette: RegiBazar2026Palette,
    selectedCount: Int,
    isPreviewing: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Regresar a administración de rutas",
                tint = palette.textPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (routeId == null) "Nueva ruta" else "Editar ruta #$routeId",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isPreviewing) {
                    "Revisa el orden antes de guardar"
                } else {
                    "$selectedCount paradas seleccionadas"
                },
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = if (isPreviewing) palette.successSoft else palette.accentSoft,
            contentColor = if (isPreviewing) palette.success else palette.accent,
            modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPreviewing) Icons.Rounded.DoneAll else Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteBuilderSelectionContent(
    uiState: RouteBuilderUiState,
    selectionSummary: RouteBuilderSelectionSummary,
    filteredOrders: List<OrderSummaryDto>,
    filteredTandas: List<AvailableTandaDto>,
    palette: RegiBazar2026Palette,
    onSearch: (String) -> Unit,
    onShowTandas: (Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onGeocode: () -> Unit,
    onOpenCami: () -> Unit,
    onToggleOrder: (Int) -> Unit,
    onToggleTanda: (String) -> Unit,
    onEditOrderAddress: (OrderSummaryDto) -> Unit,
    onEditTandaAddress: (AvailableTandaDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = RegiBazar2026Tokens.ScreenPadding,
            end = RegiBazar2026Tokens.ScreenPadding,
            top = 4.dp,
            bottom = 118.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "builder-summary") {
            BuilderSummaryPanel(
                summary = selectionSummary,
                totalOrders = uiState.orders.size,
                totalTandas = uiState.tandas.size,
                isApplyingAi = uiState.isApplyingAi,
                palette = palette,
                onOpenCami = onOpenCami
            )
        }

        item(key = "builder-mode") {
            BuilderModeAndSearch(
                uiState = uiState,
                palette = palette,
                onSearch = onSearch,
                onShowTandas = onShowTandas,
                onSelectAll = onSelectAll,
                onClear = onClear
            )
        }

        if (selectionSummary.missingCoordinates > 0) {
            item(key = "missing-coordinates") {
                MissingCoordinatesNotice(
                    count = selectionSummary.missingCoordinates,
                    isGeocoding = uiState.isGeocoding,
                    palette = palette,
                    onGeocode = onGeocode
                )
            }
        }

        item(key = "candidate-label") {
            SectionLabel(
                title = if (uiState.showTandas) "TANDAS DISPONIBLES" else "PEDIDOS DISPONIBLES",
                count = if (uiState.showTandas) filteredTandas.size else filteredOrders.size,
                palette = palette
            )
        }

        if (uiState.showTandas) {
            if (filteredTandas.isEmpty()) {
                item(key = "empty-tandas") {
                    EmptyCandidates(
                        title = "No hay tandas con ese filtro",
                        message = "Cambia la búsqueda o vuelve a pedidos.",
                        palette = palette
                    )
                }
            } else {
                items(filteredTandas, key = { it.tandaParticipantId }) { tanda ->
                    TandaCandidateRow(
                        tanda = tanda,
                        selected = tanda.tandaParticipantId in uiState.selectedTandaIds,
                        palette = palette,
                        onToggle = { onToggleTanda(tanda.tandaParticipantId) },
                        onEditAddress = { onEditTandaAddress(tanda) }
                    )
                }
            }
        } else {
            if (filteredOrders.isEmpty()) {
                item(key = "empty-orders") {
                    EmptyCandidates(
                        title = "No hay pedidos con ese filtro",
                        message = "Busca por clienta, teléfono, pedido o producto.",
                        palette = palette
                    )
                }
            } else {
                items(filteredOrders, key = { it.id }) { order ->
                    OrderCandidateRow(
                        order = order,
                        selected = order.id in uiState.selectedOrderIds,
                        palette = palette,
                        onToggle = { onToggleOrder(order.id) },
                        onEditAddress = { onEditOrderAddress(order) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BuilderSummaryPanel(
    summary: RouteBuilderSelectionSummary,
    totalOrders: Int,
    totalTandas: Int,
    isApplyingAi: Boolean,
    palette: RegiBazar2026Palette,
    onOpenCami: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Brush.linearGradient(listOf(palette.accent, Color(0xFF3E7BFF))),
                            RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Route,
                        contentDescription = null,
                        tint = Color(0xFFF8FAFF),
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Arma la jornada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        "$totalOrders pedidos · $totalTandas tandas disponibles",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                Surface(
                    onClick = onOpenCami,
                    enabled = !isApplyingAi,
                    shape = RoundedCornerShape(13.dp),
                    color = palette.accentSoft,
                    contentColor = palette.accent,
                    modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isApplyingAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = palette.accent
                            )
                        } else {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = "Armar selección con C.A.M.I.",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 15.dp),
                color = palette.divider
            )

            Row(Modifier.fillMaxWidth()) {
                SummaryValue(
                    label = "Paradas",
                    value = summary.selectedStops.toString(),
                    color = palette.textPrimary,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = "Cobro",
                    value = formatRouteMoney(summary.expectedCollection),
                    color = palette.money,
                    palette = palette,
                    modifier = Modifier.weight(1.3f)
                )
                SummaryValue(
                    label = "Sin ubicación",
                    value = summary.missingCoordinates.toString(),
                    color = if (summary.missingCoordinates > 0) palette.warning else palette.success,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryValue(
    label: String,
    value: String,
    color: Color,
    palette: RegiBazar2026Palette,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 21.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BuilderModeAndSearch(
    uiState: RouteBuilderUiState,
    palette: RegiBazar2026Palette,
    onSearch: (String) -> Unit,
    onShowTandas: (Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BuilderSegmentedControl(
            showTandas = uiState.showTandas,
            palette = palette,
            onShowTandas = onShowTandas
        )

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.textSecondary)
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearch("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            placeholder = {
                Text(
                    if (uiState.showTandas) {
                        "Clienta, tanda o producto"
                    } else {
                        "Clienta, teléfono, pedido o producto"
                    }
                )
            },
            shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.surface,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.divider,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
                focusedPlaceholderColor = palette.textSecondary,
                unfocusedPlaceholderColor = palette.textSecondary
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!uiState.showTandas) {
                TextButton(
                    onClick = onSelectAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)
                ) {
                    Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Todas con ubicación", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.weight(1f))
            if (uiState.selectedCount > 0) {
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.textSecondary)
                ) {
                    Text("Limpiar selección")
                }
            }
        }
    }
}

@Composable
private fun BuilderSegmentedControl(
    showTandas: Boolean,
    palette: RegiBazar2026Palette,
    onShowTandas: (Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(palette.surfaceMuted, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        val indicatorWidth = maxWidth / 2
        val indicatorOffset by animateDpAsState(
            targetValue = if (showTandas) indicatorWidth else 0.dp,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            label = "route-builder-mode-indicator"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .fillMaxHeight()
                .background(palette.surface, RoundedCornerShape(11.dp))
        )
        Row(Modifier.fillMaxSize()) {
            BuilderModeButton(
                label = "Pedidos",
                icon = Icons.Rounded.Inventory2,
                selected = !showTandas,
                palette = palette,
                modifier = Modifier.weight(1f),
                onClick = { onShowTandas(false) }
            )
            BuilderModeButton(
                label = "Tandas",
                icon = Icons.Rounded.PersonPinCircle,
                selected = showTandas,
                palette = palette,
                modifier = Modifier.weight(1f),
                onClick = { onShowTandas(true) }
            )
        }
    }
}

@Composable
private fun BuilderModeButton(
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
private fun MissingCoordinatesNotice(
    count: Int,
    isGeocoding: Boolean,
    palette: RegiBazar2026Palette,
    onGeocode: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.warningSoft,
        contentColor = palette.warning
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.LocationOff, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$count paradas sin ubicación",
                    color = palette.textPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Puedes resolverlas ahora o dejarlas al final del preview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
            TextButton(
                onClick = onGeocode,
                enabled = !isGeocoding,
                colors = ButtonDefaults.textButtonColors(contentColor = palette.warning)
            ) {
                Text(if (isGeocoding) "Ubicando" else "Resolver", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionLabel(
    title: String,
    count: Int,
    palette: RegiBazar2026Palette
) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = palette.textSecondary
        )
        Spacer(Modifier.weight(1f))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.accent
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderCandidateRow(
    order: OrderSummaryDto,
    selected: Boolean,
    palette: RegiBazar2026Palette,
    onToggle: () -> Unit,
    onEditAddress: () -> Unit
) {
    val payment = remember(order) { orderCandidatePaymentInfo(order) }
    val paymentColor = paymentColor(payment.tone, palette)

    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(RegiBazar2026Tokens.CardRadius),
        color = if (selected) palette.accentSoft else palette.surface,
        border = BorderStroke(1.dp, if (selected) palette.accent else palette.divider),
        shadowElevation = if (selected) 2.dp else 3.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            SelectionDot(selected = selected, palette = palette)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#${order.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accent,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        order.clientName,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    order.effectiveAddress ?: "Sin dirección capturada",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (order.effectiveAddress == null) palette.warning else palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(9.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    BuilderChip(
                        label = payment.label,
                        icon = if (payment.tone == RouteBuilderPaymentTone.TRANSFER) {
                            Icons.Rounded.SyncAlt
                        } else {
                            Icons.Rounded.Inventory2
                        },
                        foreground = paymentColor,
                        background = paymentColor.copy(alpha = 0.12f)
                    )
                    BuilderChip(
                        label = orderProductSummary(order),
                        icon = Icons.Rounded.Inventory2,
                        foreground = palette.textSecondary,
                        background = palette.surfaceMuted
                    )
                    BuilderChip(
                        label = if (order.hasCoordinates) "Ubicada" else "Sin coordenadas",
                        icon = if (order.hasCoordinates) Icons.Rounded.MyLocation else Icons.Rounded.LocationOff,
                        foreground = if (order.hasCoordinates) palette.success else palette.warning,
                        background = if (order.hasCoordinates) palette.successSoft else palette.warningSoft
                    )
                    BuilderChip(
                        label = if (order.packagesConfirmed) {
                            if (order.totalPackages == 0) "Sin bolsas" else "${order.totalPackages ?: 0} bolsa(s)"
                        } else {
                            "Bolsas pendientes"
                        },
                        icon = if (order.packagesConfirmed) Icons.Rounded.Inventory2 else Icons.Rounded.ErrorOutline,
                        foreground = if (order.packagesConfirmed) palette.success else palette.warning,
                        background = if (order.packagesConfirmed) palette.successSoft else palette.warningSoft
                    )
                }
            }
            IconButton(
                onClick = onEditAddress,
                modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
            ) {
                Icon(
                    Icons.Rounded.EditLocationAlt,
                    contentDescription = "Editar dirección",
                    tint = if (order.hasCoordinates) palette.textSecondary else palette.warning
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TandaCandidateRow(
    tanda: AvailableTandaDto,
    selected: Boolean,
    palette: RegiBazar2026Palette,
    onToggle: () -> Unit,
    onEditAddress: () -> Unit
) {
    val payment = remember(tanda) { tandaCandidatePaymentInfo(tanda) }

    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(RegiBazar2026Tokens.CardRadius),
        color = if (selected) palette.accentSoft else palette.surface,
        border = BorderStroke(1.dp, if (selected) palette.accent else palette.divider),
        shadowElevation = if (selected) 2.dp else 3.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            SelectionDot(selected = selected, palette = palette)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tanda.clientName,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    tanda.clientAddress ?: "Sin dirección capturada",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tanda.clientAddress == null) palette.warning else palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(9.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    BuilderChip(
                        label = payment.label,
                        icon = Icons.Rounded.PersonPinCircle,
                        foreground = palette.accent,
                        background = palette.accentSoft
                    )
                    BuilderChip(
                        label = tandaProductSummary(tanda),
                        icon = Icons.Rounded.Inventory2,
                        foreground = palette.textSecondary,
                        background = palette.surfaceMuted
                    )
                    BuilderChip(
                        label = if (tanda.hasCoordinates) "Ubicada" else "Sin coordenadas",
                        icon = if (tanda.hasCoordinates) Icons.Rounded.MyLocation else Icons.Rounded.LocationOff,
                        foreground = if (tanda.hasCoordinates) palette.success else palette.warning,
                        background = if (tanda.hasCoordinates) palette.successSoft else palette.warningSoft
                    )
                }
            }
            IconButton(
                onClick = onEditAddress,
                modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
            ) {
                Icon(
                    Icons.Rounded.EditLocationAlt,
                    contentDescription = "Editar dirección",
                    tint = if (tanda.hasCoordinates) palette.textSecondary else palette.warning
                )
            }
        }
    }
}

@Composable
private fun SelectionDot(selected: Boolean, palette: RegiBazar2026Palette) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(if (selected) palette.accent else palette.surface, CircleShape)
            .border(
                width = 1.5.dp,
                color = if (selected) palette.accent else palette.divider,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color(0xFFF8FAFF),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BuilderChip(
    label: String,
    icon: ImageVector,
    foreground: Color,
    background: Color
) {
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(RegiBazar2026Tokens.TagRadius))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RoutePreviewContent(
    preview: PreviewRouteResponse,
    routeId: Int?,
    palette: RegiBazar2026Palette,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = RegiBazar2026Tokens.ScreenPadding,
            end = RegiBazar2026Tokens.ScreenPadding,
            top = 4.dp,
            bottom = 118.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "preview-summary") {
            PreviewSummaryCard(
                preview = preview,
                routeId = routeId,
                palette = palette,
                onClose = onClose
            )
        }
        item(key = "preview-map") {
            RoutePreviewMap(preview = preview, palette = palette)
        }
        if (preview.skipped.isNotEmpty()) {
            item(key = "preview-skipped") {
                PreviewSkippedNotice(preview = preview, palette = palette)
            }
        }
        item(key = "preview-label") {
            SectionLabel("ORDEN PROPUESTO", preview.stops.size, palette)
        }
        items(preview.stops, key = { "${it.kind}-${it.orderId}-${it.tandaParticipantId}-${it.sortOrder}" }) { stop ->
            PreviewStopRow(stop = stop, palette = palette)
        }
    }
}

@Composable
private fun PreviewSummaryCard(
    preview: PreviewRouteResponse,
    routeId: Int?,
    palette: RegiBazar2026Palette,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = palette.successSoft,
                    contentColor = palette.success,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(23.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (routeId == null) "Preview listo" else "Cambios listos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        "El orden se optimizó antes de guardar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar preview", tint = palette.textSecondary)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 15.dp),
                color = palette.divider
            )

            Row(Modifier.fillMaxWidth()) {
                SummaryValue(
                    label = "Distancia",
                    value = formatRouteDistance(preview.totalDistanceMeters),
                    color = palette.textPrimary,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = "Tiempo",
                    value = formatRouteDuration(preview.totalDurationSeconds),
                    color = palette.accent,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = "Cobro",
                    value = formatRouteMoney(previewExpectedCollection(preview)),
                    color = palette.money,
                    palette = palette,
                    modifier = Modifier.weight(1.2f)
                )
            }

            if (preview.stopsWithoutCoords > 0) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.warningSoft, RoundedCornerShape(13.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.LocationOff, contentDescription = null, tint = palette.warning)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "${preview.stopsWithoutCoords} paradas quedaron al final por falta de coordenadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewSkippedNotice(
    preview: PreviewRouteResponse,
    palette: RegiBazar2026Palette
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.errorSoft,
        contentColor = palette.error
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(9.dp))
                Text(
                    "${preview.skipped.size} paradas no entraron",
                    color = palette.textPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                preview.skipped.take(2).joinToString(" · ") { "${it.name}: ${it.reason}" },
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RoutePreviewMap(
    preview: PreviewRouteResponse,
    palette: RegiBazar2026Palette
) {
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }
    val stopPoints = remember(preview.stops) {
        preview.stops.mapNotNull { stop ->
            val latitude = stop.latitude
            val longitude = stop.longitude
            if (latitude != null && longitude != null) LatLng(latitude, longitude) else null
        }
    }
    val depotPoint = remember(preview.depotLatitude, preview.depotLongitude) {
        val latitude = preview.depotLatitude
        val longitude = preview.depotLongitude
        if (latitude != null && longitude != null) LatLng(latitude, longitude) else null
    }
    val routePoints = remember(preview.polylineEncoded, stopPoints) {
        preview.polylineEncoded
            ?.takeIf { it.isNotBlank() }
            ?.let(DirectionsManager::decodePolyline)
            ?.takeIf { it.isNotEmpty() }
            ?: stopPoints
    }
    val allPoints = remember(stopPoints, depotPoint) {
        buildList {
            depotPoint?.let(::add)
            addAll(stopPoints)
        }
    }

    LaunchedEffect(mapLoaded, allPoints) {
        if (!mapLoaded || allPoints.isEmpty()) return@LaunchedEffect
        if (allPoints.size == 1) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(allPoints.first(), 15f)
            )
        } else {
            val bounds = LatLngBounds.builder().apply {
                allPoints.forEach(::include)
            }.build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 72)
            )
        }
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(252.dp)
            .clip(RoundedCornerShape(22.dp)),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false
        ),
        onMapLoaded = { mapLoaded = true }
    ) {
        depotPoint?.let { depot ->
            Marker(
                state = MarkerState(depot),
                title = "Punto de salida",
                snippet = "Inicio de la ruta",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
            )
        }

        preview.stops.forEach { stop ->
            val latitude = stop.latitude
            val longitude = stop.longitude
            if (latitude != null && longitude != null) {
                Marker(
                    state = MarkerState(LatLng(latitude, longitude)),
                    title = "${stop.sortOrder}. ${stop.clientName}",
                    snippet = stop.clientAddress.orEmpty(),
                    icon = BitmapDescriptorFactory.fromBitmap(
                        MapUtils.createNumberedMarkerBitmap(
                            context = context,
                            number = stop.sortOrder
                        )
                    )
                )
            }
        }

        if (routePoints.size > 1) {
            Polyline(
                points = routePoints,
                color = palette.accent,
                width = 11f
            )
        }
    }
}

@Composable
private fun PreviewStopRow(
    stop: PreviewStopDto,
    palette: RegiBazar2026Palette
) {
    val isTanda = stop.kind.equals("Tanda", ignoreCase = true)
    val mainColor = when {
        !stop.hasCoords -> palette.warning
        isTanda -> palette.accent
        else -> palette.success
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(mainColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${stop.sortOrder}",
                    color = Color(0xFFF8FAFF),
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stop.clientName,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stop.clientAddress ?: "Sin dirección",
                    color = if (stop.hasCoords) palette.textSecondary else palette.warning,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    previewStopPaymentLabel(stop),
                    color = if (isTanda) palette.accent else palette.money,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun BuilderBottomBar(
    routeId: Int?,
    summary: RouteBuilderSelectionSummary,
    previewReady: Boolean,
    isPreviewing: Boolean,
    isSaving: Boolean,
    palette: RegiBazar2026Palette,
    onPreview: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = palette.surface,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${summary.selectedStops} paradas listas",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Cobro estimado ${formatRouteMoney(summary.expectedCollection)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                if (previewReady) {
                    Surface(
                        shape = CircleShape,
                        color = palette.successSoft,
                        contentColor = palette.success
                    ) {
                        Text(
                            "Preview",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onPreview,
                    enabled = !isPreviewing && !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = palette.accent
                    ),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.34f))
                ) {
                    if (isPreviewing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = palette.accent
                        )
                    } else {
                        Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Preview", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1.25f)
                        .height(50.dp),
                    shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = Color(0xFFF8FAFF),
                        disabledContainerColor = palette.accentSoft,
                        disabledContentColor = palette.textSecondary
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFFF8FAFF),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.Route, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (routeId == null) "Crear ruta" else "Guardar cambios",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackageRequirementsDialog(
    requirements: List<com.example.regibazarapp.data.models.RoutePackageRequirementDto>,
    isSaving: Boolean,
    palette: RegiBazar2026Palette,
    onDismiss: () -> Unit,
    onConfirm: (Map<Int, Int>) -> Unit
) {
    var counts by remember(requirements) {
        mutableStateOf(
            requirements.mapNotNull { requirement ->
                requirement.totalPackages?.let { requirement.id to it }
            }.toMap()
        )
    }
    val allResolved = requirements.all { it.id in counts }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon = { Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = palette.warning) },
        title = { Text("Faltan bolsas por confirmar", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Estos pedidos no pueden salir hasta indicar cuántas bolsas llevan. Elige Sin bolsas si aplica.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                requirements.forEach { requirement ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceMuted
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "#${requirement.id} · ${requirement.clientName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                listOf(0, 1, 2, 3, 4, 5).forEach { count ->
                                    val selected = counts[requirement.id] == count
                                    OutlinedButton(
                                        onClick = { counts = counts + (requirement.id to count) },
                                        enabled = !isSaving,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (selected) palette.accentSoft else palette.surface,
                                            contentColor = if (selected) palette.accent else palette.textSecondary
                                        ),
                                        border = BorderStroke(1.dp, if (selected) palette.accent else palette.divider),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (count == 0) "Sin bolsas" else count.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        val current = counts[requirement.id] ?: 5
                                        counts = counts + (requirement.id to maxOf(6, current + 1))
                                    },
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if ((counts[requirement.id] ?: 0) > 5) palette.accentSoft else palette.surface,
                                        contentColor = if ((counts[requirement.id] ?: 0) > 5) palette.accent else palette.textSecondary
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if ((counts[requirement.id] ?: 0) > 5) palette.accent else palette.divider
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if ((counts[requirement.id] ?: 0) > 5) (counts[requirement.id] ?: 0).toString() else "+",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(counts) },
                enabled = allResolved && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color(0xFFF8FAFF)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFFF8FAFF),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirmar y crear ruta")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = palette.textSecondary)
            ) {
                Text("Revisar selección")
            }
        }
    )
}

@Composable
private fun AddressCaptureDialog(
    dialogKey: String,
    clientName: String,
    currentAddress: String?,
    currentInstructions: String?,
    isSaving: Boolean,
    palette: RegiBazar2026Palette,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var address by remember(dialogKey) { mutableStateOf(currentAddress.orEmpty()) }
    var instructions by remember(dialogKey) { mutableStateOf(currentInstructions.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = palette.accent) },
        title = { Text("Ubicar a $clientName", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "La dirección queda guardada en la clienta para futuras rutas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dirección completa") },
                    placeholder = { Text("Calle, número y colonia") },
                    minLines = 2,
                    colors = dialogTextFieldColors(palette)
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Indicaciones de entrega") },
                    placeholder = { Text("Color de casa, entre calles") },
                    minLines = 2,
                    colors = dialogTextFieldColors(palette)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(address, instructions) },
                enabled = address.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color(0xFFF8FAFF)
                )
            ) {
                Text(if (isSaving) "Ubicando" else "Guardar y ubicar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = palette.textSecondary)
            ) {
                Text("Cancelar")
            }
        },
        containerColor = palette.surface
    )
}

@Composable
private fun CamiSelectionDialog(
    initialCommand: String,
    isProcessing: Boolean,
    palette: RegiBazar2026Palette,
    onDismiss: () -> Unit,
    onListen: () -> Unit,
    onApply: (String) -> Unit
) {
    var command by remember(initialCommand) { mutableStateOf(initialCommand) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = palette.accent) },
        title = { Text("Armar ruta con C.A.M.I.", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Describe la selección como la dirías normalmente. Ejemplo: “las de Villas y las que deben más de 500”.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Qué pedidos quieres") },
                    minLines = 3,
                    enabled = !isProcessing,
                    colors = dialogTextFieldColors(palette)
                )
                OutlinedButton(
                    onClick = onListen,
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.34f))
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Dictar instrucción", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(command) },
                enabled = command.isNotBlank() && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color(0xFFF8FAFF)
                )
            ) {
                Text(if (isProcessing) "Analizando" else "Aplicar selección")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing,
                colors = ButtonDefaults.textButtonColors(contentColor = palette.textSecondary)
            ) {
                Text("Cancelar")
            }
        },
        containerColor = palette.surface
    )
}

@Composable
private fun dialogTextFieldColors(palette: RegiBazar2026Palette) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = palette.surface,
    unfocusedContainerColor = palette.surface,
    focusedBorderColor = palette.accent,
    unfocusedBorderColor = palette.divider,
    focusedTextColor = palette.textPrimary,
    unfocusedTextColor = palette.textPrimary,
    focusedLabelColor = palette.accent,
    unfocusedLabelColor = palette.textSecondary,
    focusedPlaceholderColor = palette.textSecondary,
    unfocusedPlaceholderColor = palette.textSecondary
)

@Composable
private fun RouteBuilderLoadingState(palette: RegiBazar2026Palette) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LoadingBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp),
                radius = 22.dp,
                palette = palette
            )
        }
        item {
            LoadingBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
                radius = 16.dp,
                palette = palette
            )
        }
        items(6) {
            LoadingBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                radius = 18.dp,
                palette = palette
            )
        }
    }
}

@Composable
private fun LoadingBlock(
    modifier: Modifier,
    radius: androidx.compose.ui.unit.Dp,
    palette: RegiBazar2026Palette
) {
    Box(modifier.background(palette.surfaceMuted, RoundedCornerShape(radius)))
}

@Composable
private fun EmptyCandidates(
    title: String,
    message: String,
    palette: RegiBazar2026Palette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(42.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun paymentColor(
    tone: RouteBuilderPaymentTone,
    palette: RegiBazar2026Palette
): Color = when (tone) {
    RouteBuilderPaymentTone.MONEY -> palette.money
    RouteBuilderPaymentTone.TRANSFER -> palette.accent
    RouteBuilderPaymentTone.SETTLED -> palette.success
    RouteBuilderPaymentTone.TANDA -> palette.accent
}
