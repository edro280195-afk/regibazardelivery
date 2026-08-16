package com.example.regibazarapp.ui.screens.route

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.regibazarapp.data.DeliveryEvidenceStore
import com.example.regibazarapp.data.OfflineQueue
import com.example.regibazarapp.data.PendingAction
import com.example.regibazarapp.data.TokenManager
import com.example.regibazarapp.data.local.RouteDatabase
import com.example.regibazarapp.data.local.RouteEntity
import com.example.regibazarapp.data.local.WidgetDataStore
import com.example.regibazarapp.service.RegiBazarFirebaseService
import com.example.regibazarapp.service.SyncWorker
import com.example.regibazarapp.ui.widget.DeliveryWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.regibazarapp.data.models.*
import com.example.regibazarapp.data.network.NetworkModule
import com.example.regibazarapp.service.CamiManager
import com.example.regibazarapp.service.CamiState
import com.example.regibazarapp.service.LocationForegroundService
import com.example.regibazarapp.service.SignalREvent
import com.example.regibazarapp.service.SignalRManager
import com.example.regibazarapp.ui.utils.DirectionsManager
import com.example.regibazarapp.ui.utils.RouteInfo
import com.google.android.gms.maps.model.LatLng
import com.example.regibazarapp.data.NetworkMonitor
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.regibazarapp.data.SyncManager
import com.google.android.gms.location.*
import java.util.UUID

/**
 * ═══════════════════════════════════════════════════════════
 *  Route ViewModel – Cerebro del HUD del Repartidor
 *
 *  Integra: GPS service, CAMI voz, SignalR tiempo real,
 *  cola offline, polilínea, resumen de ruta completada.
 * ═══════════════════════════════════════════════════════════
 */

data class RouteUiState(
    val isLoading: Boolean = true,
    val routeData: RouteResponse? = null,
    val routeName: String = "",
    val routeStartedAt: String? = null,
    val currentDelivery: RouteDeliveryDto? = null,
    val deliveredCount: Int = 0,
    val failedCount: Int = 0,
    val totalCount: Int = 0,
    val totalCollected: Double = 0.0,
    val collectedByCash: Double = 0.0,
    val collectedByTransfer: Double = 0.0,
    val collectedByDeposit: Double = 0.0,
    val error: String? = null,
    val isRouteStarted: Boolean = false,
    val isLocationPaused: Boolean = false,
    val isRouteComplete: Boolean = false,
    // C.A.M.I.
    val camiState: CamiState = CamiState.IDLE,
    val camiTranscript: String? = null,
    val camiResponse: String? = null,
    // Foto capturada
    val capturedPhoto: Bitmap? = null,
    // Firma digital capturada
    val capturedSignature: CapturedSignature? = null,
    // SignalR
    val isSignalRConnected: Boolean = false,
    val hasNewChatMessage: Boolean = false,
    // Cola offline
    val pendingActionsCount: Int = 0,
    // Acción de entrega en vuelo
    val isDelivering: Boolean = false,
    // Llave de idempotencia — se regenera al cambiar de entrega
    val deliveryIdempotencyKey: String = UUID.randomUUID().toString(),
    // Mapa
    val polylinePoints: List<LatLng> = emptyList(),
    val routeInfo: RouteInfo? = null,
    // Proximidad
    val proximityAlert: Boolean = false,
    val proximityDeliveryName: String = "",
    val isOffline: Boolean = false,
    val sessionExpired: Boolean = false,
    // Pedido modificado por admin durante la ruta
    val orderChangedAlert: Boolean = false,
    val orderChangedClientName: String = "",
    val orderChangedNewTotal: Double = 0.0,
    val orderChangedBalanceDue: Double = 0.0,
    // Ruta eliminada por admin mientras estaba activa
    val routeDeletedByAdmin: Boolean = false,
    val routeDeletedMessage: String = "",
    // ── Métricas de clientas (tiempo en puerta) ──
    val avgDoorTimeSecs: Long? = null,
    val fastestDoorTimeSecs: Long? = null,
    val slowestDoorTimeSecs: Long? = null,
    val routeDurationMinutes: Long? = null
)

data class CapturedSignature(
    val bitmap: Bitmap,
    val svg: String,
    val signedByName: String?
)

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val offlineQueue = OfflineQueue(application)
    private val evidenceStore = DeliveryEvidenceStore(application)
    private val routeDao = RouteDatabase.getInstance(application).routeDao()
    private val widgetStore = WidgetDataStore(application)
    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    private var driverToken: String = ""
    private val signalR = SignalRManager.getInstance()
    private var camiManager: CamiManager? = null
    private var lastCamiToggleMs = 0L

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private var locationCallback: LocationCallback? = null

    // ─── INIT ───

    fun initialize(token: String) {
        if (driverToken == token) return // Ya inicializado
        driverToken = token

        // Restaurar JWT para el interceptor de NetworkModule (Cerca de 401 fix)
        viewModelScope.launch {
            tokenManager.jwtToken.collect { jwt ->
                NetworkModule.jwtToken = jwt
            }
        }

        // Re-registrar token FCM con el driverToken de la ruta activa.
        // Esto garantiza que la BD tenga el token fresco incluso si la llamada
        // en el login o en onNewToken falló (ej. API dormida en Render).
        RegiBazarFirebaseService.registerCurrentToken(getApplication(), driverRouteToken = token)

        loadRoute()
        setupSignalR()
        setupCami()
        setupSyncBus()
        processOfflineQueue()
        monitorOfflineQueue()
        monitorNetworkConnectivity()
        startPeriodicSafetyRefresh()
        // WorkManager: sincroniza cola offline cuando haya red (incluso con app en background)
        SyncWorker.schedule(getApplication(), token)
    }

    private fun setupSyncBus() {
        viewModelScope.launch {
            SyncManager.refreshFlow.collect {
                loadRoute()
            }
        }
    }

    // ─── DATA LOADING ───

    fun loadRoute() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = NetworkModule.api.getRoute(driverToken)
                if (response.isSuccessful && response.body() != null) {
                    val routeData = response.body()!!
                    // Cachear en Room para Modo Búnker
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        routeDao.upsertRoute(RouteEntity(token = driverToken, routeJson = Gson().toJson(routeData)))
                    }
                    processRouteData(routeData)
                } else {
                    if (response.code() == 401) {
                        _uiState.update { it.copy(isLoading = false, sessionExpired = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Error cargando ruta (${response.code()})") }
                    }
                }
            } catch (e: Exception) {
                // Sin red — cargar desde caché Room (Modo Búnker)
                val cached = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    routeDao.getRoute(driverToken)
                }
                if (cached != null) {
                    val routeData = Gson().fromJson(cached.routeJson, RouteResponse::class.java)
                    processRouteData(routeData)
                    _uiState.update { it.copy(error = "📡 Modo Búnker — caché de ${formatCachedTime(cached.cachedAt)}") }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Sin conexión y sin datos en caché") }
                }
            }
        }
    }

    private fun formatCachedTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "hace menos de 1 min"
            diff < 3_600_000 -> "hace ${diff / 60_000} min"
            else -> "hace ${diff / 3_600_000} h"
        }
    }

    private fun processRouteData(route: RouteResponse) {
        val orderedDeliveries = route.deliveries
            .sortedWith(compareBy<RouteDeliveryDto> { it.sortOrder }.thenBy { it.deliveryId })
        val normalizedRoute = route.copy(deliveries = orderedDeliveries)
        val deliveries = normalizedRoute.deliveries
        val delivered = deliveries.count { it.status == "Delivered" }
        val failed = deliveries.count { it.status == "NotDelivered" }
        val current = nextRouteDelivery(deliveries)

        // Calcular cobros por método de pago
        val allPayments = deliveries
            .filter { it.status == "Delivered" }
            .flatMap { it.payments ?: emptyList() }

        val totalCollected = allPayments.sumOf { it.amount }
        val byCash = allPayments.filter { it.method.contains("Efectivo", true) }.sumOf { it.amount }
        val byTransfer = allPayments.filter { it.method.contains("Transfer", true) }.sumOf { it.amount }
        val byDeposit = allPayments.filter { it.method.contains("Depósito", true) || it.method.contains("Deposito", true) }.sumOf { it.amount }

        // Determinar si ruta completada:
        // — La API puede haber cambiado el status a "Completed" antes del siguiente loadRoute()
        // — También lo detectamos localmente si todas las entregas están resueltas y la ruta está "Active"
        val isComplete = route.status == "Completed" ||
                (route.status == "Active" && deliveries.isNotEmpty() &&
                deliveries.all { it.status == "Delivered" || it.status == "NotDelivered" })

        // Regenerar llave idempotente solo cuando cambia la entrega activa
        val newIdempotencyKey = if (current?.deliveryId != _uiState.value.currentDelivery?.deliveryId)
            UUID.randomUUID().toString() else _uiState.value.deliveryIdempotencyKey

        // ── Métricas de clientas (tiempo en puerta: arrivedAt → deliveredAt) ──
        val doorTimeSecs: List<Long> = deliveries
            .filter { it.status == "Delivered" && it.arrivedAt != null && it.deliveredAt != null }
            .mapNotNull { del ->
                try {
                    val arrived = java.time.Instant.parse(del.arrivedAt!!).toEpochMilli()
                    val delivered = java.time.Instant.parse(del.deliveredAt!!).toEpochMilli()
                    val diff = (delivered - arrived) / 1000L
                    if (diff > 0) diff else null
                } catch (_: Exception) { null }
            }

        val avgDoorTimeSecs = if (doorTimeSecs.isNotEmpty()) doorTimeSecs.average().toLong() else null
        val fastestDoorTimeSecs = doorTimeSecs.minOrNull()
        val slowestDoorTimeSecs = doorTimeSecs.maxOrNull()

        // Usar el deliveredAt más tardío como hora de fin real; si la ruta no está completa, usar ahora
        val routeEndMs: Long = deliveries
            .filter { it.status == "Delivered" || it.status == "NotDelivered" }
            .mapNotNull { d -> d.deliveredAt ?: d.arrivedAt }
            .mapNotNull { ts ->
                try { java.time.Instant.parse(ts).toEpochMilli() } catch (_: Exception) { null }
            }
            .maxOrNull()
            ?.takeIf { isComplete }
            ?: System.currentTimeMillis()

        val routeDurationMinutes: Long? = route.startedAt?.let { startedAt ->
            try {
                val start = java.time.Instant.parse(startedAt).toEpochMilli()
                ((routeEndMs - start) / 60_000L).coerceAtLeast(0)
            } catch (_: Exception) { null }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                routeData = normalizedRoute,
                routeName = route.name ?: "",
                routeStartedAt = route.startedAt,
                currentDelivery = current,
                deliveredCount = delivered,
                failedCount = failed,
                totalCount = deliveries.size,
                totalCollected = totalCollected,
                collectedByCash = byCash,
                collectedByTransfer = byTransfer,
                collectedByDeposit = byDeposit,
                isRouteStarted = route.status == "Active",
                isLocationPaused = if (route.status == "Active") it.isLocationPaused else false,
                isRouteComplete = isComplete,
                deliveryIdempotencyKey = newIdempotencyKey,
                avgDoorTimeSecs = avgDoorTimeSecs,
                fastestDoorTimeSecs = fastestDoorTimeSecs,
                slowestDoorTimeSecs = slowestDoorTimeSecs,
                routeDurationMinutes = routeDurationMinutes
            )
        }

        // Si la ruta está completada, detenemos el GPS automáticamente para no gastar batería
        // ni molestar con la notificación persistente.
        if (isComplete) {
            Log.i("RouteViewModel", "✅ Ruta completada detectada. Deteniendo tracking GPS...")
            stopTracking()
        }

        // Actualizar service companion con datos de progreso y siguiente destino
        LocationForegroundService.deliveredCount = delivered
        LocationForegroundService.totalCount = deliveries.size
        if (current != null) {
            LocationForegroundService.currentDeliveryLat = current.latitude
            LocationForegroundService.currentDeliveryLng = current.longitude
            LocationForegroundService.currentDeliveryName = current.clientName
            LocationForegroundService.currentDeliveryId = current.deliveryId
            LocationForegroundService.currentDeliveryInstructions = current.deliveryInstructions
            LocationForegroundService.currentDeliveryBalance = current.balanceDue
            LocationForegroundService.currentDeliveryPhone = current.clientPhone
            LocationForegroundService.currentDeliveryPaymentMethod = current.paymentMethod
        } else {
            LocationForegroundService.currentDeliveryLat = null
            LocationForegroundService.currentDeliveryLng = null
            LocationForegroundService.currentDeliveryName = ""
            LocationForegroundService.currentDeliveryId = -1
            LocationForegroundService.currentDeliveryInstructions = null
            LocationForegroundService.currentDeliveryBalance = 0.0
            LocationForegroundService.currentDeliveryPhone = null
            LocationForegroundService.currentDeliveryPaymentMethod = null
        }

        // Fetch polilínea completa de la ruta
        fetchFullRoutePolyline()

        // Geocodificar si falta ubicación al actual
        current?.let { geocodeDeliveryAddressIfNeeded(it) }

        // Actualizar Widget de pantalla de inicio
        widgetStore.save(
            name = current?.clientName ?: "",
            address = current?.displayAddress,
            balance = current?.balanceDue ?: 0.0,
            delivered = delivered,
            total = deliveries.size,
            isComplete = isComplete
        )
        viewModelScope.launch {
            try {
                GlanceAppWidgetManager(getApplication())
                    .getGlanceIds(DeliveryWidget::class.java)
                    .forEach { id -> DeliveryWidget().update(getApplication(), id) }
            } catch (_: Exception) { /* Widget no colocado en pantalla de inicio */ }
        }
    }

    private fun geocodeDeliveryAddressIfNeeded(delivery: RouteDeliveryDto) {
        if (delivery.latitude != null && delivery.longitude != null) return
        val address = delivery.displayAddress ?: return
        if (address.isBlank()) return

        viewModelScope.launch {
            try {
                Log.i("RouteViewModel", "Intentando geocodificar dirección: $address")
                val geocoder = android.location.Geocoder(getApplication())
                val results = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        geocoder.getFromLocationName(address, 1)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (!results.isNullOrEmpty()) {
                    val lat = results[0].latitude
                    val lng = results[0].longitude
                    Log.i("RouteViewModel", "📍 Geocoding exitoso para $address: $lat, $lng")

                    val response = NetworkModule.api.updateDeliveryCoordinates(
                        driverToken,
                        delivery.deliveryId,
                        UpdateLocationRequest(lat, lng)
                    )

                    if (response.isSuccessful) {
                        loadRoute()
                    }
                } else {
                    Log.w("RouteViewModel", "⚠️ No se encontraron coordenadas para la dirección: $address")
                }
            } catch (e: Exception) {
                Log.e("RouteViewModel", "❌ Error geocoding address $address: ${e.message}")
            }
        }
    }

    // ─── LOCATION MONITORING (PRE-ROUTE) ───

    fun startLocationMonitoring() {
        if (locationCallback != null) return // Ya monitoreando

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    LocationForegroundService.lastLat = location.latitude
                    LocationForegroundService.lastLng = location.longitude
                    LocationForegroundService.hasLocation = true

                    // Si no hay polilínea aún, intentar fetch completo
                    if (_uiState.value.polylinePoints.isEmpty() && _uiState.value.routeData != null) {
                        Log.i("RouteViewModel", "📍 Ubicación recibida, obteniendo ruta completa...")
                        fetchFullRoutePolyline()
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                android.os.Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("RouteViewModel", "No se pudo iniciar monitoreo: ${e.message}")
        }
    }

    private fun stopLocationMonitoring() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    // ─── POLYLINE ───

    fun fetchFullRoutePolyline() {
        viewModelScope.launch {
            if (!LocationForegroundService.hasLocation) {
                Log.w("RouteViewModel", "fetchFullRoutePolyline: No hay ubicación GPS")
                return@launch
            }
            val routeData = _uiState.value.routeData ?: run {
                Log.w("RouteViewModel", "fetchFullRoutePolyline: No hay datos de ruta")
                return@launch
            }
            
            val originLat = LocationForegroundService.lastLat
            val originLng = LocationForegroundService.lastLng

            val pendingDeliveries = routeData.deliveries
                .filter { (it.status == "Pending" || it.status == "InTransit") && it.latitude != null && it.longitude != null }
                .sortedBy { it.sortOrder }

            if (pendingDeliveries.isEmpty()) {
                _uiState.update { it.copy(polylinePoints = emptyList(), routeInfo = null) }
                return@launch
            }

            // Destino final es la última de la lista
            val finalDestination = pendingDeliveries.last()
            val destLat = finalDestination.latitude!!
            val destLng = finalDestination.longitude!!

            // Waypoints son todas las que van ANTES de la última
            val waypoints = if (pendingDeliveries.size > 1) {
                pendingDeliveries.dropLast(1).map { LatLng(it.latitude!!, it.longitude!!) }
            } else {
                emptyList()
            }

            Log.i("RouteViewModel", "Fetching full polyline: Origin($originLat, $originLng) -> ${waypoints.size} waypoints -> Dest($destLat, $destLng)")

            val info = DirectionsManager.getRoutePoints(
                originLat, originLng, 
                destLat, destLng, 
                waypoints
            )
            
            if (info != null) {
                Log.i("RouteViewModel", "✅ Ruta obtenida: ${info.points.size} puntos")
                _uiState.update { it.copy(polylinePoints = info.points, routeInfo = info) }
            } else {
                Log.e("RouteViewModel", "❌ Error al obtener ruta (info == null)")
            }
        }
    }

    // Deprecated in favor of fetchFullRoutePolyline, kept for internal reference if needed
    private fun fetchPolyline(destLat: Double, destLng: Double) {
        fetchFullRoutePolyline()
    }

    // ─── SIGNALR ───

    private fun setupSignalR() {
        viewModelScope.launch {
            signalR.events.collect { event ->
                when (event) {
                    SignalREvent.Connected -> {
                        _uiState.update { it.copy(isSignalRConnected = true) }
                    }
                    SignalREvent.Disconnected -> {
                        _uiState.update { it.copy(isSignalRConnected = false) }
                    }
                    SignalREvent.NewChatMessage -> {
                        _uiState.update { it.copy(hasNewChatMessage = true) }
                    }
                    SignalREvent.RouteNeedsRefresh -> Unit
                    is SignalREvent.AdminMessageReceived -> {
                        _uiState.update { it.copy(hasNewChatMessage = true) }
                        camiManager?.announce("Soporte te envió un mensaje: ${event.message.text}")
                    }
                    is SignalREvent.ClientMessageReceived -> {
                        val isCurrent = event.message.deliveryId == _uiState.value.currentDelivery?.deliveryId
                        if (isCurrent) {
                            camiManager?.announce("La clienta ${event.message.sender} te escribió: ${event.message.text}")
                        }
                        _uiState.update { it.copy(hasNewChatMessage = true) }
                    }
                    is SignalREvent.OrderDataChanged -> {
                        handleOrderDataChanged(
                            clientName = event.clientName,
                            newTotal = event.newTotal,
                            balanceDue = event.balanceDue,
                            changeType = event.changeType
                        )
                    }
                    is SignalREvent.RouteDeleted -> {
                        LocationForegroundService.stop(getApplication())
                        _uiState.update {
                            it.copy(routeDeletedByAdmin = true, routeDeletedMessage = event.message)
                        }
                        camiManager?.announce(
                            "Atención. El administrador eliminó esta ruta. Por favor contacta al administrador."
                        )
                    }
                }
            }
        }
        signalR.connect(driverToken)
    }

    private fun handleOrderDataChanged(
        clientName: String,
        newTotal: Double,
        balanceDue: Double,
        changeType: String
    ) {
        _uiState.update {
            it.copy(
                orderChangedAlert = true,
                orderChangedClientName = clientName,
                orderChangedNewTotal = newTotal,
                orderChangedBalanceDue = balanceDue
            )
        }
        val announcement = if (changeType == "payment") {
            if (balanceDue <= 0) {
                "Atención. El admin registró un pago completo para el pedido de $clientName. No necesitas cobrar nada."
            } else {
                "Atención. El admin registró un abono para el pedido de $clientName. El saldo pendiente ahora es ${String.format("%,.0f", balanceDue)} pesos."
            }
        } else {
            "Atención. El admin actualizó el pedido de $clientName. " +
                "El nuevo total es ${String.format("%,.0f", newTotal)} pesos. " +
                "Revisa los detalles antes de cobrar."
        }
        camiManager?.announce(announcement)
        viewModelScope.launch {
            delay(20_000)
            _uiState.update { it.copy(orderChangedAlert = false) }
        }
    }

    private fun setupSignalRCallbacksDeprecated() {
        signalR.onAdminMessageReceived = { msg ->
            _uiState.update { it.copy(hasNewChatMessage = true) }
            camiManager?.announce("Soporte te envió un mensaje: ${msg.text}")
        }
        
        signalR.onClientMessageReceived = { msg ->
            // Si el mensaje es para la clienta de la parada actual, avisamos con más énfasis
            val isCurrent = msg.deliveryId == _uiState.value.currentDelivery?.deliveryId
            if (isCurrent) {
                camiManager?.announce("La clienta ${msg.sender} te escribió: ${msg.text}")
            }
            _uiState.update { it.copy(hasNewChatMessage = true) }
        }

        signalR.onConnected = {
            _uiState.update { it.copy(isSignalRConnected = true) }
        }
        signalR.onDisconnected = {
            _uiState.update { it.copy(isSignalRConnected = false) }
        }
        // ... rest of event handlers stay the same ...
        // El admin modificó precios/productos/pagos de un pedido activo en la ruta
        signalR.onOrderDataChanged = { clientName, newTotal, balanceDue, changeType ->
            _uiState.update {
                it.copy(
                    orderChangedAlert = true,
                    orderChangedClientName = clientName,
                    orderChangedNewTotal = newTotal,
                    orderChangedBalanceDue = balanceDue
                )
            }
            // CAMI anuncia el cambio con el mensaje correcto según el tipo
            val announcement = if (changeType == "payment") {
                if (balanceDue <= 0)
                    "Atención. El admin registró un pago completo para el pedido de $clientName. No necesitas cobrar nada."
                else
                    "Atención. El admin registró un abono para el pedido de $clientName. El saldo pendiente ahora es ${String.format("%,.0f", balanceDue)} pesos."
            } else {
                "Atención. El admin actualizó el pedido de $clientName. " +
                "El nuevo total es ${String.format("%,.0f", newTotal)} pesos. " +
                "Revisa los detalles antes de cobrar."
            }
            camiManager?.announce(announcement)
            // Auto-dismiss después de 20 segundos
            viewModelScope.launch {
                delay(20_000)
                _uiState.update { it.copy(orderChangedAlert = false) }
            }
        }

        // El admin eliminó la ruta mientras estaba activa
        signalR.onRouteDeleted = { message ->
            LocationForegroundService.stop(getApplication())
            _uiState.update {
                it.copy(routeDeletedByAdmin = true, routeDeletedMessage = message)
            }
            camiManager?.announce("Atención. El administrador eliminó esta ruta. Por favor contacta al administrador.")
        }

        signalR.connect(driverToken)
    }

    fun dismissOrderChangedAlert() {
        _uiState.update { it.copy(orderChangedAlert = false) }
    }

    // ─── REFRESH PERIÓDICO DE SEGURIDAD ───
    // Fallback en caso de que SignalR pierda algún evento (ej. reconexión tardía).
    // Solo corre mientras la ruta está activa y no completada.
    private fun startPeriodicSafetyRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(120_000L) // cada 2 minutos
                val state = _uiState.value
                if (state.isRouteStarted && !state.isRouteComplete && !state.routeDeletedByAdmin) {
                    Log.d("RouteViewModel", "🔄 Refresh periódico de seguridad")
                    loadRoute()
                }
            }
        }
    }

    // ─── C.A.M.I. ───

    private fun setupCami() {
        camiManager = CamiManager(getApplication(), viewModelScope).apply {
            setToken(driverToken)
            onStateChange = { state ->
                _uiState.update { it.copy(camiState = state) }
            }
            onTranscript = { text ->
                _uiState.update { it.copy(camiTranscript = text) }
            }
            onResponse = { text ->
                _uiState.update { it.copy(camiResponse = text) }
                // Limpiar respuesta después de 8 segundos
                viewModelScope.launch {
                    delay(8000)
                    _uiState.update { it.copy(camiResponse = null, camiTranscript = null) }
                }
            }
            onError = { error ->
                _uiState.update { it.copy(error = "CAMI: $error") }
            }
        }

        // Auto-TTS Bluetooth — CAMI anuncia briefing completo al llegar cerca del destino
        LocationForegroundService.onProximityDetected = { name, instructions, balance, phone, paymentMethod ->
            val currentItems = _uiState.value.currentDelivery?.items
            val totalPieces = currentItems?.sumOf { it.quantity } ?: 0
            val itemCount = currentItems?.size ?: 0

            val msg = buildString {
                append("Atención. Estás llegando con $name.")
                if (balance > 0) {
                    append(" Cobra ${String.format("%,.0f", balance)} pesos")
                    if (!paymentMethod.isNullOrBlank()) append(" en $paymentMethod")
                    append(".")
                }
                if (totalPieces > 0) {
                    append(" Lleva $itemCount ${if (itemCount == 1) "producto" else "productos"}")
                    append(", $totalPieces ${if (totalPieces == 1) "pieza" else "piezas"} en total.")
                }
                if (!instructions.isNullOrBlank()) append(" Nota: $instructions.")
                append(" ¡Suerte!")
            }
            camiManager?.announce(msg)

            // Auto-marcar InTransit en la API
            val deliveryId = LocationForegroundService.currentDeliveryId
            if (deliveryId > 0) {
                viewModelScope.launch {
                    try {
                        NetworkModule.api.markInTransit(driverToken, deliveryId)
                        Log.i("RouteViewModel", "✅ Auto InTransit para delivery $deliveryId")
                    } catch (e: Exception) {
                        Log.e("RouteViewModel", "❌ Auto InTransit falló: ${e.message}")
                    }
                }
            }

            // Mostrar banner de proximidad en la UI
            _uiState.update { it.copy(proximityAlert = true, proximityDeliveryName = name) }
            viewModelScope.launch {
                delay(8000)
                _uiState.update { it.copy(proximityAlert = false) }
            }
        }
    }

    fun toggleCami() {
        val now = System.currentTimeMillis()
        if (now - lastCamiToggleMs < 1500L) return // debounce 1.5s
        lastCamiToggleMs = now
        camiManager?.toggleListening()
    }

    fun dismissChatAlert() {
        _uiState.update { it.copy(hasNewChatMessage = false) }
    }

    // ─── OFFLINE QUEUE ───

    private fun monitorOfflineQueue() {
        viewModelScope.launch {
            offlineQueue.hasPending.collect { hasPending ->
                val all = offlineQueue.getAll()
                _uiState.update { it.copy(pendingActionsCount = all.size) }
            }
        }
    }

    private fun monitorNetworkConnectivity() {
        val monitor = NetworkMonitor(getApplication())
        var wasOffline = false
        viewModelScope.launch {
            monitor.isOnline.collect { isOnline ->
                _uiState.update { it.copy(isOffline = !isOnline) }
                if (isOnline && wasOffline) {
                    Log.i("RouteViewModel", "🌐 Conexión restaurada — reintentando cola offline")
                    processOfflineQueue()
                }
                wasOffline = !isOnline
            }
        }
    }

    private fun processOfflineQueue() {
        viewModelScope.launch {
            val pending = offlineQueue.getAll()
            if (pending.isEmpty()) return@launch

            Log.i("RouteViewModel", "🔄 Procesando ${pending.size} acciones offline...")
            pending.forEach { action ->
                try {
                    val sent = when (action.type) {
                        "DELIVER" -> retryDeliver(action)
                        "FAIL" -> retryFail(action)
                        else -> false
                    }
                    if (sent) {
                        offlineQueue.remove(action.id)
                        evidenceStore.delete(action.allEvidencePaths)
                    }
                } catch (e: Exception) {
                    Log.w("RouteViewModel", "⚠️ No se pudo reintentar acción ${action.id}: ${e.message}")
                }
            }
            loadRoute()
        }
    }

    private suspend fun retryDeliver(action: PendingAction): Boolean {
        val paymentsJson = action.paymentsJson ?: "[]"

        val textMediaType = "text/plain".toMediaTypeOrNull()
        val signatureSvg = evidenceStore.readText(action.signatureSvgPath)
        val evidenceParts = evidenceStore.buildPhotoParts(action.evidencePaths.orEmpty())

        val response = NetworkModule.api.markDelivered(
            driverToken = action.driverToken,
            deliveryId = action.deliveryId,
            idempotencyKey = "offline-retry-${action.id}",
            notes = (action.notes ?: "Entregado offline (reintento)").toRequestBody(textMediaType),
            payments = paymentsJson.toRequestBody(textMediaType),
            paymentsJson = paymentsJson.toRequestBody(textMediaType),
            signatureSvg = signatureSvg?.toRequestBody(textMediaType),
            signedByName = action.signedByName?.toRequestBody(textMediaType),
            photos = evidenceParts.ifEmpty { null }
        )
        return response.isSuccessful
    }

    private suspend fun retryFail(action: PendingAction): Boolean {
        val textMediaType = "text/plain".toMediaTypeOrNull()
        val evidenceParts = evidenceStore.buildPhotoParts(action.evidencePaths.orEmpty())

        val response = NetworkModule.api.markFailed(
            driverToken = action.driverToken,
            deliveryId = action.deliveryId,
            idempotencyKey = "offline-retry-${action.id}",
            reason = (action.reason ?: "Sin conexión").toRequestBody("text/plain".toMediaTypeOrNull()),
            notes = (action.notes ?: "Reportado offline (reintento)").toRequestBody(textMediaType),
            photos = evidenceParts.ifEmpty { null }
        )
        return response.isSuccessful
    }

    // ─── ROUTE ACTIONS ───

    fun startRoute() {
        viewModelScope.launch {
            try {
                val response = NetworkModule.api.startRoute(driverToken)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLocationPaused = false) }
                    try {
                        LocationForegroundService.start(getApplication(), driverToken)
                    } catch (e: Exception) {
                        Log.e("RouteViewModel", "No se pudo iniciar GPS service: ${e.message}")
                    }
                    loadRoute()
                    // Feature #6 — Briefing de CAMI al iniciar la ruta
                    triggerRouteBriefing()
                } else {
                    _uiState.update { it.copy(error = "Error del servidor: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error iniciando ruta: ${e.localizedMessage}") }
            }
        }
    }

    /** Feature #6 — CAMI anuncia el briefing de la ruta al conductor en voz. */
    private fun triggerRouteBriefing() {
        viewModelScope.launch {
            try {
                // Obtener el routeId directamente de la API (loadRoute() puede no haber terminado aún)
                val routeResponse = NetworkModule.api.getRoute(driverToken)
                val routeId = routeResponse.body()?.id ?: return@launch
                val response = NetworkModule.api.getRouteBriefing(routeId)
                if (response.isSuccessful) {
                    val briefing = response.body() ?: return@launch
                    if (briefing.text.isNotBlank()) {
                        // Pequeña pausa para que el chofer ya haya visto la ruta iniciada
                        delay(2000)
                        camiManager?.announce(briefing.text)
                    }
                }
            } catch (e: Exception) {
                Log.w("RouteViewModel", "No se pudo obtener briefing de CAMI: ${e.message}")
            }
        }
    }

    fun markCurrentInTransit() {
        val delivery = _uiState.value.currentDelivery ?: return
        if (delivery.status == "InTransit") return

        viewModelScope.launch {
            try {
                val response = NetworkModule.api.markInTransit(driverToken, delivery.deliveryId)
                if (response.isSuccessful) {
                    loadRoute()
                } else {
                    _uiState.update { it.copy(error = "No se pudo marcar en camino (${response.code()})") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Sin conexión para marcar en camino") }
            }
        }
    }

    fun deliverCurrent(payments: List<PaymentInputDto>) {
        val delivery = _uiState.value.currentDelivery ?: return
        val photo = _uiState.value.capturedPhoto
        val signature = _uiState.value.capturedSignature
        val idempotencyKey = _uiState.value.deliveryIdempotencyKey

        viewModelScope.launch {
            _uiState.update { it.copy(isDelivering = true) }
            try {
                val paymentsJson = Gson().toJson(payments)
                val textMediaType = "text/plain".toMediaTypeOrNull()

                val photoParts = buildPhotoParts(photo) + buildSignatureParts(signature)

                val response = NetworkModule.api.markDelivered(
                    driverToken = driverToken,
                    deliveryId = delivery.deliveryId,
                    idempotencyKey = idempotencyKey,
                    notes = "Entregado desde app nativa".toRequestBody(textMediaType),
                    payments = paymentsJson.toRequestBody(textMediaType),
                    paymentsJson = paymentsJson.toRequestBody(textMediaType),
                    signatureSvg = signature?.svg?.toRequestBody(textMediaType),
                    signedByName = signature?.signedByName?.toRequestBody(textMediaType),
                    photos = photoParts.ifEmpty { null }
                )

                if (!response.isSuccessful && !shouldQueueOffline(response.code())) {
                    _uiState.update {
                        it.copy(
                            isDelivering = false,
                            error = "No se pudo registrar la entrega (${response.code()}). Revisa los datos o contacta al administrador."
                        )
                    }
                    return@launch
                }

                if (response.isSuccessful) {
                    _uiState.update { it.copy(isDelivering = false, capturedPhoto = null, capturedSignature = null) }
                    loadRoute()
                } else {
                    // Encolar para reintentar
                    enqueueDeliveryOffline(delivery, paymentsJson, photo, signature)
                    _uiState.update { it.copy(isDelivering = false, capturedPhoto = null, capturedSignature = null, error = "Sin conexión. Acción encolada para reintentar.") }
                }
            } catch (e: Exception) {
                // Encolar para reintentar offline
                val paymentsJson = Gson().toJson(payments)
                enqueueDeliveryOffline(delivery, paymentsJson, photo, signature)
                _uiState.update { it.copy(isDelivering = false, capturedPhoto = null, capturedSignature = null, error = "Sin conexión. Acción guardada para cuando haya internet.") }
            }
        }
    }

    fun failCurrent(reason: String) {
        val delivery = _uiState.value.currentDelivery ?: return
        val photo = _uiState.value.capturedPhoto
        val idempotencyKey = _uiState.value.deliveryIdempotencyKey

        viewModelScope.launch {
            try {
                val photoParts = buildPhotoParts(photo)

                val response = NetworkModule.api.markFailed(
                    driverToken = driverToken,
                    deliveryId = delivery.deliveryId,
                    idempotencyKey = idempotencyKey,
                    reason = reason.toRequestBody("text/plain".toMediaTypeOrNull()),
                    notes = "Reportado desde app nativa".toRequestBody("text/plain".toMediaTypeOrNull()),
                    photos = photoParts.ifEmpty { null }
                )

                if (!response.isSuccessful && !shouldQueueOffline(response.code())) {
                    _uiState.update {
                        it.copy(
                            capturedPhoto = null,
                            error = "No se pudo registrar la falla (${response.code()}). Revisa los datos o contacta al administrador."
                        )
                    }
                    return@launch
                }

                if (response.isSuccessful) {
                    _uiState.update { it.copy(capturedPhoto = null) }
                    loadRoute()
                } else {
                    enqueueFailOffline(delivery, reason, photo)
                    _uiState.update { it.copy(capturedPhoto = null, error = "Sin conexión. Falla guardada para reintentar.") }
                }
            } catch (e: Exception) {
                enqueueFailOffline(delivery, reason, photo)
                _uiState.update { it.copy(capturedPhoto = null, error = "Sin conexión. Falla guardada.") }
            }
        }
    }

    private suspend fun enqueueDeliveryOffline(
        delivery: RouteDeliveryDto,
        paymentsJson: String,
        photo: Bitmap?,
        signature: CapturedSignature?
    ) {
        val actionId = UUID.randomUUID().toString()
        val evidencePaths = listOfNotNull(
            evidenceStore.saveJpeg(actionId, "photo", photo),
            evidenceStore.savePng(actionId, "signature", signature?.bitmap)
        )
        val signatureSvgPath = evidenceStore.saveText(actionId, "signature_svg", signature?.svg)

        offlineQueue.enqueue(
            PendingAction(
                id = actionId,
                type = "DELIVER",
                driverToken = driverToken,
                deliveryId = delivery.deliveryId,
                reason = null,
                notes = "Entregado desde app nativa",
                paymentsJson = paymentsJson,
                evidencePaths = evidencePaths,
                signatureSvgPath = signatureSvgPath,
                signedByName = signature?.signedByName,
                timestamp = System.currentTimeMillis()
            )
        )
        SyncWorker.schedule(getApplication(), driverToken)
    }

    private suspend fun enqueueFailOffline(
        delivery: RouteDeliveryDto,
        reason: String,
        photo: Bitmap?
    ) {
        val actionId = UUID.randomUUID().toString()
        val evidencePaths = listOfNotNull(evidenceStore.saveJpeg(actionId, "failure_photo", photo))

        offlineQueue.enqueue(
            PendingAction(
                id = actionId,
                type = "FAIL",
                driverToken = driverToken,
                deliveryId = delivery.deliveryId,
                reason = reason,
                notes = "Reportado desde app nativa",
                paymentsJson = null,
                evidencePaths = evidencePaths,
                timestamp = System.currentTimeMillis()
            )
        )
        SyncWorker.schedule(getApplication(), driverToken)
    }

    fun reorderDeliveries(orderedDeliveryIds: List<Int>) {
        viewModelScope.launch {
            try {
                val currentRoute = _uiState.value.routeData
                if (currentRoute != null && orderedDeliveryIds.isNotEmpty()) {
                    val reorderedDeliveries = currentRoute.deliveries.map { delivery ->
                        val newPos = orderedDeliveryIds.indexOf(delivery.deliveryId)
                        if (newPos != -1) {
                            delivery.copy(sortOrder = newPos + 1)
                        } else {
                            delivery
                        }
                    }.sortedWith(compareBy<RouteDeliveryDto> { it.sortOrder }.thenBy { it.deliveryId })

                    val newCurrent = reorderedDeliveries.firstOrNull { it.status == "Pending" || it.status == "InTransit" }
                    _uiState.update { state ->
                        state.copy(
                            routeData = currentRoute.copy(deliveries = reorderedDeliveries),
                            currentDelivery = newCurrent ?: state.currentDelivery
                        )
                    }
                }

                val response = NetworkModule.api.reorderDeliveries(driverToken, orderedDeliveryIds)
                if (response.isSuccessful) {
                    loadRoute()
                } else {
                    _uiState.update { it.copy(error = "Error reordenando entregas") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error reordenando: ${e.localizedMessage}") }
            } finally {
                fetchFullRoutePolyline()
            }
        }
    }

    fun onPhotoCaptured(bitmap: Bitmap?) {
        _uiState.update { it.copy(capturedPhoto = bitmap) }
    }

    fun onSignatureCaptured(signature: CapturedSignature?) {
        _uiState.update { it.copy(capturedSignature = signature) }
    }

    fun pauseRoute() {
        _uiState.update { it.copy(isLocationPaused = true) }
    }

    fun resumeLocationTracking() {
        _uiState.update { it.copy(isLocationPaused = false) }
        LocationForegroundService.start(getApplication(), driverToken)
    }

    fun stopTracking() {
        LocationForegroundService.stop(getApplication())
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ─── HELPERS ───

    private fun shouldQueueOffline(statusCode: Int): Boolean =
        statusCode == 408 || statusCode == 429 || statusCode in 500..599

    private fun buildPhotoParts(photo: Bitmap?): List<MultipartBody.Part> {
        if (photo == null) return emptyList()
        val stream = java.io.ByteArrayOutputStream()
        photo.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val photoBody = stream.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull())
        return listOf(MultipartBody.Part.createFormData("photos", "evidence.jpg", photoBody))
    }

    private fun buildSignatureParts(signature: CapturedSignature?): List<MultipartBody.Part> {
        if (signature == null) return emptyList()
        val stream = java.io.ByteArrayOutputStream()
        signature.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val body = stream.toByteArray().toRequestBody("image/png".toMediaTypeOrNull())
        return listOf(MultipartBody.Part.createFormData("photos", "signature.png", body))
    }

    override fun onCleared() {
        super.onCleared()
        camiManager?.destroy()
        stopLocationMonitoring()
        // SignalR se mantiene conectado si hay otras vistas usando la ruta
    }
}
