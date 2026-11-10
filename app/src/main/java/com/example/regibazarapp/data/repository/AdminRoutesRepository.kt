package com.example.regibazarapp.data.repository

import com.example.regibazarapp.data.models.AvailableTandaDto
import com.example.regibazarapp.data.models.AiRouteSelectionRequest
import com.example.regibazarapp.data.models.AiRouteSelectionResponse
import com.example.regibazarapp.data.models.BulkGeocodeRequest
import com.example.regibazarapp.data.models.BulkGeocodeResultDto
import com.example.regibazarapp.data.models.ClientDto
import com.example.regibazarapp.data.models.CreateRouteRequest
import com.example.regibazarapp.data.models.CreateRouteResponse
import com.example.regibazarapp.data.models.OrderSummaryDto
import com.example.regibazarapp.data.models.PreviewRouteRequest
import com.example.regibazarapp.data.models.PreviewRouteResponse
import com.example.regibazarapp.data.models.RecomposeRouteRequest
import com.example.regibazarapp.data.models.RecomposeRouteResponse
import com.example.regibazarapp.data.models.RouteListDto
import com.example.regibazarapp.data.models.RoutePackageRequirementDto
import com.example.regibazarapp.data.models.SetClientCoordinatesRequest
import com.example.regibazarapp.data.models.SetPackagesRequest
import com.example.regibazarapp.data.network.AdminApi
import com.example.regibazarapp.data.network.NetworkModule
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Response

sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data class Error(
        val message: String,
        val statusCode: Int? = null,
        val code: String? = null,
        val packageRequirements: List<RoutePackageRequirementDto> = emptyList()
    ) : RepositoryResult<Nothing>
}

private data class ApiErrorBody(
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("detail") val detail: String?,
    @SerializedName("orders") val orders: List<RoutePackageRequirementDto>?
)

private data class ParsedApiError(
    val message: String,
    val code: String? = null,
    val packageRequirements: List<RoutePackageRequirementDto> = emptyList()
)

class AdminRoutesRepository(
    private val api: AdminApi = NetworkModule.adminApi,
    private val gson: Gson = Gson()
) {
    suspend fun getRoutes(): RepositoryResult<List<RouteListDto>> =
        request { api.getRoutes() }

    suspend fun getRoute(routeId: Int): RepositoryResult<RouteListDto> =
        request { api.getRoute(routeId) }

    suspend fun getOrders(): RepositoryResult<List<OrderSummaryDto>> =
        request { api.getOrders() }

    suspend fun getAvailableTandas(): RepositoryResult<List<AvailableTandaDto>> =
        request { api.getAvailableTandas() }

    suspend fun getClients(): RepositoryResult<List<ClientDto>> =
        request { api.getClients() }

    suspend fun selectRouteWithCami(
        command: String,
        availableOrders: List<OrderSummaryDto>
    ): RepositoryResult<AiRouteSelectionResponse> = request {
        api.selectRouteWithCami(
            AiRouteSelectionRequest(
                voiceCommand = command,
                availableOrders = availableOrders
            )
        )
    }

    suspend fun previewRoute(
        orderIds: List<Int>,
        tandaParticipantIds: List<String>,
        latitude: Double? = null,
        longitude: Double? = null
    ): RepositoryResult<PreviewRouteResponse> = request {
        api.previewRoute(
            PreviewRouteRequest(
                orderIds = orderIds,
                tandaParticipantIds = tandaParticipantIds,
                startLat = latitude,
                startLng = longitude
            )
        )
    }

    suspend fun createRoute(
        orderIds: List<Int>,
        tandaParticipantIds: List<String>,
        initialCash: Double? = null,
        initialBreakdown: com.example.regibazarapp.data.models.CashDenominations? = null,
        initialExchangeRate: Double? = null
    ): RepositoryResult<CreateRouteResponse> = request {
        api.createRoute(
            CreateRouteRequest(
                orderIds = orderIds,
                tandaParticipantIds = tandaParticipantIds,
                initialCash = initialCash,
                initialBreakdown = initialBreakdown,
                initialExchangeRate = initialExchangeRate
            )
        )
    }

    suspend fun setPackages(
        orderId: Int,
        totalPackages: Int,
        confirmed: Boolean = true
    ): RepositoryResult<OrderSummaryDto> = request {
        api.setPackages(orderId, SetPackagesRequest(totalPackages, confirmed))
    }

    suspend fun recomposeRoute(
        routeId: Int,
        orderIds: List<Int>,
        tandaParticipantIds: List<String>
    ): RepositoryResult<RecomposeRouteResponse> = request {
        api.recomposeRoute(
            routeId,
            RecomposeRouteRequest(orderIds, tandaParticipantIds)
        )
    }

    suspend fun bulkGeocode(clientIds: List<Int>): RepositoryResult<List<BulkGeocodeResultDto>> =
        request { api.bulkGeocodeClients(BulkGeocodeRequest(clientIds.distinct())) }

    suspend fun setClientCoordinates(
        clientId: Int,
        request: SetClientCoordinatesRequest
    ): RepositoryResult<Unit> = requestUnit { api.setClientCoordinates(clientId, request) }

    suspend fun optimizeRoute(
        routeId: Int,
        latitude: Double? = null,
        longitude: Double? = null
    ): RepositoryResult<Unit> = requestUnit {
        api.optimizeRoute(routeId, latitude, longitude)
    }

    suspend fun reorderRoute(routeId: Int, deliveryIds: List<Int>): RepositoryResult<Unit> =
        requestUnit { api.reorderRoute(routeId, deliveryIds) }

    suspend fun removeOrder(routeId: Int, orderId: Int): RepositoryResult<Unit> =
        requestUnit { api.removeOrder(routeId, orderId) }

    suspend fun removeTanda(routeId: Int, participantId: String): RepositoryResult<Unit> =
        requestUnit { api.removeTanda(routeId, participantId) }

    suspend fun deleteRoute(routeId: Int): RepositoryResult<Unit> =
        requestUnit { api.deleteRoute(routeId) }

    suspend fun liquidateRoute(routeId: Int): RepositoryResult<Unit> =
        requestUnit { api.liquidateRoute(routeId) }

    private suspend fun <T> request(call: suspend () -> Response<T>): RepositoryResult<T> =
        try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                RepositoryResult.Success(body)
            } else {
                val apiError = parseStructuredError(response)
                RepositoryResult.Error(
                    message = apiError.message,
                    statusCode = response.code(),
                    code = apiError.code,
                    packageRequirements = apiError.packageRequirements
                )
            }
        } catch (exception: Exception) {
            RepositoryResult.Error(
                exception.localizedMessage ?: "No se pudo conectar con Regi Bazar."
            )
        }

    private suspend fun requestUnit(call: suspend () -> Response<*>): RepositoryResult<Unit> =
        try {
            val response = call()
            if (response.isSuccessful) {
                RepositoryResult.Success(Unit)
            } else {
                val apiError = parseStructuredError(response)
                RepositoryResult.Error(
                    message = apiError.message,
                    statusCode = response.code(),
                    code = apiError.code,
                    packageRequirements = apiError.packageRequirements
                )
            }
        } catch (exception: Exception) {
            RepositoryResult.Error(
                exception.localizedMessage ?: "No se pudo conectar con Regi Bazar."
            )
        }

    private fun parseStructuredError(response: Response<*>): ParsedApiError {
        val raw = response.errorBody()?.string()
        if (raw.isNullOrBlank()) {
            return ParsedApiError("No se pudo procesar la solicitud.")
        }

        val error = runCatching {
            gson.fromJson(raw, ApiErrorBody::class.java)
        }.getOrNull()

        return ParsedApiError(
            message = error?.message?.takeIf(String::isNotBlank)
                ?: error?.detail?.takeIf(String::isNotBlank)
                ?: raw.trim('"'),
            code = error?.code,
            packageRequirements = error?.orders.orEmpty()
        )
    }

    private fun parseError(response: Response<*>): String {
        val raw = response.errorBody()?.string()
        if (raw.isNullOrBlank()) {
            return when (response.code()) {
                401 -> "Tu sesión expiró. Inicia sesión de nuevo."
                404 -> "No encontré el recurso solicitado."
                else -> "El servidor respondió con error ${response.code()}."
            }
        }

        return runCatching {
            gson.fromJson(raw, ApiErrorBody::class.java)
        }.getOrNull()?.let { error ->
            error.message?.takeIf(String::isNotBlank)
                ?: error.detail?.takeIf(String::isNotBlank)
        } ?: raw.trim('"')
    }
}
