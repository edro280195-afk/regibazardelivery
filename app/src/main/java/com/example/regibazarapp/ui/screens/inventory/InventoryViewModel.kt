package com.example.regibazarapp.ui.screens.inventory

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.regibazarapp.data.models.AdjustInventoryItemDto
import com.example.regibazarapp.data.models.CreateInventoryBoxDto
import com.example.regibazarapp.data.models.CreateInventoryItemDto
import com.example.regibazarapp.data.models.CompleteInventoryCountDto
import com.example.regibazarapp.data.models.InventoryBarcodeMatchDto
import com.example.regibazarapp.data.models.InventoryBoxDto
import com.example.regibazarapp.data.models.InventoryBoxSummaryDto
import com.example.regibazarapp.data.models.InventoryCountItemDto
import com.example.regibazarapp.data.models.InventoryItemDto
import com.example.regibazarapp.data.models.LabelPrintMethod
import com.example.regibazarapp.data.models.LabelTargetKind
import com.example.regibazarapp.data.models.TransferInventoryItemsDto
import com.example.regibazarapp.data.TokenManager
import com.example.regibazarapp.data.network.NetworkModule
import com.example.regibazarapp.data.repository.InventoryRepository
import com.example.regibazarapp.data.repository.RepositoryResult
import com.example.regibazarapp.labelprint.DirectBluetoothLabelPrinter
import com.example.regibazarapp.labelprint.DirectPrinterResult
import com.example.regibazarapp.labelprint.DirectPrinterSettings
import com.example.regibazarapp.labelprint.DirectPrinterSettingsStore
import com.example.regibazarapp.labelprint.PrinterScope
import com.example.regibazarapp.labelprint.LabelPrintCoordinator
import com.example.regibazarapp.labelprint.PairedBluetoothPrinter
import com.example.regibazarapp.labelprint.RenderedLabelPreview
import com.example.regibazarapp.nfc.NfcInventoryReader
import com.example.regibazarapp.nfc.NfcWriteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventoryUiState(
    val isLoading: Boolean = true,
    val isProcessingNfc: Boolean = false,
    val isSaving: Boolean = false,
    val isPreparingLabel: Boolean = false,
    val isDirectPrinting: Boolean = false,
    val isLoadingPairedPrinters: Boolean = false,
    val waitingToWriteNfc: Boolean = false,
    val boxes: List<InventoryBoxSummaryDto> = emptyList(),
    val selectedBox: InventoryBoxDto? = null,
    val barcodeMatches: List<InventoryBarcodeMatchDto> = emptyList(),
    val labelPreview: RenderedLabelPreview? = null,
    val directPrinterSettings: DirectPrinterSettings = DirectPrinterSettings(),
    val pairedPrinters: List<PairedBluetoothPrinter> = emptyList(),
    val notice: String? = null,
    val error: String? = null
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository()
    private val labelPrintCoordinator = LabelPrintCoordinator()
    private val directBluetoothPrinter = DirectBluetoothLabelPrinter(application)
    private val directPrinterSettingsStore = DirectPrinterSettingsStore(application, PrinterScope.BOX)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        ensureAuthToken()
        loadBoxes()
        viewModelScope.launch {
            directPrinterSettingsStore.settings.collectLatest { settings ->
                _uiState.update { current -> current.copy(directPrinterSettings = settings) }
            }
        }
    }

    private fun ensureAuthToken() {
        if (NetworkModule.jwtToken.isNullOrBlank()) {
            NetworkModule.jwtToken = tokenManager.getJwtTokenSync()
        }
    }

    fun loadPairedPrinters() {
        if (_uiState.value.isLoadingPairedPrinters) return
        _uiState.update { it.copy(isLoadingPairedPrinters = true, error = null) }
        when (val result = directBluetoothPrinter.listPairedPrinters()) {
            is DirectPrinterResult.Success -> _uiState.update {
                it.copy(isLoadingPairedPrinters = false, pairedPrinters = result.value)
            }
            is DirectPrinterResult.Error -> _uiState.update {
                it.copy(isLoadingPairedPrinters = false, error = result.message)
            }
        }
    }

    fun saveDirectPrinter(printer: PairedBluetoothPrinter) {
        viewModelScope.launch {
            directPrinterSettingsStore.save(printer)
            _uiState.update { it.copy(notice = "${printer.name} configurada como impresora predeterminada.") }
        }
    }

    fun clearDirectPrinter() {
        viewModelScope.launch {
            directPrinterSettingsStore.clear()
            _uiState.update { it.copy(notice = "Impresora predeterminada removida.") }
        }
    }

    fun printDirect(preview: RenderedLabelPreview) {
        val printerAddress = _uiState.value.directPrinterSettings.address
        if (printerAddress.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Primero configura tu impresora Bluetooth predeterminada.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDirectPrinting = true, error = null) }
            when (val result = directBluetoothPrinter.printE40(preview.bitmap, printerAddress)) {
                is DirectPrinterResult.Success -> {
                    labelPrintCoordinator.register(preview, LabelPrintMethod.NativeBluetooth)
                    _uiState.update {
                        it.copy(
                            isDirectPrinting = false,
                            labelPreview = null,
                            notice = "Etiqueta enviada directamente a ${result.value.printerName}."
                        )
                    }
                }
                is DirectPrinterResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isDirectPrinting = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun loadBoxes() {
        viewModelScope.launch {
            ensureAuthToken()
            _uiState.update { it.copy(isLoading = it.boxes.isEmpty(), error = null) }
            when (val result = repository.getBoxes()) {
                is RepositoryResult.Success -> _uiState.update { it.copy(isLoading = false, boxes = result.value) }
                is RepositoryResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun selectBox(boxId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getBox(boxId)) {
                is RepositoryResult.Success -> updateSelectedBox(result.value, "Caja abierta.")
                is RepositoryResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun closeBox() {
        _uiState.update { it.copy(selectedBox = null, notice = null, error = null, waitingToWriteNfc = false) }
    }

    fun createBox(code: String, name: String, location: String?) {
        if (code.isBlank() || name.isBlank()) {
            _uiState.update { it.copy(error = "El código y el nombre de la caja son obligatorios.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val result = repository.createBox(CreateInventoryBoxDto(code.trim(), name.trim(), location.nullIfBlank()))) {
                is RepositoryResult.Success -> updateSelectedBox(result.value, "Caja creada. Toca “Vincular etiqueta NFC” para prepararla.")
                is RepositoryResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
            }
        }
    }

    fun addItem(name: String, variant: String?, barcode: String?, quantity: Int, note: String?) {
        val box = _uiState.value.selectedBox ?: return
        if (name.isBlank() || quantity <= 0) {
            _uiState.update { it.copy(error = "Escribe el artículo y una cantidad válida.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val request = CreateInventoryItemDto(name.trim(), variant.nullIfBlank(), barcode.nullIfBlank(), quantity, note.nullIfBlank())
            when (val result = repository.addItem(box.id, request)) {
                is RepositoryResult.Success -> updateSelectedBox(result.value, "Artículo guardado en ${box.code}.")
                is RepositoryResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
            }
        }
    }

    fun adjustItem(item: InventoryItemDto, delta: Int) {
        if (item.quantity + delta < 0 || delta == 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val result = repository.adjustItem(item.id, AdjustInventoryItemDto(delta, null))) {
                is RepositoryResult.Success -> updateSelectedBox(result.value, null)
                is RepositoryResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
            }
        }
    }

    fun transfer(item: InventoryItemDto, destinationBoxId: String, quantity: Int, note: String?) {
        val source = _uiState.value.selectedBox ?: return
        if (destinationBoxId.isBlank() || quantity !in 1..item.quantity) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val request = TransferInventoryItemsDto(source.id, destinationBoxId, item.id, quantity, note.nullIfBlank())
            when (val result = repository.transfer(request)) {
                is RepositoryResult.Success -> {
                    // La API regresa el destino; recargamos el origen para mantener la vista abierta coherente.
                    when (val sourceResult = repository.getBox(source.id)) {
                        is RepositoryResult.Success -> updateSelectedBox(sourceResult.value, "Mercancía movida correctamente.")
                        is RepositoryResult.Error -> _uiState.update { it.copy(isSaving = false, error = sourceResult.message) }
                    }
                    loadBoxes()
                }
                is RepositoryResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
            }
        }
    }

    fun completeCount(items: List<InventoryCountItemDto>, note: String?) {
        val box = _uiState.value.selectedBox ?: return
        if (items.size != box.items.size || items.any { it.actualQuantity < 0 }) {
            _uiState.update { it.copy(error = "Cuenta todos los artículos con cantidades de cero o más.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val result = repository.completeCount(box.id, CompleteInventoryCountDto(items, note.nullIfBlank()))) {
                is RepositoryResult.Success -> updateSelectedBox(result.value, "Conteo físico guardado. Las diferencias quedaron en el historial.")
                is RepositoryResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
            }
        }
    }

    fun scanCode(rawValue: String) {
        val value = rawValue.trim()
        if (value.isEmpty()) return

        val token = NfcInventoryReader.tokenFromInventoryUrl(value)
        if (token != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null, barcodeMatches = emptyList()) }
                when (val result = repository.getBoxByToken(token)) {
                    is RepositoryResult.Success -> updateSelectedBox(result.value, "Caja abierta desde su código QR.")
                    is RepositoryResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, barcodeMatches = emptyList()) }
            when (val result = repository.getItemsByBarcode(value)) {
                is RepositoryResult.Success -> when {
                    result.value.isEmpty() -> _uiState.update {
                        it.copy(isLoading = false, error = "No encontramos existencias para el código $value.")
                    }
                    result.value.size == 1 -> {
                        val match = result.value.first()
                        when (val box = repository.getBox(match.boxId)) {
                            is RepositoryResult.Success -> updateSelectedBox(box.value, "Código ${match.scannableCode}: ${match.itemName} está en ${match.boxCode}.")
                            is RepositoryResult.Error -> _uiState.update { it.copy(isLoading = false, error = box.message) }
                        }
                    }
                    else -> _uiState.update {
                        it.copy(isLoading = false, barcodeMatches = result.value, notice = "El código está en varias cajas. Elige cuál abrir.")
                    }
                }
                is RepositoryResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun openBarcodeMatch(match: InventoryBarcodeMatchDto) {
        _uiState.update { it.copy(barcodeMatches = emptyList(), notice = null) }
        selectBox(match.boxId)
    }

    fun dismissBarcodeMatches() {
        _uiState.update { it.copy(barcodeMatches = emptyList()) }
    }

    fun prepareBoxLabel() {
        val box = _uiState.value.selectedBox ?: return
        prepareLabel(LabelTargetKind.InventoryBox, box.id, "Caja ${box.code}")
    }

    fun prepareItemLabel(item: InventoryItemDto) {
        prepareLabel(LabelTargetKind.InventoryItem, item.id, item.name)
    }

    fun dismissLabelPreview() {
        _uiState.update { it.copy(labelPreview = null) }
    }

    fun reportLabelOutputError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun registerLabelOutput(method: LabelPrintMethod) {
        val preview = _uiState.value.labelPreview ?: return
        viewModelScope.launch {
            when (val result = labelPrintCoordinator.register(preview, method)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(notice = "Etiqueta enviada a impresión. Se registró la versión ${preview.context.template.versionNumber}.")
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(error = "La etiqueta se preparó, pero no pudimos registrar la impresión: ${result.message}")
                }
            }
        }
    }

    fun prepareNfcWrite() {
        val box = _uiState.value.selectedBox ?: return
        _uiState.update {
            it.copy(
                waitingToWriteNfc = true,
                notice = "Acerca una etiqueta vacía a tu teléfono para vincular la caja ${box.code}.",
                error = null
            )
        }
    }

    fun onTagDiscovered(tag: Tag) {
        if (_uiState.value.isProcessingNfc) return
        viewModelScope.launch {
            if (_uiState.value.waitingToWriteNfc) {
                writeCurrentBoxToTag(tag)
            } else {
                readBoxFromTag(tag)
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(notice = null, error = null) }
    }

    private fun prepareLabel(kind: LabelTargetKind, targetId: String, title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingLabel = true, error = null, notice = null, labelPreview = null) }
            when (val result = labelPrintCoordinator.prepare(kind, targetId, title)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(
                        isPreparingLabel = false,
                        labelPreview = result.value,
                        notice = "Vista previa lista. Revisa la medida antes de enviarla a imprimir."
                    )
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isPreparingLabel = false, error = result.message)
                }
            }
        }
    }

    private suspend fun readBoxFromTag(tag: Tag) {
        _uiState.update { it.copy(isProcessingNfc = true, error = null, notice = "Leyendo etiqueta…") }
        val url = NfcInventoryReader.readUrl(tag)
        val token = url?.let(NfcInventoryReader::tokenFromInventoryUrl)
        if (token == null) {
            _uiState.update { it.copy(isProcessingNfc = false, error = "Esta etiqueta no es una caja de Regi Bazar.") }
            return
        }

        when (val result = repository.getBoxByToken(token)) {
            is RepositoryResult.Success -> updateSelectedBox(result.value, "Caja leída correctamente.")
            is RepositoryResult.Error -> _uiState.update { it.copy(isProcessingNfc = false, error = result.message) }
        }
    }

    private suspend fun writeCurrentBoxToTag(tag: Tag) {
        val box = _uiState.value.selectedBox ?: return
        _uiState.update { it.copy(isProcessingNfc = true, error = null) }
        when (val result = NfcInventoryReader.writeUrl(tag, box.nfcUrl)) {
            NfcWriteResult.Success -> {
                when (val bind = repository.bindNfc(box.id, NfcInventoryReader.tagUid(tag))) {
                    is RepositoryResult.Success -> updateSelectedBox(bind.value, "Etiqueta lista para ${box.code}. Ya puedes pegarla en su caja.")
                    is RepositoryResult.Error -> _uiState.update {
                        it.copy(
                            isProcessingNfc = false,
                            waitingToWriteNfc = false,
                            error = "La etiqueta se escribió, pero no pudimos confirmarla: ${bind.message}"
                        )
                    }
                }
            }
            is NfcWriteResult.Error -> _uiState.update {
                it.copy(isProcessingNfc = false, waitingToWriteNfc = false, error = result.message)
            }
        }
    }

    private fun updateSelectedBox(box: InventoryBoxDto, notice: String?) {
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                isProcessingNfc = false,
                isSaving = false,
                waitingToWriteNfc = false,
                selectedBox = box,
                boxes = upsertSummary(current.boxes, box),
                barcodeMatches = emptyList(),
                notice = notice,
                error = null
            )
        }
    }

    private fun upsertSummary(current: List<InventoryBoxSummaryDto>, box: InventoryBoxDto): List<InventoryBoxSummaryDto> {
        val summary = InventoryBoxSummaryDto(
            id = box.id,
            code = box.code,
            name = box.name,
            location = box.location,
            isNfcBound = box.isNfcBound,
            articleTypesCount = box.items.count { it.quantity > 0 },
            totalUnits = box.items.sumOf { it.quantity },
            updatedAt = box.updatedAt
        )
        return (current.filterNot { it.id == box.id } + summary).sortedBy { it.code }
    }
}

private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf(String::isNotEmpty)
