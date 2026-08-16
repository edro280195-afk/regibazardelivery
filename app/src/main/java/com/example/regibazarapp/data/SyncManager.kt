package com.example.regibazarapp.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * ══════════════════════════════════════════════════════════
 *  SyncManager – Controlador global de refresco de datos
 *
 *  Permite que eventos externos (FCM, SignalR, Background Sync)
 *  notifiquen a la UI activa que debe recargar la ruta.
 * ══════════════════════════════════════════════════════════
 */
object SyncManager {
    private val _refreshFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshFlow = _refreshFlow.asSharedFlow()

    /**
     * Emite una señal de refresco. Los ViewModels suscritos
     * reaccionarán llamando a sus métodos de carga (ej. loadRoute).
     */
    fun triggerRefresh() {
        _refreshFlow.tryEmit(Unit)
    }
}
