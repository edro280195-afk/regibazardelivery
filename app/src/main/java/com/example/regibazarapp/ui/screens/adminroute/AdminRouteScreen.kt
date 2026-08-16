package com.example.regibazarapp.ui.screens.adminroute

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.data.models.RouteListDto
import com.example.regibazarapp.data.models.LabelPrintMethod
import com.example.regibazarapp.labelprint.LabelOutputController
import com.example.regibazarapp.ui.components.DirectPackageLabelPrintCard
import com.example.regibazarapp.ui.components.DirectPrinterPickerDialog
import com.example.regibazarapp.ui.components.LabelOutputPreviewDialog
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.theme.RegiBazar2026Tokens
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import com.example.regibazarapp.ui.utils.formatRouteMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRouteScreen(
    routeId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onOpenDriver: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: AdminRouteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = regiBazar2026Palette()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val labelOutput = remember(context) { LabelOutputController(context) }
    var removeCandidate by remember { mutableStateOf<RouteDeliveryDto?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showPrinterPicker by remember { mutableStateOf(false) }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showPrinterPicker = true
            viewModel.loadPairedPrinters()
        } else {
            viewModel.reportLabelOutputError("Autoriza Bluetooth para configurar la impresora directa.")
        }
    }

    fun openPrinterPicker() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            showPrinterPicker = true
            viewModel.loadPairedPrinters()
        }
    }

    LaunchedEffect(routeId) {
        viewModel.initialize(routeId)
    }

    LaunchedEffect(uiState.error, uiState.message) {
        val feedback = uiState.message
            ?: uiState.error?.takeIf { uiState.route != null }
        if (!feedback.isNullOrBlank()) {
            snackbarHostState.showSnackbar(feedback)
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(uiState.routeDeleted) {
        if (uiState.routeDeleted) onDeleted()
    }

    removeCandidate?.let { delivery ->
        RemoveStopDialog(
            delivery = delivery,
            palette = palette,
            onConfirm = {
                viewModel.removeDelivery(delivery)
                removeCandidate = null
            },
            onDismiss = { removeCandidate = null }
        )
    }

    if (showDeleteDialog) {
        DeleteRouteDialog(
            routeId = routeId,
            palette = palette,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteRoute()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            AdminRouteHeader(
                routeId = routeId,
                palette = palette,
                isWorking = uiState.isWorking,
                menuExpanded = showMenu,
                onBack = onBack,
                onRefresh = { viewModel.loadRoute(showLoading = false) },
                onMenuClick = { showMenu = true },
                onMenuDismiss = { showMenu = false },
                onLiquidate = {
                    showMenu = false
                    viewModel.liquidate()
                },
                onDelete = {
                    showMenu = false
                    showDeleteDialog = true
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = palette.background
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(palette.background)
        ) {
            when {
                uiState.isLoading -> AdminRouteLoadingState(palette)

                uiState.route != null -> {
                    val route = uiState.route
                    if (route != null) {
                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.loadRoute(showLoading = false) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AdminRouteContent(
                                route = route,
                                uiState = uiState,
                                palette = palette,
                                onOptimize = viewModel::optimize,
                                onEdit = { onEdit(route.id) },
                                onOpenDriver = { onOpenDriver(route.driverToken) },
                                onShare = { shareRoute(context, route) },
                                onMoveUp = { deliveryId ->
                                    viewModel.moveDelivery(deliveryId, -1)
                                },
                                onMoveDown = { deliveryId ->
                                    viewModel.moveDelivery(deliveryId, 1)
                                },
                                onRemove = { removeCandidate = it },
                                onCall = { phone -> openPhoneDialer(context, phone) },
                                onDirections = { delivery -> openDirections(context, delivery) },
                                onPrintPackage = viewModel::printPackage,
                                onPreviewPackage = viewModel::preparePackageLabel,
                                onPrintAllPackages = viewModel::printAllRoutePackages,
                                onRetryDirectPrints = viewModel::retryFailedDirectPrints,
                                onConfigurePrinter = ::openPrinterPicker
                            )
                        }
                    }
                }

                else -> {
                    AdminRouteErrorState(
                        message = uiState.error ?: "No pude cargar la ruta.",
                        palette = palette,
                        onRetry = { viewModel.loadRoute() }
                    )
                }
            }

            if (uiState.isWorking) {
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

    uiState.labelPreview?.let { preview ->
        LabelOutputPreviewDialog(
            preview = preview,
            onDismiss = viewModel::dismissLabelPreview,
            onShare = {
                runCatching { labelOutput.shareWithPrinterApp(preview.bitmap, preview.title) }
                    .onSuccess {
                        viewModel.registerLabelOutput(LabelPrintMethod.SystemShare)
                        viewModel.dismissLabelPreview()
                    }
                    .onFailure { viewModel.reportLabelOutputError(it.message ?: "No pudimos compartir la etiqueta de bolsa.") }
            },
            onSystemPrint = {
                runCatching { labelOutput.openSystemPrint(preview.bitmap, preview.profile, preview.title) }
                    .onSuccess {
                        viewModel.registerLabelOutput(LabelPrintMethod.NativeSystemPrint)
                        viewModel.dismissLabelPreview()
                    }
                    .onFailure { viewModel.reportLabelOutputError(it.message ?: "No pudimos abrir la impresión de Android.") }
            }
        )
    }

    if (showPrinterPicker) {
        DirectPrinterPickerDialog(
            title = "Impresora de bolsas (pedidos)",
            printers = uiState.pairedPrinters,
            isLoading = uiState.isLoadingPairedPrinters,
            settings = uiState.directPrinterSettings,
            palette = palette,
            onSelect = { printer ->
                viewModel.saveDirectPrinter(printer)
                showPrinterPicker = false
            },
            onClear = {
                viewModel.clearDirectPrinter()
                showPrinterPicker = false
            },
            onRefresh = viewModel::loadPairedPrinters,
            onDismiss = { showPrinterPicker = false }
        )
    }
}

@Composable
private fun AdminRouteHeader(
    routeId: Int,
    palette: RegiBazar2026Palette,
    isWorking: Boolean,
    menuExpanded: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onLiquidate: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Regresar a rutas",
                tint = palette.textPrimary
            )
        }
        Text(
            text = "Ruta #$routeId",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        AdminHeaderButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = "Actualizar ruta",
            palette = palette,
            enabled = !isWorking,
            onClick = onRefresh
        )
        Spacer(Modifier.width(4.dp))
        Box {
            AdminHeaderButton(
                icon = Icons.Rounded.MoreVert,
                contentDescription = "Más acciones",
                palette = palette,
                enabled = !isWorking,
                onClick = onMenuClick
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onMenuDismiss
            ) {
                DropdownMenuItem(
                    text = { Text("Liquidar ruta") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null)
                    },
                    enabled = !isWorking,
                    onClick = onLiquidate
                )
                DropdownMenuItem(
                    text = { Text("Eliminar ruta", color = palette.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = palette.error
                        )
                    },
                    enabled = !isWorking,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun AdminHeaderButton(
    icon: ImageVector,
    contentDescription: String,
    palette: RegiBazar2026Palette,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget),
        shape = RoundedCornerShape(13.dp),
        color = palette.surface,
        contentColor = palette.textPrimary,
        shadowElevation = 3.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AdminRouteContent(
    route: RouteListDto,
    uiState: AdminRouteUiState,
    palette: RegiBazar2026Palette,
    onOptimize: () -> Unit,
    onEdit: () -> Unit,
    onOpenDriver: () -> Unit,
    onShare: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (RouteDeliveryDto) -> Unit,
    onCall: (String) -> Unit,
    onDirections: (RouteDeliveryDto) -> Unit,
    onPrintPackage: (String, String, Int) -> Unit,
    onPreviewPackage: (String, String, Int) -> Unit,
    onPrintAllPackages: () -> Unit,
    onRetryDirectPrints: () -> Unit,
    onConfigurePrinter: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = RegiBazar2026Tokens.ScreenPadding,
            end = RegiBazar2026Tokens.ScreenPadding,
            top = 4.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!uiState.notice.isNullOrBlank()) {
            item(key = "route-notice") {
                AdminRouteNotice(uiState.notice, palette)
            }
        }

        item(key = "route-summary") {
            AdminRouteSummary(
                route = route,
                palette = palette,
                onShare = onShare
            )
        }

        val packageCount = route.deliveries.sumOf { delivery -> delivery.packages.orEmpty().size }
        if (packageCount > 0) {
            item(key = "direct-package-print") {
                DirectPackageLabelPrintCard(
                    packageCount = packageCount,
                    settings = uiState.directPrinterSettings,
                    queue = uiState.directPrintQueue,
                    isPrinting = uiState.isDirectPrintRunning,
                    palette = palette,
                    onPrintAll = onPrintAllPackages,
                    onRetryFailed = onRetryDirectPrints,
                    onConfigurePrinter = onConfigurePrinter
                )
            }
        }

        item(key = "route-actions") {
            AdminRouteActions(
                enabled = !uiState.isWorking,
                palette = palette,
                onOptimize = onOptimize,
                onEdit = onEdit,
                onOpenDriver = onOpenDriver
            )
        }

        item(key = "stops-label") {
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PARADAS DE LA RUTA",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.textSecondary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = route.deliveries.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent
                )
            }
        }

        if (route.deliveries.isEmpty()) {
            item(key = "empty-stops") {
                EmptyStopsState(palette, onEdit)
            }
        } else {
            itemsIndexed(
                items = route.deliveries,
                key = { _, delivery -> delivery.deliveryId }
            ) { index, delivery ->
                AdminStopCard(
                    position = index + 1,
                    delivery = delivery,
                    palette = palette,
                    canMoveUp = index > 0,
                    canMoveDown = index < route.deliveries.lastIndex,
                    enabled = !uiState.isWorking,
                    onMoveUp = { onMoveUp(delivery.deliveryId) },
                    onMoveDown = { onMoveDown(delivery.deliveryId) },
                    onRemove = { onRemove(delivery) },
                    onCall = { delivery.clientPhone?.let(onCall) },
                    onDirections = { onDirections(delivery) },
                    isPreparingLabel = uiState.isPreparingLabel,
                    isDirectPrintRunning = uiState.isDirectPrintRunning,
                    onPrintPackage = { packageId, packageNumber -> onPrintPackage(packageId, delivery.clientName, packageNumber) },
                    onPreviewPackage = { packageId, packageNumber -> onPreviewPackage(packageId, delivery.clientName, packageNumber) }
                )
            }
        }
    }
}

@Composable
private fun AdminRouteNotice(
    message: String,
    palette: RegiBazar2026Palette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.warningSoft, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = palette.warning,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AdminRouteSummary(
    route: RouteListDto,
    palette: RegiBazar2026Palette,
    onShare: () -> Unit
) {
    val statusColors = adminStatusColors(route.status, palette)
    val isClosed = route.status.equals("Completed", ignoreCase = true)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = statusColors.background,
                    contentColor = statusColors.foreground
                ) {
                    Text(
                        text = adminRouteStatusLabel(route.status),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
                ) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = "Compartir enlace de la ruta",
                        tint = palette.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth()) {
                SummaryMetric(
                    label = "Paradas",
                    value = route.totalCount.toString(),
                    color = palette.textPrimary,
                    secondaryColor = palette.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = "Entregadas",
                    value = route.deliveredCount.toString(),
                    color = palette.success,
                    secondaryColor = palette.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = if (isClosed) "Fallidas" else "Pendientes",
                    value = if (isClosed) {
                        route.failedCount.toString()
                    } else {
                        route.pendingCount.toString()
                    },
                    color = if (isClosed) palette.error else palette.money,
                    secondaryColor = palette.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = palette.divider
            )

            Text(
                text = "Cobro esperado",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = formatRouteMoney(adminExpectedCollection(route)),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.money,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    color: Color,
    secondaryColor: Color,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 25.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = secondaryColor
        )
    }
}

@Composable
private fun AdminRouteActions(
    enabled: Boolean,
    palette: RegiBazar2026Palette,
    onOptimize: () -> Unit,
    onEdit: () -> Unit,
    onOpenDriver: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOptimize,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color(0xFFF8FAFF),
                    disabledContainerColor = palette.accentSoft,
                    disabledContentColor = palette.textSecondary
                )
            ) {
                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Optimizar", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onEdit,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accentSoft,
                    contentColor = palette.accent,
                    disabledContainerColor = palette.surfaceMuted,
                    disabledContentColor = palette.textSecondary
                )
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Armar", fontWeight = FontWeight.Bold)
            }
        }

        DashedActionButton(
            label = "Abrir modo reparto",
            icon = Icons.Rounded.PlayArrow,
            enabled = enabled,
            palette = palette,
            onClick = onOpenDriver
        )
    }
}

@Composable
private fun DashedActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    palette: RegiBazar2026Palette,
    onClick: () -> Unit
) {
    val borderColor = if (enabled) palette.accent.copy(alpha = 0.38f) else palette.divider
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .drawWithCache {
                val strokeWidth = 1.5.dp.toPx()
                val cornerRadius = 14.dp.toPx()
                val pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(10.dp.toPx(), 7.dp.toPx())
                )
                onDrawBehind {
                    drawRoundRect(
                        color = borderColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                        style = Stroke(width = strokeWidth, pathEffect = pathEffect)
                    )
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) palette.accentSoft.copy(alpha = 0.52f) else palette.surfaceMuted,
        contentColor = if (enabled) palette.accent else palette.textSecondary
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminStopCard(
    position: Int,
    delivery: RouteDeliveryDto,
    palette: RegiBazar2026Palette,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    enabled: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onCall: () -> Unit,
    onDirections: () -> Unit,
    isPreparingLabel: Boolean,
    isDirectPrintRunning: Boolean,
    onPrintPackage: (String, Int) -> Unit,
    onPreviewPackage: (String, Int) -> Unit
) {
    val payment = remember(delivery) { stopPaymentInfo(delivery) }
    val paymentColor = when (payment.tone) {
        StopPaymentTone.MONEY -> palette.money
        StopPaymentTone.SUCCESS -> palette.success
        StopPaymentTone.TANDA -> palette.accent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(deliveryStatusColor(delivery.status, palette), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF8FAFF)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = delivery.clientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stopDisplayAddress(delivery),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (delivery.displayAddress == null) {
                            palette.warning
                        } else {
                            palette.textSecondary
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = payment.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = paymentColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 11.dp),
                color = palette.divider
            )

            if (delivery.packages.orEmpty().isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Etiquetas de bolsas",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondary
                    )
                    delivery.packages.orEmpty().forEach { packageInfo ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { onPrintPackage(packageInfo.id, packageInfo.packageNumber) },
                                enabled = enabled && !isPreparingLabel && !isDirectPrintRunning,
                                colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)
                            ) {
                                Text(
                                    if (isPreparingLabel || isDirectPrintRunning) {
                                        "Preparando etiqueta…"
                                    } else {
                                        "Imprimir bolsa ${packageInfo.packageNumber}"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(
                                onClick = { onPreviewPackage(packageInfo.id, packageInfo.packageNumber) },
                                enabled = enabled && !isPreparingLabel && !isDirectPrintRunning,
                                colors = ButtonDefaults.textButtonColors(contentColor = palette.textSecondary)
                            ) {
                                Text("Vista previa", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDirections,
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)
                ) {
                    Icon(
                        Icons.Rounded.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Llegar", fontWeight = FontWeight.Bold)
                }

                if (!delivery.clientPhone.isNullOrBlank()) {
                    StopIconButton(
                        icon = Icons.Rounded.Call,
                        contentDescription = "Llamar a ${delivery.clientName}",
                        tint = palette.success,
                        enabled = enabled,
                        onClick = onCall
                    )
                }

                Spacer(Modifier.weight(1f))

                StopIconButton(
                    icon = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Subir parada",
                    tint = palette.textSecondary,
                    enabled = canMoveUp && enabled,
                    onClick = onMoveUp
                )
                StopIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Bajar parada",
                    tint = palette.textSecondary,
                    enabled = canMoveDown && enabled,
                    onClick = onMoveDown
                )
                StopIconButton(
                    icon = Icons.Rounded.DeleteOutline,
                    contentDescription = "Retirar parada",
                    tint = palette.error,
                    enabled = enabled,
                    onClick = onRemove
                )
            }
        }
    }
}

@Composable
private fun StopIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun EmptyStopsState(
    palette: RegiBazar2026Palette,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Route,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(42.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Esta ruta no tiene paradas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
        Text(
            "Usa Armar para agregar pedidos o tandas.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )
        Spacer(Modifier.height(14.dp))
        TextButton(
            onClick = onEdit,
            colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Armar ruta", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminRouteLoadingState(palette: RegiBazar2026Palette) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LoadingBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                radius = 20.dp,
                palette = palette
            )
        }
        item {
            LoadingBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                radius = 14.dp,
                palette = palette
            )
        }
        items(4) {
            LoadingBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp),
                radius = 16.dp,
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
private fun AdminRouteErrorState(
    message: String,
    palette: RegiBazar2026Palette,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = palette.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No pudimos cargar esta ruta",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accent,
                contentColor = Color(0xFFF8FAFF)
            )
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

@Composable
private fun RemoveStopDialog(
    delivery: RouteDeliveryDto,
    palette: RegiBazar2026Palette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = palette.error
            )
        },
        title = { Text("Retirar parada", color = palette.textPrimary) },
        text = {
            Text(
                "¿Quitar a ${delivery.clientName} de esta ruta?",
                color = palette.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.error,
                    contentColor = Color(0xFFF8FAFF)
                )
            ) {
                Text("Quitar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        containerColor = palette.surface
    )
}

@Composable
private fun DeleteRouteDialog(
    routeId: Int,
    palette: RegiBazar2026Palette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = palette.error
            )
        },
        title = { Text("Eliminar ruta #$routeId", color = palette.textPrimary) },
        text = {
            Text(
                "Los pedidos pendientes regresarán al tablero. Esta acción no se puede deshacer.",
                color = palette.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.error,
                    contentColor = Color(0xFFF8FAFF)
                )
            ) {
                Text("Eliminar definitivamente")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        containerColor = palette.surface
    )
}

private data class StatusColors(
    val foreground: Color,
    val background: Color
)

private fun adminStatusColors(
    status: String,
    palette: RegiBazar2026Palette
): StatusColors = when (adminRouteStatusTone(status)) {
    AdminRouteStatusTone.ACTIVE -> StatusColors(palette.accent, palette.accentSoft)
    AdminRouteStatusTone.COMPLETED -> StatusColors(palette.success, palette.successSoft)
    AdminRouteStatusTone.CANCELED -> StatusColors(palette.error, palette.errorSoft)
    AdminRouteStatusTone.PENDING -> StatusColors(palette.warning, palette.warningSoft)
}

private fun deliveryStatusColor(
    status: String,
    palette: RegiBazar2026Palette
): Color = when {
    status.equals("Delivered", ignoreCase = true) -> palette.success
    status.equals("NotDelivered", ignoreCase = true) -> palette.error
    else -> palette.accent
}

private fun shareRoute(context: Context, route: RouteListDto) {
    val text = route.driverLink
        ?: "https://regibazarapi.onrender.com/api/driver/${route.driverToken}"
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Compartir ruta"
        )
    )
}

private fun openPhoneDialer(context: Context, phone: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
}

private fun openDirections(context: Context, delivery: RouteDeliveryDto) {
    val destination = if (delivery.latitude != null && delivery.longitude != null) {
        "${delivery.latitude},${delivery.longitude}"
    } else {
        delivery.displayAddress ?: delivery.clientName
    }
    val encodedDestination = Uri.encode(destination)
    val googleMapsIntent = Intent(
        Intent.ACTION_VIEW,
        "google.navigation:q=$encodedDestination".toUri()
    ).setPackage("com.google.android.apps.maps")

    runCatching {
        context.startActivity(googleMapsIntent)
    }.getOrElse {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "geo:0,0?q=$encodedDestination".toUri()
            )
        )
    }
}
