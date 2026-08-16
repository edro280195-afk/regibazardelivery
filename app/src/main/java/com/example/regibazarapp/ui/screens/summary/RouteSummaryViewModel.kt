package com.example.regibazarapp.ui.screens.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.regibazarapp.data.local.RouteDatabase
import com.example.regibazarapp.data.local.RouteEntity
import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.data.models.RouteResponse
import com.example.regibazarapp.data.network.NetworkModule
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

data class RouteSummaryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val routeName: String = "",
    val deliveredCount: Int = 0,
    val failedCount: Int = 0,
    val totalCount: Int = 0,
    val totalCollected: Double = 0.0,
    val collectedByCash: Double = 0.0,
    val collectedByTransfer: Double = 0.0,
    val collectedByDeposit: Double = 0.0,
    val startedAt: String? = null,
    val deliveries: List<RouteDeliveryDto> = emptyList(),
    val fastestDoorTimeSecs: Long? = null,
    val slowestDoorTimeSecs: Long? = null
)

class RouteSummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val routeDao = RouteDatabase.getInstance(application).routeDao()
    private val gson = Gson()
    private val _uiState = MutableStateFlow(RouteSummaryUiState())
    val uiState: StateFlow<RouteSummaryUiState> = _uiState.asStateFlow()
    private var initializedToken: String? = null

    fun initialize(token: String) {
        if (initializedToken == token) return
        initializedToken = token
        loadSummary(token)
    }

    private fun loadSummary(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = NetworkModule.api.getRoute(token)
                if (response.isSuccessful && response.body() != null) {
                    val route = response.body()!!
                    withContext(Dispatchers.IO) {
                        routeDao.upsertRoute(RouteEntity(token = token, routeJson = gson.toJson(route)))
                    }
                    processRoute(route)
                } else {
                    loadCachedSummary(token, "No se pudo cargar el resumen (${response.code()}).")
                }
            } catch (_: Exception) {
                loadCachedSummary(token, "Sin conexión y sin resumen guardado.")
            }
        }
    }

    private suspend fun loadCachedSummary(token: String, fallbackError: String) {
        val cached = withContext(Dispatchers.IO) { routeDao.getRoute(token) }
        if (cached == null) {
            _uiState.update { it.copy(isLoading = false, error = fallbackError) }
            return
        }

        val route = runCatching {
            gson.fromJson(cached.routeJson, RouteResponse::class.java)
        }.getOrNull()

        if (route == null) {
            _uiState.update { it.copy(isLoading = false, error = fallbackError) }
        } else {
            processRoute(route)
        }
    }

    private fun processRoute(route: RouteResponse) {
        val deliveries = route.deliveries
        val delivered = deliveries.count { it.status.equals("Delivered", ignoreCase = true) }
        val failed = deliveries.count { it.status.equals("NotDelivered", ignoreCase = true) }
        val payments = deliveries
            .filter { it.status.equals("Delivered", ignoreCase = true) }
            .flatMap { it.payments.orEmpty() }

        val doorTimeSecs = deliveries
            .filter { it.status.equals("Delivered", ignoreCase = true) }
            .mapNotNull { delivery ->
                val arrived = parseInstant(delivery.arrivedAt)
                val deliveredAt = parseInstant(delivery.deliveredAt)
                if (arrived == null || deliveredAt == null) {
                    null
                } else {
                    val diff = (deliveredAt.toEpochMilli() - arrived.toEpochMilli()) / 1000L
                    diff.takeIf { it > 0 }
                }
            }

        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                routeName = route.name.orEmpty(),
                deliveredCount = delivered,
                failedCount = failed,
                totalCount = deliveries.size,
                totalCollected = payments.sumOf { payment -> payment.amount },
                collectedByCash = payments
                    .filter { payment -> payment.method.contains("Efectivo", ignoreCase = true) }
                    .sumOf { payment -> payment.amount },
                collectedByTransfer = payments
                    .filter { payment -> payment.method.contains("Transfer", ignoreCase = true) }
                    .sumOf { payment -> payment.amount },
                collectedByDeposit = payments
                    .filter { payment ->
                        payment.method.contains("Depósito", ignoreCase = true) ||
                            payment.method.contains("Deposito", ignoreCase = true)
                    }
                    .sumOf { payment -> payment.amount },
                startedAt = route.startedAt,
                deliveries = deliveries,
                fastestDoorTimeSecs = doorTimeSecs.minOrNull(),
                slowestDoorTimeSecs = doorTimeSecs.maxOrNull()
            )
        }
    }

    private fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value).toInstant()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(value.take(19)).atZone(ZoneId.systemDefault()).toInstant()
            } catch (_: Exception) {
                null
            }
        }
    }
}
