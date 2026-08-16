package com.example.regibazarapp.service

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import com.example.regibazarapp.data.SyncManager
import com.example.regibazarapp.data.models.ChatMessageDto
import com.example.regibazarapp.data.network.NetworkModule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * ══════════════════════════════════════════════════════════════
 *  SignalRManager – Singleton hub connection for DeliveryHub
 *
 *  Hub URL  : https://regibazarapi.onrender.com/deliveryHub
 *  Pattern  : Singleton via companion object getInstance()
 * ══════════════════════════════════════════════════════════════
 */

// ── Payload data class used for typed hub events ──────────────
data class SignalRPayload(
    val id: Int? = null,
    val status: String? = null,
    val routeId: Int? = null
)

// ── Payload para cambios de datos en pedidos activos ──────────
data class OrderChangedPayload(
    val orderId: Int? = null,
    val clientName: String? = null,
    val newTotal: Double? = null,
    val balanceDue: Double? = null,
    val changeType: String? = null  // "items" | "payment" | "details"
)

// ── Payload para eliminación de ruta ──────────────────────────
data class RouteDeletedPayload(
    val message: String? = null
)

sealed class SignalREvent {
    data object RouteNeedsRefresh : SignalREvent()
    data object NewChatMessage : SignalREvent()
    data object Connected : SignalREvent()
    data object Disconnected : SignalREvent()
    data class AdminMessageReceived(val message: ChatMessageDto) : SignalREvent()
    data class ClientMessageReceived(val message: ChatMessageDto) : SignalREvent()
    data class OrderDataChanged(
        val clientName: String,
        val newTotal: Double,
        val balanceDue: Double,
        val changeType: String
    ) : SignalREvent()
    data class RouteDeleted(val message: String) : SignalREvent()
}

class SignalRManager private constructor() {

    companion object {
        // Reconnect backoff settings
        private const val RECONNECT_DELAY_START_MS = 3_000L
        private const val RECONNECT_DELAY_MAX_MS   = 30_000L

        @Volatile
        private var instance: SignalRManager? = null

        fun getInstance(): SignalRManager =
            instance ?: synchronized(this) {
                instance ?: SignalRManager().also { instance = it }
            }
    }

    // ── Coroutine scope for async ops ─────────────────────────
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Hub connection ────────────────────────────────────────
    private var hubConnection: HubConnection? = null

    // ── Internal state ────────────────────────────────────────
    private var currentToken: String = ""
    private var reconnectDelayMs: Long = RECONNECT_DELAY_START_MS
    private var shouldReconnect: Boolean = false

    private val _events = MutableSharedFlow<SignalREvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SignalREvent> = _events.asSharedFlow()

    // ── Public callbacks ──────────────────────────────────────
    var onRouteNeedsRefresh: (() -> Unit)? = null
    var onNewChatMessage: (() -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    /** Se invoca cuando el admin modifica datos de un pedido que está en la ruta activa */
    var onOrderDataChanged: ((clientName: String, newTotal: Double, balanceDue: Double, changeType: String) -> Unit)? = null
    /** Se invoca cuando el admin elimina la ruta mientras está activa */
    var onRouteDeleted: ((message: String) -> Unit)? = null

    // ── Chat Real-Time Callbacks ──
    var onAdminMessageReceived: ((ChatMessageDto) -> Unit)? = null
    var onClientMessageReceived: ((ChatMessageDto) -> Unit)? = null

    // ── Public properties ─────────────────────────────────────
    val isConnected: Boolean
        get() = hubConnection?.connectionState == HubConnectionState.CONNECTED

    // ── Public API ────────────────────────────────────────────

    /**
     * Connect to the DeliveryHub with the given driver token.
     * Registers all server-push event handlers and joins the route group.
     * Auto-reconnects on unexpected closure with exponential backoff.
     */
    fun connect(token: String) {
        if (currentToken == token && isConnected) {
            emitEvent(SignalREvent.Connected)
            joinRoute(token)
            return
        }
        currentToken = token
        shouldReconnect = true
        reconnectDelayMs = RECONNECT_DELAY_START_MS
        buildAndStart()
    }

    fun disconnect() {
        shouldReconnect = false
        scope.launch {
            try {
                hubConnection?.stop()?.blockingAwait()
            } catch (_: Exception) { }
            hubConnection = null
            emitEvent(SignalREvent.Disconnected)
        }
    }

    fun reportLocation(lat: Double, lng: Double) {
        if (!isConnected) return
        try {
            hubConnection?.send("ReportLocation", currentToken, lat, lng)
        } catch (e: Exception) {
            Log.e("SignalRManager", "❌ Error enviando ubicación: ${e.message}")
        }
    }

    // ── Internal connection lifecycle ─────────────────────────

    private fun buildAndStart() {
        // Tear down any previous connection first
        try { hubConnection?.stop()?.blockingAwait() } catch (_: Exception) { }

        val connection = HubConnectionBuilder
            .create(NetworkModule.SIGNALR_HUB_URL)
            .build()

        registerEventHandlers(connection)

        // Handle unexpected disconnection → exponential backoff reconnect
        connection.onClosed { error ->
            onDisconnected?.invoke()
            emitEvent(SignalREvent.Disconnected)
            if (shouldReconnect) {
                scope.launch {
                    delay(reconnectDelayMs)
                    reconnectDelayMs = minOf(reconnectDelayMs * 2, RECONNECT_DELAY_MAX_MS)
                    buildAndStart()
                }
            }
        }

        hubConnection = connection

        // start() returns a Single<Unit> (reactive type)
        connection.start().subscribe(
            {
                // Success: connection established
                reconnectDelayMs = RECONNECT_DELAY_START_MS   // reset backoff on success
                onConnected?.invoke()
                emitEvent(SignalREvent.Connected)
                joinRoute(currentToken)
            },
            { error ->
                // Error starting — schedule reconnect
                if (shouldReconnect) {
                    scope.launch {
                        delay(reconnectDelayMs)
                        reconnectDelayMs = minOf(reconnectDelayMs * 2, RECONNECT_DELAY_MAX_MS)
                        buildAndStart()
                    }
                }
            }
        )
    }

    private fun joinRoute(token: String) {
        try {
            hubConnection?.send("JoinRoute", token)
        } catch (e: Exception) {
            // Non-critical: the route events may still flow without joining
        }
    }

    private fun emitEvent(event: SignalREvent) {
        if (!_events.tryEmit(event)) {
            scope.launch {
                _events.emit(event)
            }
        }
    }

    // ── Server-push event registration ───────────────────────

    private fun registerEventHandlers(connection: HubConnection) {

        // "DeliveryStatusUpdate" – server sends {id, status}
        connection.on(
            "DeliveryStatusUpdate",
            { _: SignalRPayload ->
                onRouteNeedsRefresh?.invoke()
                emitEvent(SignalREvent.RouteNeedsRefresh)
                SyncManager.triggerRefresh()
            },
            SignalRPayload::class.java
        )

        // "RouteUpdated" – server sends route change notification
        connection.on(
            "RouteUpdated",
            { _: SignalRPayload ->
                onRouteNeedsRefresh?.invoke()
                emitEvent(SignalREvent.RouteNeedsRefresh)
                SyncManager.triggerRefresh()
            },
            SignalRPayload::class.java
        )

        // "ReceiveMessage" – new chat message (trigger both callbacks)
        connection.on(
            "ReceiveMessage",
            { _: Any? ->
                onNewChatMessage?.invoke()
                emitEvent(SignalREvent.NewChatMessage)
                onRouteNeedsRefresh?.invoke()
                emitEvent(SignalREvent.RouteNeedsRefresh)
                SyncManager.triggerRefresh()
            },
            Any::class.java
        )

        // "RouteStarted" – server signals route has started
        connection.on(
            "RouteStarted",
            { _: SignalRPayload ->
                onRouteNeedsRefresh?.invoke()
                emitEvent(SignalREvent.RouteNeedsRefresh)
                SyncManager.triggerRefresh()
            },
            SignalRPayload::class.java
        )

        // "ReceiveChatMessage" – admin envió mensaje al conductor via controller (no hub)
        connection.on(
            "ReceiveChatMessage",
            { msg: ChatMessageDto ->
                onAdminMessageReceived?.invoke(msg)
                emitEvent(SignalREvent.AdminMessageReceived(msg))
                onNewChatMessage?.invoke()
                emitEvent(SignalREvent.NewChatMessage)
                SyncManager.triggerRefresh()
            },
            ChatMessageDto::class.java
        )

        // "ReceiveClientChatMessage" – el cliente respondió al conductor
        connection.on(
            "ReceiveClientChatMessage",
            { msg: ChatMessageDto ->
                onClientMessageReceived?.invoke(msg)
                emitEvent(SignalREvent.ClientMessageReceived(msg))
                onNewChatMessage?.invoke()
                emitEvent(SignalREvent.NewChatMessage)
            },
            ChatMessageDto::class.java
        )

        // "OrderDataChanged" – admin modificó precios/productos/pagos de un pedido en ruta activa
        connection.on(
            "OrderDataChanged",
            { payload: OrderChangedPayload ->
                val clientName = payload.clientName ?: ""
                val newTotal = payload.newTotal ?: 0.0
                val balanceDue = payload.balanceDue ?: newTotal
                val changeType = payload.changeType ?: "items"
                onOrderDataChanged?.invoke(clientName, newTotal, balanceDue, changeType)
                emitEvent(
                    SignalREvent.OrderDataChanged(
                        clientName = clientName,
                        newTotal = newTotal,
                        balanceDue = balanceDue,
                        changeType = changeType
                    )
                )
                onRouteNeedsRefresh?.invoke()
                emitEvent(SignalREvent.RouteNeedsRefresh)
                SyncManager.triggerRefresh()
            },
            OrderChangedPayload::class.java
        )

        // "RouteDeleted" – admin eliminó la ruta mientras estaba activa
        connection.on(
            "RouteDeleted",
            { payload: RouteDeletedPayload ->
                val message = payload.message ?: "La ruta fue eliminada por el administrador."
                onRouteDeleted?.invoke(message)
                emitEvent(SignalREvent.RouteDeleted(message))
            },
            RouteDeletedPayload::class.java
        )
    }
}
