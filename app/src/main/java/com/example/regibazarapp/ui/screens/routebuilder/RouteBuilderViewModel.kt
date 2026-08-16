package com.example.regibazarapp.ui.screens.routebuilder

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.regibazarapp.data.models.AvailableTandaDto
import com.example.regibazarapp.data.models.ClientDto
import com.example.regibazarapp.data.models.OrderSummaryDto
import com.example.regibazarapp.data.models.PreviewRouteResponse
import com.example.regibazarapp.data.models.RoutePackageRequirementDto
import com.example.regibazarapp.data.models.SetClientCoordinatesRequest
import com.example.regibazarapp.data.repository.AdminRoutesRepository
import com.example.regibazarapp.data.repository.RepositoryResult
import com.example.regibazarapp.data.AdminLocationProvider
import com.example.regibazarapp.data.SyncManager
import com.example.regibazarapp.ui.utils.eligibleRouteOrders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class RouteBuilderUiState(
    val isLoading: Boolean = true,
    val isPreviewing: Boolean = false,
    val isSaving: Boolean = false,
    val isSavingPackages: Boolean = false,
    val isGeocoding: Boolean = false,
    val isApplyingAi: Boolean = false,
    val editingRouteId: Int? = null,
    val orders: List<OrderSummaryDto> = emptyList(),
    val tandas: List<AvailableTandaDto> = emptyList(),
    val selectedOrderIds: Set<Int> = emptySet(),
    val selectedTandaIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val aiCommand: String = "",
    val showTandas: Boolean = false,
    val preview: PreviewRouteResponse? = null,
    val error: String? = null,
    val packageRequirements: List<RoutePackageRequirementDto> = emptyList(),
    val message: String? = null,
    val savedRouteId: Int? = null
) {
    val selectedCount: Int
        get() = selectedOrderIds.size + selectedTandaIds.size

    val selectedWithoutCoordinates: Int
        get() = (
            orders.count { it.id in selectedOrderIds && !it.hasCoordinates }
                + tandas.count { it.tandaParticipantId in selectedTandaIds && !it.hasCoordinates }
            )
}

class RouteBuilderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminRoutesRepository()
    private val locationProvider = AdminLocationProvider(application)
    private val _uiState = MutableStateFlow(RouteBuilderUiState())
    val uiState: StateFlow<RouteBuilderUiState> = _uiState.asStateFlow()
    private var initializedRouteId: Int? = Int.MIN_VALUE

    fun initialize(routeId: Int?) {
        if (initializedRouteId == routeId) return
        initializedRouteId = routeId

        viewModelScope.launch {
            _uiState.update {
                RouteBuilderUiState(isLoading = true, editingRouteId = routeId)
            }

            coroutineScope {
                val ordersDeferred = async { repository.getOrders() }
                val tandasDeferred = async { repository.getAvailableTandas() }
                val clientsDeferred = async { repository.getClients() }
                val routeDeferred = routeId?.let { id ->
                    async { repository.getRoute(id) }
                }

                val ordersResult = ordersDeferred.await()
                val tandasResult = tandasDeferred.await()
                val clientsResult = clientsDeferred.await()
                val routeResult = routeDeferred?.await()

                val availableTandas = when (tandasResult) {
                    is RepositoryResult.Success -> tandasResult.value
                    is RepositoryResult.Error -> emptyList()
                }
                val clients = when (clientsResult) {
                    is RepositoryResult.Success -> clientsResult.value
                    is RepositoryResult.Error -> emptyList()
                }

                val route = when (routeResult) {
                    null -> null
                    is RepositoryResult.Success -> routeResult.value
                    is RepositoryResult.Error -> {
                        showLoadError(routeResult.message)
                        return@coroutineScope
                    }
                }

                val closedOrderIds = route?.deliveries
                    ?.filter { delivery ->
                        delivery.kind == "Order"
                            && (delivery.status == "Delivered" || delivery.status == "NotDelivered")
                    }
                    ?.mapNotNull { it.orderId }
                    ?.toSet()
                    .orEmpty()

                val orders = when (ordersResult) {
                    is RepositoryResult.Success -> eligibleRouteOrders(ordersResult.value, routeId)
                        .filter { order -> order.id !in closedOrderIds }
                    is RepositoryResult.Error -> {
                        showLoadError(ordersResult.message)
                        return@coroutineScope
                    }
                }

                val pendingRouteDeliveries = route?.deliveries
                    ?.filter { delivery ->
                        delivery.status != "Delivered" && delivery.status != "NotDelivered"
                    }
                    .orEmpty()

                val currentOrderIds = pendingRouteDeliveries
                    .mapNotNull { it.orderId }
                    .toSet()

                val currentTandas = pendingRouteDeliveries
                    .filter { it.kind == "Tanda" && it.tandaParticipantId != null }
                    .map { delivery ->
                        val client = findClientForDelivery(
                            clients = clients,
                            clientName = delivery.clientName,
                            clientPhone = delivery.clientPhone
                        )
                        AvailableTandaDto(
                            tandaParticipantId = delivery.tandaParticipantId.orEmpty(),
                            tandaId = delivery.tandaId.orEmpty(),
                            tandaName = delivery.tandaName ?: "Tanda",
                            tandaProductName = delivery.tandaProductName,
                            week = delivery.tandaWeek ?: 1,
                            totalWeeks = delivery.tandaTotalWeeks ?: 1,
                            variant = delivery.tandaVariant,
                            clientId = client?.id ?: 0,
                            clientName = delivery.clientName,
                            clientAddress = delivery.alternativeAddress
                                ?: delivery.clientAddress
                                ?: client?.address,
                            clientPhone = delivery.clientPhone,
                            clientLatitude = delivery.latitude,
                            clientLongitude = delivery.longitude,
                            deliveryInstructions = delivery.deliveryInstructions
                                ?: client?.deliveryInstructions
                        )
                    }

                val mergedTandas = (currentTandas + availableTandas)
                    .distinctBy { it.tandaParticipantId }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        orders = orders,
                        tandas = mergedTandas,
                        selectedOrderIds = currentOrderIds,
                        selectedTandaIds = currentTandas
                            .map { tanda -> tanda.tandaParticipantId }
                            .toSet(),
                        error = null
                    )
                }
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query, preview = null) }
    }

    fun updateAiCommand(command: String) {
        _uiState.update { it.copy(aiCommand = command) }
    }

    fun showTandas(show: Boolean) {
        _uiState.update { it.copy(showTandas = show, searchQuery = "", preview = null) }
    }

    fun toggleOrder(orderId: Int) {
        _uiState.update { state ->
            val selected = state.selectedOrderIds.toMutableSet()
            if (!selected.add(orderId)) selected.remove(orderId)
            state.copy(selectedOrderIds = selected, preview = null)
        }
    }

    fun toggleTanda(participantId: String) {
        _uiState.update { state ->
            val selected = state.selectedTandaIds.toMutableSet()
            if (!selected.add(participantId)) selected.remove(participantId)
            state.copy(selectedTandaIds = selected, preview = null)
        }
    }

    fun selectAllOrdersWithCoordinates() {
        _uiState.update { state ->
            state.copy(
                selectedOrderIds = state.orders
                    .filter { it.hasCoordinates }
                    .map { it.id }
                    .toSet(),
                preview = null
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedOrderIds = emptySet(),
                selectedTandaIds = emptySet(),
                preview = null
            )
        }
    }

    fun previewRoute() {
        val state = _uiState.value
        if (state.selectedCount == 0 || state.isPreviewing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPreviewing = true, error = null, message = null) }
            val location = locationProvider.getLastKnownLocation()
            when (
                val result = repository.previewRoute(
                    state.selectedOrderIds.toList(),
                    state.selectedTandaIds.toList(),
                    latitude = location?.latitude,
                    longitude = location?.longitude
                )
            ) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isPreviewing = false, preview = result.value)
                }

                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isPreviewing = false, error = result.message)
                }
            }
        }
    }

    fun closePreview() {
        _uiState.update { it.copy(preview = null) }
    }

    fun applyCamiSelection(command: String = _uiState.value.aiCommand) {
        val normalizedCommand = command.trim()
        val state = _uiState.value
        if (normalizedCommand.isBlank() || state.isApplyingAi) return
        if (state.orders.isEmpty()) {
            _uiState.update { it.copy(error = "No hay pedidos disponibles para analizar.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isApplyingAi = true,
                    aiCommand = normalizedCommand,
                    error = null,
                    message = null
                )
            }
            when (val result = repository.selectRouteWithCami(normalizedCommand, state.orders)) {
                is RepositoryResult.Success -> {
                    val validIds = result.value.selectedOrderIds
                        .filterTo(mutableSetOf()) { selectedId ->
                            state.orders.any { order -> order.id == selectedId }
                        }
                    _uiState.update {
                        it.copy(
                            isApplyingAi = false,
                            selectedOrderIds = validIds,
                            showTandas = false,
                            preview = null,
                            message = result.value.aiConfirmationMessage.ifBlank {
                                if (validIds.isEmpty()) {
                                    "C.A.M.I. no encontró pedidos que coincidan."
                                } else {
                                    "C.A.M.I. seleccionó ${validIds.size} pedidos."
                                }
                            }
                        )
                    }
                }

                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isApplyingAi = false, error = result.message)
                }
            }
        }
    }

    fun bulkGeocodeSelected() {
        val state = _uiState.value
        val clientIds = buildList {
            state.orders
                .filter { it.id in state.selectedOrderIds && !it.hasCoordinates }
                .mapNotNullTo(this) { it.clientId }
            state.tandas
                .filter { it.tandaParticipantId in state.selectedTandaIds && !it.hasCoordinates }
                .mapTo(this) { it.clientId }
        }.distinct()

        if (clientIds.isEmpty() || state.isGeocoding) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGeocoding = true, error = null, message = null) }
            when (val result = repository.bulkGeocode(clientIds)) {
                is RepositoryResult.Success -> {
                    val byClient = result.value.associateBy { it.clientId }
                    val successes = result.value.count { it.success }
                    val failures = result.value.size - successes

                    _uiState.update { current ->
                        current.copy(
                            isGeocoding = false,
                            orders = current.orders.map { order ->
                                val geocode = order.clientId?.let(byClient::get)
                                if (geocode?.success == true) {
                                    order.copy(
                                        clientAddress = geocode.formattedAddress ?: order.clientAddress,
                                        clientLatitude = geocode.latitude,
                                        clientLongitude = geocode.longitude
                                    )
                                } else {
                                    order
                                }
                            },
                            tandas = current.tandas.map { tanda ->
                                val geocode = byClient[tanda.clientId]
                                if (geocode?.success == true) {
                                    tanda.copy(
                                        clientAddress = geocode.formattedAddress ?: tanda.clientAddress,
                                        clientLatitude = geocode.latitude,
                                        clientLongitude = geocode.longitude
                                    )
                                } else {
                                    tanda
                                }
                            },
                            message = "$successes ubicaciones resueltas"
                                + if (failures > 0) ", $failures requieren captura manual" else "",
                            preview = null
                        )
                    }
                }

                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isGeocoding = false, error = result.message)
                }
            }
        }
    }

    fun saveAddress(orderId: Int, address: String, instructions: String) {
        val order = _uiState.value.orders.firstOrNull { it.id == orderId } ?: return
        val clientId = order.clientId
        if (clientId == null) {
            _uiState.update { it.copy(error = "Este pedido no tiene una clienta vinculada.") }
            return
        }
        if (address.isBlank()) {
            _uiState.update { it.copy(error = "Captura una dirección antes de guardarla.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGeocoding = true, error = null, message = null) }
            val coordinates = geocodeAddress(address)
            if (coordinates == null) {
                _uiState.update {
                    it.copy(
                        isGeocoding = false,
                        error = "No pude ubicar esa dirección. Agrega calle, número y colonia."
                    )
                }
                return@launch
            }

            val request = SetClientCoordinatesRequest(
                latitude = coordinates.first,
                longitude = coordinates.second,
                address = address.trim(),
                deliveryInstructions = instructions.trim()
            )

            when (val result = repository.setClientCoordinates(clientId, request)) {
                is RepositoryResult.Success -> _uiState.update { state ->
                    state.copy(
                        isGeocoding = false,
                        orders = state.orders.map { current ->
                            if (current.id == orderId) {
                                current.copy(
                                    clientAddress = address.trim(),
                                    clientLatitude = coordinates.first,
                                    clientLongitude = coordinates.second,
                                    deliveryInstructions = instructions.trim()
                                )
                            } else {
                                current
                            }
                        },
                        message = "Dirección guardada para ${order.clientName}",
                        preview = null
                    )
                }

                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isGeocoding = false, error = result.message)
                }
            }
        }
    }

    fun saveTandaAddress(participantId: String, address: String, instructions: String) {
        val tanda = _uiState.value.tandas
            .firstOrNull { it.tandaParticipantId == participantId }
            ?: return
        if (tanda.clientId <= 0) {
            _uiState.update {
                it.copy(error = "Esta participante no tiene una clienta vinculada para guardar la ubicación.")
            }
            return
        }
        if (address.isBlank()) {
            _uiState.update { it.copy(error = "Captura una dirección antes de guardarla.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGeocoding = true, error = null, message = null) }
            val coordinates = geocodeAddress(address)
            if (coordinates == null) {
                _uiState.update {
                    it.copy(
                        isGeocoding = false,
                        error = "No pude ubicar esa dirección. Agrega calle, número y colonia."
                    )
                }
                return@launch
            }

            val request = SetClientCoordinatesRequest(
                latitude = coordinates.first,
                longitude = coordinates.second,
                address = address.trim(),
                deliveryInstructions = instructions.trim()
            )

            when (val result = repository.setClientCoordinates(tanda.clientId, request)) {
                is RepositoryResult.Success -> _uiState.update { state ->
                    state.copy(
                        isGeocoding = false,
                        tandas = state.tandas.map { current ->
                            if (current.tandaParticipantId == participantId) {
                                current.copy(
                                    clientAddress = address.trim(),
                                    clientLatitude = coordinates.first,
                                    clientLongitude = coordinates.second,
                                    deliveryInstructions = instructions.trim()
                                )
                            } else {
                                current
                            }
                        },
                        message = "Dirección guardada para ${tanda.clientName}",
                        preview = null
                    )
                }

                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isGeocoding = false, error = result.message)
                }
            }
        }
    }

    fun saveRoute() {
        val state = _uiState.value
        if (state.selectedCount == 0 || state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, message = null) }
            val result = state.editingRouteId?.let { routeId ->
                repository.recomposeRoute(
                    routeId,
                    state.selectedOrderIds.toList(),
                    state.selectedTandaIds.toList()
                )
            }

            if (result != null) {
                when (result) {
                    is RepositoryResult.Success -> {
                        SyncManager.triggerRefresh()
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                savedRouteId = result.value.route.id,
                                message = "Ruta actualizada y optimizada"
                            )
                        }
                    }

                    is RepositoryResult.Error -> _uiState.update {
                        it.copy(isSaving = false, error = result.message)
                    }
                }
            } else {
                when (
                    val createResult = repository.createRoute(
                        state.selectedOrderIds.toList(),
                        state.selectedTandaIds.toList()
                    )
                ) {
                    is RepositoryResult.Success -> {
                        SyncManager.triggerRefresh()
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                savedRouteId = createResult.value.route.id,
                                message = "Ruta creada y optimizada"
                            )
                        }
                    }

                    is RepositoryResult.Error -> _uiState.update {
                        if (createResult.code == "PACKAGES_REQUIRED" && createResult.packageRequirements.isNotEmpty()) {
                            it.copy(
                                isSaving = false,
                                packageRequirements = createResult.packageRequirements
                            )
                        } else {
                            it.copy(isSaving = false, error = createResult.message)
                        }
                    }
                }
            }
        }
    }

    fun dismissPackageRequirements() {
        _uiState.update { it.copy(packageRequirements = emptyList()) }
    }

    fun confirmPackageRequirements(counts: Map<Int, Int>) {
        val requirements = _uiState.value.packageRequirements
        if (requirements.isEmpty() || _uiState.value.isSavingPackages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPackages = true, error = null) }
            for (requirement in requirements) {
                val count = counts[requirement.id]
                if (count == null || count < 0) {
                    _uiState.update {
                        it.copy(
                            isSavingPackages = false,
                            error = "Indica las bolsas de todos los pedidos antes de continuar."
                        )
                    }
                    return@launch
                }

                when (val result = repository.setPackages(requirement.id, count)) {
                    is RepositoryResult.Success -> Unit
                    is RepositoryResult.Error -> {
                        _uiState.update {
                            it.copy(isSavingPackages = false, error = result.message)
                        }
                        return@launch
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isSavingPackages = false,
                    packageRequirements = emptyList(),
                    message = "Bolsas confirmadas. Ya puedes crear la ruta."
                )
            }
            saveRoute()
        }
    }

    fun consumeSavedRoute() {
        _uiState.update { it.copy(savedRouteId = null) }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, message = null) }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocodeAddress(address: String): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val fullAddress = if (
                    address.contains("nuevo laredo", ignoreCase = true)
                    || address.contains("tamaulipas", ignoreCase = true)
                ) {
                    address
                } else {
                    "$address, Nuevo Laredo, Tamaulipas, México"
                }
                val result = Geocoder(getApplication(), Locale("es", "MX"))
                    .getFromLocationName(fullAddress, 1)
                    ?.firstOrNull()
                result?.let { it.latitude to it.longitude }
            }.getOrNull()
        }

    private fun showLoadError(message: String) {
        _uiState.update { it.copy(isLoading = false, error = message) }
    }

    private fun findClientForDelivery(
        clients: List<ClientDto>,
        clientName: String,
        clientPhone: String?
    ): ClientDto? {
        val phoneDigits = clientPhone
            ?.filter(Char::isDigit)
            ?.takeLast(10)
            ?.takeIf { it.isNotBlank() }

        return clients.firstOrNull { client ->
            phoneDigits != null && client.phone
                ?.filter(Char::isDigit)
                ?.takeLast(10) == phoneDigits
        } ?: clients.firstOrNull { client ->
            client.name.trim().equals(clientName.trim(), ignoreCase = true)
        }
    }
}
