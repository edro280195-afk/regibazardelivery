package com.example.regibazarapp.ui.screens.inventory

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import com.example.regibazarapp.data.models.InventoryBarcodeMatchDto
import com.example.regibazarapp.data.models.InventoryBoxDto
import com.example.regibazarapp.data.models.InventoryBoxSummaryDto
import com.example.regibazarapp.data.models.InventoryCountItemDto
import com.example.regibazarapp.data.models.InventoryItemDto
import com.example.regibazarapp.data.models.LabelPrintMethod
import com.example.regibazarapp.labelprint.LabelOutputController
import com.example.regibazarapp.nfc.NfcInventoryReader
import com.example.regibazarapp.ui.components.DirectPrinterPickerDialog
import com.example.regibazarapp.ui.components.InventoryCodeScannerOverlay
import com.example.regibazarapp.ui.components.LabelOutputPreviewDialog
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.theme.regiBazar2026Palette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = regiBazar2026Palette()
    val context = LocalContext.current
    val labelOutput = remember(context) { LabelOutputController(context) }
    val activity = context as? FragmentActivity
    val reader = remember(activity) { activity?.let(::NfcInventoryReader) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showAddItemDialog by rememberSaveable { mutableStateOf(false) }
    var showCountDialog by rememberSaveable { mutableStateOf(false) }
    var showPrinterPicker by rememberSaveable { mutableStateOf(false) }
    var showCodeScanner by rememberSaveable { mutableStateOf(false) }
    var scannerTarget by rememberSaveable { mutableStateOf("lookup") }
    var scannedItemBarcode by rememberSaveable { mutableStateOf("") }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var showCameraPermissionHelp by rememberSaveable { mutableStateOf(false) }
    var scannerMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var transferItem by remember { mutableStateOf<InventoryItemDto?>(null) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (granted) showCodeScanner = true else showCameraPermissionHelp = true
    }

    fun requestCodeScanner(target: String) {
        scannerTarget = target
        if (hasCameraPermission) showCodeScanner = true
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(reader) {
        reader?.start(viewModel::onTagDiscovered)
        onDispose { reader?.stop() }
    }

    BackHandler(enabled = uiState.selectedBox != null) { viewModel.closeBox() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.selectedBox == null) "Mi bodega" else uiState.selectedBox!!.code,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.selectedBox == null) onBack() else viewModel.closeBox() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadPairedPrinters()
                        showPrinterPicker = true
                    }) {
                        Icon(
                            Icons.Rounded.Print,
                            contentDescription = "Configurar impresora predeterminada",
                            tint = if (uiState.directPrinterSettings.isConfigured) palette.accent else palette.textSecondary
                        )
                    }
                    if (uiState.selectedBox == null) {
                        IconButton(onClick = { requestCodeScanner("lookup") }) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Escanear QR o código de barras")
                        }
                        IconButton(onClick = viewModel::loadBoxes) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar cajas")
                        }
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Crear caja")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.background)
            )
        },
        containerColor = palette.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            InventoryMessage(uiState, palette, viewModel::clearMessage)
            when {
                uiState.isLoading && uiState.boxes.isEmpty() -> LoadingInventory()
                uiState.selectedBox != null -> BoxDetail(
                    box = uiState.selectedBox!!,
                    palette = palette,
                    isSaving = uiState.isSaving,
                    isPreparingLabel = uiState.isPreparingLabel,
                    isWritingNfc = uiState.waitingToWriteNfc || uiState.isProcessingNfc,
                    onPrepareNfc = viewModel::prepareNfcWrite,
                    onPrintBox = viewModel::prepareBoxLabel,
                    onPrintItem = viewModel::prepareItemLabel,
                    onAddItem = { scannedItemBarcode = ""; showAddItemDialog = true },
                    onCount = { showCountDialog = true },
                    onScanCode = { requestCodeScanner("lookup") },
                    onAdjust = viewModel::adjustItem,
                    onTransfer = { transferItem = it }
                )
                else -> BoxesList(
                    boxes = uiState.boxes,
                    palette = palette,
                    onBoxSelected = viewModel::selectBox,
                    onCreateBox = { showCreateDialog = true }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateBoxDialog(
            isSaving = uiState.isSaving,
            onDismiss = { showCreateDialog = false },
            onConfirm = { code, name, location ->
                viewModel.createBox(code, name, location)
                showCreateDialog = false
            }
        )
    }

    if (showAddItemDialog && uiState.selectedBox != null) {
        AddInventoryItemDialog(
            defaultName = uiState.selectedBox?.name.orEmpty(),
            isSaving = uiState.isSaving,
            barcodeValue = scannedItemBarcode,
            onBarcodeChange = { scannedItemBarcode = it },
            onScanBarcode = { requestCodeScanner("itemBarcode") },
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, variant, barcode, quantity, note ->
                viewModel.addItem(name, variant, barcode, quantity, note)
                showAddItemDialog = false
            }
        )
    }

    transferItem?.let { item ->
        val source = uiState.selectedBox
        if (source != null) {
            TransferInventoryItemDialog(
                item = item,
                boxes = uiState.boxes.filter { it.id != source.id },
                isSaving = uiState.isSaving,
                onDismiss = { transferItem = null },
                onConfirm = { destinationId, quantity, note ->
                    viewModel.transfer(item, destinationId, quantity, note)
                    transferItem = null
                }
            )
        }
    }

    if (showCountDialog && uiState.selectedBox != null) {
        CountInventoryDialog(
            box = uiState.selectedBox!!,
            isSaving = uiState.isSaving,
            onDismiss = { showCountDialog = false },
            onConfirm = { items, note ->
                viewModel.completeCount(items, note)
                showCountDialog = false
            }
        )
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
                    .onFailure { viewModel.reportLabelOutputError(it.message ?: "No pudimos compartir la etiqueta.") }
            },
            onSystemPrint = {
                runCatching { labelOutput.openSystemPrint(preview.bitmap, preview.profile, preview.title) }
                    .onSuccess {
                        viewModel.registerLabelOutput(LabelPrintMethod.NativeSystemPrint)
                        viewModel.dismissLabelPreview()
                    }
                    .onFailure { viewModel.reportLabelOutputError(it.message ?: "No pudimos abrir la impresión de Android.") }
            },
            directPrinterSettings = uiState.directPrinterSettings,
            isDirectPrinting = uiState.isDirectPrinting,
            onDirectPrint = { viewModel.printDirect(preview) },
            onConfigurePrinter = {
                viewModel.loadPairedPrinters()
                showPrinterPicker = true
            }
        )
    }

    if (showPrinterPicker) {
        DirectPrinterPickerDialog(
            title = "Impresora de cajas (bodega)",
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

    if (uiState.barcodeMatches.isNotEmpty()) {
        BarcodeMatchesDialog(
            matches = uiState.barcodeMatches,
            onDismiss = viewModel::dismissBarcodeMatches,
            onOpen = viewModel::openBarcodeMatch
        )
    }

    if (showCameraPermissionHelp) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionHelp = false },
            title = { Text("Permiso de cámara") },
            text = { Text("Necesitamos la cámara para leer QR y códigos de barras. Puedes habilitarla desde Ajustes si cambias de opinión.") },
            confirmButton = { Button(onClick = { showCameraPermissionHelp = false }) { Text("Entendido") } }
        )
    }

    scannerMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { scannerMessage = null },
            title = { Text("Código no aplicable") },
            text = { Text(message) },
            confirmButton = { Button(onClick = { scannerMessage = null }) { Text("Entendido") } }
        )
    }

    if (showCodeScanner) {
        InventoryCodeScannerOverlay(
            onCodeScanned = { value ->
                showCodeScanner = false
                if (scannerTarget == "itemBarcode") {
                    if (NfcInventoryReader.tokenFromInventoryUrl(value) != null) {
                        scannerMessage = "Ese QR identifica una caja. Escanéalo desde la pantalla de Mi bodega, no dentro del artículo."
                    } else {
                        scannedItemBarcode = value
                    }
                } else {
                    viewModel.scanCode(value)
                }
            },
            onDismiss = { showCodeScanner = false }
        )
    }
}

@Composable
private fun InventoryMessage(
    state: InventoryUiState,
    palette: RegiBazar2026Palette,
    onDismiss: () -> Unit
) {
    val message = state.error ?: state.notice ?: return
    val isError = state.error != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (isError) palette.errorSoft else palette.accentSoft),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isError) "⚠️" else "✨")
            Spacer(Modifier.width(9.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) palette.error else palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                Text("×", style = MaterialTheme.typography.titleLarge)
            }
        }
    }

}

@Composable
private fun LoadingInventory() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BoxesList(
    boxes: List<InventoryBoxSummaryDto>,
    palette: RegiBazar2026Palette,
    onBoxSelected: (String) -> Unit,
    onCreateBox: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.accentSoft),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Nfc, contentDescription = null, tint = palette.accent, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Acerca una etiqueta", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                        Text("La caja se abrirá solita. Para crear una, usa el botón +.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                }
            }
        }
        if (boxes.isEmpty()) {
            item {
                EmptyInventory(onCreateBox, palette)
            }
        } else {
            items(boxes, key = { it.id }) { box ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBoxSelected(box.id) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(palette.accentSoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("🧺", style = MaterialTheme.typography.headlineSmall) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${box.code} · ${box.name}", fontWeight = FontWeight.ExtraBold, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(box.location ?: "Ubicación pendiente", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary, maxLines = 1)
                            Spacer(Modifier.height(3.dp))
                            Text("${box.articleTypesCount} artículos · ${box.totalUnits} piezas", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                        }
                        Text(if (box.isNfcBound) "NFC" else "Pend.", style = MaterialTheme.typography.labelSmall, color = if (box.isNfcBound) palette.accent else palette.warning)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInventory(onCreateBox: () -> Unit, palette: RegiBazar2026Palette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🧺", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text("Tu bodega empieza aquí", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
        Text("Crea la primera caja y luego vincula una etiqueta NFC.", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onCreateBox) { Text("Crear primera caja") }
    }
}

@Composable
private fun BoxDetail(
    box: InventoryBoxDto,
    palette: RegiBazar2026Palette,
    isSaving: Boolean,
    isPreparingLabel: Boolean,
    isWritingNfc: Boolean,
    onPrepareNfc: () -> Unit,
    onPrintBox: () -> Unit,
    onPrintItem: (InventoryItemDto) -> Unit,
    onAddItem: () -> Unit,
    onCount: () -> Unit,
    onScanCode: () -> Unit,
    onAdjust: (InventoryItemDto, Int) -> Unit,
    onTransfer: (InventoryItemDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface)
            ) {
                Column(Modifier.padding(17.dp)) {
                    Text(box.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                    Text("📍 ${box.location ?: "Ubicación pendiente"}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onPrepareNfc,
                        enabled = !isWritingNfc,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                    ) {
                        Icon(Icons.Rounded.Nfc, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isWritingNfc) "Acerca la etiqueta…" else if (box.isNfcBound) "Reescribir etiqueta NFC" else "Vincular etiqueta NFC")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onPrintBox, enabled = !isPreparingLabel, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isPreparingLabel) "Preparando etiqueta térmica…" else "Imprimir etiqueta de caja")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onAddItem, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Agregar artículo")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onScanCode, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Escanear QR o código")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onCount, enabled = !isSaving && box.items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Calculate, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Hacer conteo físico")
                    }
                }
            }
        }
        item { Text("Contenido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary, modifier = Modifier.padding(top = 4.dp, start = 2.dp)) }
        if (box.items.isEmpty()) {
            item { Text("Todavía no hay artículos en esta caja.", color = palette.textSecondary, modifier = Modifier.padding(vertical = 20.dp)) }
        } else {
            items(box.items, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = palette.surface), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
                                Text(listOfNotNull(item.variant, item.barcode).joinToString(" · ").ifBlank { "Sin variante" }, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            }
                            QuantityEditor(item.quantity, !isSaving, { onAdjust(item, -1) }, { onAdjust(item, 1) }, palette)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = palette.divider)
                        OutlinedButton(onClick = { onTransfer(item) }, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text("Mover a otra caja")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { onPrintItem(item) }, enabled = !isPreparingLabel, modifier = Modifier.fillMaxWidth()) {
                            Text(if (isPreparingLabel) "Preparando etiqueta térmica…" else "Imprimir etiqueta de artículo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityEditor(quantity: Int, enabled: Boolean, onMinus: () -> Unit, onPlus: () -> Unit, palette: RegiBazar2026Palette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus, enabled = enabled && quantity > 0) { Text("−", style = MaterialTheme.typography.titleLarge, color = palette.accent) }
        Text(quantity.toString(), fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
        IconButton(onClick = onPlus, enabled = enabled) { Text("+", style = MaterialTheme.typography.titleLarge, color = palette.accent) }
    }
}

@Composable
private fun CreateBoxDialog(isSaving: Boolean, onDismiss: () -> Unit, onConfirm: (String, String, String?) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva caja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(code, { code = it }, label = { Text("Código visible, ej. B-01") }, singleLine = true)
                OutlinedTextField(name, { name = it }, label = { Text("¿Qué guardarás?") }, singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("Ubicación") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(code, name, location) }, enabled = !isSaving && code.isNotBlank() && name.isNotBlank()) { Text("Crear caja") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AddInventoryItemDialog(
    defaultName: String = "",
    isSaving: Boolean,
    barcodeValue: String,
    onBarcodeChange: (String) -> Unit,
    onScanBarcode: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, Int, String?) -> Unit
) {
    var name by rememberSaveable(defaultName) { mutableStateOf(defaultName) }
    var variant by rememberSaveable { mutableStateOf("") }
    var quantityText by rememberSaveable { mutableStateOf("1") }
    var note by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (defaultName.isNotBlank()) "Agregar a $defaultName" else "Agregar artículo",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (defaultName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Contenido de la caja: ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                defaultName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = variant,
                    onValueChange = { variant = it },
                    label = { Text("Talla, color o variante (ej. King Size Azul)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del artículo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter(Char::isDigit) },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = barcodeValue,
                    onValueChange = onBarcodeChange,
                    label = { Text("Código de barras o QR de artículo") },
                    trailingIcon = {
                        IconButton(onClick = onScanBarcode) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Escanear código")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val quantity = quantityText.toIntOrNull() ?: 0
            val finalName = name.ifBlank { defaultName }.trim()
            Button(
                onClick = { onConfirm(finalName, variant, barcodeValue, quantity, note) },
                enabled = !isSaving && finalName.isNotBlank() && quantity > 0
            ) {
                Text("Guardar artículo")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun CountInventoryDialog(
    box: InventoryBoxDto,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<InventoryCountItemDto>, String?) -> Unit
) {
    val quantities = remember(box.id) {
        box.items.map { item -> item.id to item.quantity.toString() }.toMutableStateMap()
    }
    var note by rememberSaveable(box.id) { mutableStateOf("") }
    val countItems = box.items.map { item ->
        InventoryCountItemDto(item.id, quantities[item.id]?.toIntOrNull() ?: -1)
    }
    val canSave = countItems.all { it.actualQuantity >= 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conteo físico · ${box.code}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text("Cuenta cada artículo. El sistema sólo ajustará las diferencias.", style = MaterialTheme.typography.bodySmall)
                }
                items(box.items, key = { it.id }) { item ->
                    OutlinedTextField(
                        value = quantities[item.id].orEmpty(),
                        onValueChange = { quantities[item.id] = it.filter(Char::isDigit) },
                        label = { Text("${item.name} · Sistema: ${item.quantity}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Nota del conteo (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(countItems, note) },
                enabled = !isSaving && canSave
            ) { Text("Guardar conteo") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun BarcodeMatchesDialog(
    matches: List<InventoryBarcodeMatchDto>,
    onDismiss: () -> Unit,
    onOpen: (InventoryBarcodeMatchDto) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("El código está en varias cajas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Elige la caja que quieres abrir.")
                matches.forEach { match ->
                    OutlinedButton(onClick = { onOpen(match) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text("${match.boxCode} · ${match.boxName}", fontWeight = FontWeight.ExtraBold)
                            Text("${match.itemName} · ${match.quantity} piezas", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { OutlinedButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun TransferInventoryItemDialog(
    item: InventoryItemDto,
    boxes: List<InventoryBoxSummaryDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String?) -> Unit
) {
    var destinationId by rememberSaveable { mutableStateOf("") }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var quantityText by rememberSaveable { mutableStateOf("1") }
    var note by rememberSaveable { mutableStateOf("") }
    val selectedName = boxes.firstOrNull { it.id == destinationId }?.let { "${it.code} · ${it.name}" } ?: "Elige una caja"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mover ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { menuExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        boxes.forEach { box ->
                            DropdownMenuItem(
                                text = { Text("${box.code} · ${box.name}") },
                                onClick = { destinationId = box.id; menuExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(quantityText, { quantityText = it.filter(Char::isDigit) }, label = { Text("Cantidad (máximo ${item.quantity})") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Nota (opcional)") }, singleLine = true)
            }
        },
        confirmButton = {
            val quantity = quantityText.toIntOrNull() ?: 0
            Button(onClick = { onConfirm(destinationId, quantity, note) }, enabled = !isSaving && destinationId.isNotBlank() && quantity in 1..item.quantity) { Text("Mover") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
