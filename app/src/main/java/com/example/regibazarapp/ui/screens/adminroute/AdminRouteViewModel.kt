package com.example.regibazarapp.ui.screens.adminroute

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.data.models.LabelPrintMethod
import com.example.regibazarapp.data.models.LabelPrinterProfile
import com.example.regibazarapp.data.models.LabelTargetKind
import com.example.regibazarapp.data.models.RouteListDto
import com.example.regibazarapp.data.repository.AdminRoutesRepository
import com.example.regibazarapp.data.repository.RepositoryResult
import com.example.regibazarapp.labelprint.DirectBluetoothLabelPrinter
import com.example.regibazarapp.labelprint.DirectPrinterResult
import com.example.regibazarapp.labelprint.DirectPrinterSettings
import com.example.regibazarapp.labelprint.DirectPrinterSettingsStore
import com.example.regibazarapp.labelprint.PrinterScope
import com.example.regibazarapp.labelprint.LabelPrintCoordinator
import com.example.regibazarapp.labelprint.PackageLabelPrintQueue
import com.example.regibazarapp.labelprint.PackageLabelPrintQueueItem
import com.example.regibazarapp.labelprint.PackageLabelPrintStatus
import com.example.regibazarapp.labelprint.PairedBluetoothPrinter
import com.example.regibazarapp.labelprint.RenderedLabelPreview
import com.example.regibazarapp.data.AdminLocationProvider
import com.example.regibazarapp.data.SyncManager
import com.example.regibazarapp.data.local.RouteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdminRouteUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isWorking: Boolean = false,
    val isPreparingLabel: Boolean = false,
    val isLoadingPairedPrinters: Boolean = false,
    val isDirectPrintRunning: Boolean = false,
    val route: RouteListDto? = null,
    val labelPreview: RenderedLabelPreview? = null,
    val directPrinterSettings: DirectPrinterSettings = DirectPrinterSettings(),
    val pairedPrinters: List<PairedBluetoothPrinter> = emptyList(),
    val directPrintQueue: PackageLabelPrintQueue? = null,
    val error: String? = null,
    val message: String? = null,
    val notice: String? = null,
    val routeDeleted: Boolean = false
)

class AdminRouteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminRoutesRepository()
    private val labelPrintCoordinator = LabelPrintCoordinator()
    private val directBluetoothPrinter = DirectBluetoothLabelPrinter(application)
    private val directPrinterSettingsStore = DirectPrinterSettingsStore(application, PrinterScope.PACKAGE)
    private val locationProvider = AdminLocationProvider(application)
    private val routeDao = RouteDatabase.getInstance(application).routeDao()
    private val gson = Gson()
    private val _uiState = MutableStateFlow(AdminRouteUiState())
    val uiState: StateFlow<AdminRouteUiState> = _uiState.asStateFlow()
    private var routeId: Int? = null

    init {
        viewModelScope.launch {
            directPrinterSettingsStore.settings.collectLatest { settings ->
                _uiState.update { current -> current.copy(directPrinterSettings = settings) }
            }
        }
    }

    fun initialize(id: Int) {
        if (routeId == id && _uiState.value.route != null) return
        routeId = id
        loadRoute()
    }

    fun loadRoute(showLoading: Boolean = true) {
        val id = routeId ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = showLoading && it.route == null,
                    isRefreshing = it.route != null,
                    error = null,
                    notice = null
                )
            }
            when (val result = repository.getRoute(id)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        route = result.value,
                        error = null,
                        notice = null
                    )
                }

                is RepositoryResult.Error -> loadCachedRoute(id, result.message)
            }
        }
    }

    fun preparePackageLabel(packageId: String, clientName: String, packageNumber: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingLabel = true, error = null, labelPreview = null) }
            when (val result = labelPrintCoordinator.prepare(
                LabelTargetKind.OrderPackage,
                packageId,
                "Bolsa ${packageNumber} · $clientName"
            )) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isPreparingLabel = false, labelPreview = result.value, message = "Vista previa de bolsa lista.")
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isPreparingLabel = false, error = result.message)
                }
            }
        }
    }

    /** Imprime una bolsa directamente si ya existe una E40 configurada; si no, conserva la vista previa. */
    fun printPackage(packageId: String, clientName: String, packageNumber: Int) {
        if (_uiState.value.directPrinterSettings.isConfigured) {
            startDirectPrinting(
                listOf(
                    PackageLabelPrintQueueItem(
                        packageId = packageId,
                        packageNumber = packageNumber,
                        clientName = clientName
                    )
                )
            )
        } else {
            preparePackageLabel(packageId, clientName, packageNumber)
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
            _uiState.update { it.copy(message = "${printer.name} quedó lista para imprimir etiquetas de bolsas.") }
        }
    }

    fun clearDirectPrinter() {
        viewModelScope.launch {
            directPrinterSettingsStore.clear()
            _uiState.update { it.copy(message = "La impresora directa se desconectó de esta app.") }
        }
    }

    fun printAllRoutePackages() {
        val packages = _uiState.value.route
            ?.deliveries
            .orEmpty()
            .flatMap { delivery ->
                delivery.packages.orEmpty().map { packageInfo ->
                    PackageLabelPrintQueueItem(
                        packageId = packageInfo.id,
                        packageNumber = packageInfo.packageNumber,
                        clientName = delivery.clientName
                    )
                }
            }
            .distinctBy(PackageLabelPrintQueueItem::packageId)
            .sortedWith(compareBy(PackageLabelPrintQueueItem::clientName, PackageLabelPrintQueueItem::packageNumber))

        if (packages.isEmpty()) {
            _uiState.update { it.copy(error = "No hay bolsas generadas en esta ruta para imprimir.") }
            return
        }
        startDirectPrinting(packages)
    }

    fun retryFailedDirectPrints() {
        val queue = _uiState.value.directPrintQueue ?: return
        val retryItems = queue.items.filter { it.status == PackageLabelPrintStatus.FAILED }
        if (retryItems.isEmpty()) return

        startDirectPrinting(
            retryItems,
            initialQueue = queue.copy(
                items = queue.items.map { item ->
                    if (item.status == PackageLabelPrintStatus.FAILED) {
                        item.copy(status = PackageLabelPrintStatus.PENDING, detail = null)
                    } else {
                        item
                    }
                }
            )
        )
    }

    fun dismissLabelPreview() {
        _uiState.update { it.copy(labelPreview = null) }
    }

    fun registerLabelOutput(method: LabelPrintMethod) {
        val preview = _uiState.value.labelPreview ?: return
        viewModelScope.launch {
            when (val result = labelPrintCoordinator.register(preview, method)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(message = "Etiqueta de bolsa enviada a impresión.")
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(error = "La etiqueta se envió, pero no pudimos registrarla: ${result.message}")
                }
            }
        }
    }

    fun reportLabelOutputError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    private fun startDirectPrinting(
        items: List<PackageLabelPrintQueueItem>,
        initialQueue: PackageLabelPrintQueue = PackageLabelPrintQueue(items)
    ) {
        if (_uiState.value.isDirectPrintRunning) return
        val printerAddress = _uiState.value.directPrinterSettings.address
        if (printerAddress.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Primero configura la AIYIN E40 Pro para imprimir directo.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDirectPrintRunning = true,
                    directPrintQueue = initialQueue,
                    error = null,
                    message = null
                )
            }

            items.forEach { item ->
                printQueueItem(item, printerAddress)
            }

            val queue = _uiState.value.directPrintQueue
            val sent = queue?.sentCount ?: 0
            val failed = queue?.failedCount ?: 0
            _uiState.update {
                it.copy(
                    isDirectPrintRunning = false,
                    message = when {
                        failed == 0 -> "$sent etiqueta(s) enviada(s) a la impresora."
                        sent == 0 -> "No pudimos enviar las etiquetas. Revisa la cola e intenta de nuevo."
                        else -> "$sent etiqueta(s) enviada(s) y $failed pendiente(s) para reintentar."
                    }
                )
            }
        }
    }

    private suspend fun printQueueItem(item: PackageLabelPrintQueueItem, printerAddress: String) {
        updateQueueItem(item.packageId, PackageLabelPrintStatus.PREPARING, "Preparando diseño publicado…")
        val prepared = labelPrintCoordinator.prepare(
            LabelTargetKind.OrderPackage,
            item.packageId,
            "Bolsa ${item.packageNumber} · ${item.clientName}"
        )
        val preview = when (prepared) {
            is RepositoryResult.Success -> prepared.value
            is RepositoryResult.Error -> {
                updateQueueItem(item.packageId, PackageLabelPrintStatus.FAILED, prepared.message)
                return
            }
        }

        if (preview.profile != LabelPrinterProfile.AiyinE40_4x6) {
            updateQueueItem(
                item.packageId,
                PackageLabelPrintStatus.FAILED,
                "La plantilla publicada no usa el perfil AIYIN E40 Pro de 4 × 6 in."
            )
            return
        }

        updateQueueItem(item.packageId, PackageLabelPrintStatus.SENDING, "Enviando por Bluetooth…")
        when (val result = directBluetoothPrinter.printE40(preview.bitmap, printerAddress)) {
            is DirectPrinterResult.Success -> {
                val audit = labelPrintCoordinator.register(preview, LabelPrintMethod.NativeBluetooth)
                val detail = when (audit) {
                    is RepositoryResult.Success -> "Enviada a ${result.value.printerName}"
                    is RepositoryResult.Error -> "Enviada a ${result.value.printerName}; no se pudo registrar en bitácora."
                }
                updateQueueItem(item.packageId, PackageLabelPrintStatus.SENT, detail)
            }
            is DirectPrinterResult.Error -> {
                updateQueueItem(item.packageId, PackageLabelPrintStatus.FAILED, result.message)
            }
        }
    }

    private fun updateQueueItem(
        packageId: String,
        status: PackageLabelPrintStatus,
        detail: String? = null
    ) {
        _uiState.update { current ->
            current.copy(
                directPrintQueue = current.directPrintQueue?.update(packageId, status, detail)
            )
        }
    }

    private suspend fun loadCachedRoute(id: Int, networkError: String) {
        val cachedRoute = withContext(Dispatchers.IO) {
            val cached = routeDao.getAdminRoutes() ?: return@withContext null
            val listType = object : TypeToken<List<RouteListDto>>() {}.type
            runCatching {
                gson.fromJson<List<RouteListDto>>(cached.routesJson, listType)
            }.getOrNull()?.firstOrNull { it.id == id }
        }

        if (cachedRoute == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = networkError
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                route = cachedRoute,
                error = null,
                notice = "Sin conexión: mostrando la última versión guardada"
            )
        }
    }

    fun optimize() {
        val id = routeId ?: return
        if (_uiState.value.isWorking) return

        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null, message = null) }
            val location = locationProvider.getLastKnownLocation()
            when (
                val result = repository.optimizeRoute(
                    id,
                    latitude = location?.latitude,
                    longitude = location?.longitude
                )
            ) {
                is RepositoryResult.Success -> {
                    SyncManager.triggerRefresh()
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            message = if (location == null) {
                                "Ruta optimizada desde el punto configurado"
                            } else {
                                "Ruta optimizada desde tu ubicación"
                            }
                        )
                    }
                    loadRoute(showLoading = false)
                }

                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isWorking = false, error = result.message)
                }
            }
        }
    }

    fun moveDelivery(deliveryId: Int, direction: Int) {
        val route = _uiState.value.route ?: return
        val deliveries = route.deliveries.toMutableList()
        val currentIndex = deliveries.indexOfFirst { it.deliveryId == deliveryId }
        val targetIndex = currentIndex + direction
        if (currentIndex < 0 || targetIndex !in deliveries.indices) return

        val moved = deliveries.removeAt(currentIndex)
        deliveries.add(targetIndex, moved)
        val reordered = deliveries.mapIndexed { index, delivery ->
            delivery.copy(sortOrder = index + 1)
        }

        _uiState.update { it.copy(route = route.copy(deliveries = reordered), isWorking = true) }
        viewModelScope.launch {
            when (
                val result = repository.reorderRoute(
                    route.id,
                    reordered.map(RouteDeliveryDto::deliveryId)
                )
            ) {
                is RepositoryResult.Success -> {
                    SyncManager.triggerRefresh()
                    _uiState.update {
                        it.copy(isWorking = false, message = "Orden actualizado")
                    }
                }

                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            route = route,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun removeDelivery(delivery: RouteDeliveryDto) {
        val id = routeId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null, message = null) }
            val result = if (delivery.kind == "Tanda" && delivery.tandaParticipantId != null) {
                repository.removeTanda(id, delivery.tandaParticipantId)
            } else {
                val orderId = delivery.orderId
                if (orderId == null) {
                    RepositoryResult.Error("Esta entrega no tiene pedido asociado.")
                } else {
                    repository.removeOrder(id, orderId)
                }
            }

            when (result) {
                is RepositoryResult.Success -> {
                    SyncManager.triggerRefresh()
                    _uiState.update { it.copy(isWorking = false, message = "Parada retirada") }
                    loadRoute(showLoading = false)
                }

                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isWorking = false, error = result.message)
                }
            }
        }
    }

    fun liquidate() = performMutation(
        successMessage = "Ruta liquidada correctamente"
    ) { id ->
        repository.liquidateRoute(id)
    }

    fun deleteRoute() {
        val id = routeId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }
            when (val result = repository.deleteRoute(id)) {
                is RepositoryResult.Success -> {
                    SyncManager.triggerRefresh()
                    _uiState.update {
                        it.copy(isWorking = false, routeDeleted = true)
                    }
                }

                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isWorking = false, error = result.message)
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, message = null) }
    }

    private fun performMutation(
        successMessage: String,
        block: suspend (Int) -> RepositoryResult<Unit>
    ) {
        val id = routeId ?: return
        if (_uiState.value.isWorking) return

        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null, message = null) }
            when (val result = block(id)) {
                is RepositoryResult.Success -> {
                    SyncManager.triggerRefresh()
                    _uiState.update { it.copy(isWorking = false, message = successMessage) }
                    loadRoute(showLoading = false)
                }

                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isWorking = false, error = result.message)
                }
            }
        }
    }
}
