package com.example.regibazarapp.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.regibazarapp.data.models.ChatMessageDto
import com.example.regibazarapp.data.models.SendMessageRequest
import com.example.regibazarapp.data.network.NetworkModule
import com.example.regibazarapp.service.SignalREvent
import com.example.regibazarapp.service.SignalRManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * ═══════════════════════════════════════════════════════════
 *  Chat ViewModel – Optimizado con SignalR (Real-time)
 * ═══════════════════════════════════════════════════════════
 */

data class ChatUiState(
    val isLoading: Boolean = true,
    val messages: List<ChatMessageDto> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isConnected: Boolean = true,
    val error: String? = null
)

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var driverToken: String = ""
    private var deliveryId: Int = 0

    fun initialize(token: String, deliveryId: Int) {
        if (this.driverToken == token && this.deliveryId == deliveryId) return
        this.driverToken = token
        this.deliveryId = deliveryId
        
        loadMessages()
        setupSignalR()
    }

    private fun setupSignalR() {
        val signalR = SignalRManager.getInstance()
        viewModelScope.launch {
            signalR.events.collect { event ->
                when (event) {
                    is SignalREvent.AdminMessageReceived -> {
                        val msg = event.message
                        if (deliveryId == 0 && (msg.deliveryId == null || msg.deliveryId == 0)) {
                            addMessage(msg)
                        }
                    }
                    is SignalREvent.ClientMessageReceived -> {
                        val msg = event.message
                        if (deliveryId > 0 && msg.deliveryId == deliveryId) {
                            addMessage(msg)
                        }
                    }
                    SignalREvent.Connected -> _uiState.update { it.copy(isConnected = true) }
                    SignalREvent.Disconnected -> _uiState.update { it.copy(isConnected = false) }
                    else -> Unit
                }
            }
        }
    }

    private fun setupSignalRCallbacksDeprecated() {
        val signalR = SignalRManager.getInstance()
        
        // Listener para mensajes de Admin
        signalR.onAdminMessageReceived = { msg ->
            // Si el ViewModel está en modo Admin (deliveryId == 0) 
            // y el mensaje NO tiene un deliveryId específico, es para el canal de soporte general
            if (deliveryId == 0 && (msg.deliveryId == null || msg.deliveryId == 0)) {
               addMessage(msg)
            }
        }
        
        // Listener para mensajes de Cliente
        signalR.onClientMessageReceived = { msg ->
            // Filtramos por deliveryId para asegurar que el chofer 
            // no reciba mensajes de otros clientes en esta vista activa
            if (deliveryId > 0 && msg.deliveryId == deliveryId) {
                addMessage(msg)
            }
        }
    }

    private fun addMessage(msg: ChatMessageDto) {
        _uiState.update { currentState ->
            if (currentState.messages.any { it.id == msg.id }) return@update currentState
            currentState.copy(messages = currentState.messages + msg)
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                val response = if (deliveryId == 0) {
                    NetworkModule.api.getAdminChat(driverToken)
                } else {
                    NetworkModule.api.getClientChat(driverToken, deliveryId)
                }
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(isLoading = false, messages = response.body()!!, error = null)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error cargando mensajes") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Sin conexión") }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isSending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, inputText = "") }
            try {
                val response = if (deliveryId == 0) {
                    NetworkModule.api.sendAdminMessage(driverToken, SendMessageRequest(text))
                } else {
                    NetworkModule.api.sendClientMessage(driverToken, deliveryId, SendMessageRequest(text))
                }
                if (response.isSuccessful && response.body() != null) {
                    // Agregamos el mensaje localmente para feedback inmediato
                    addMessage(response.body()!!)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error enviando mensaje") }
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
