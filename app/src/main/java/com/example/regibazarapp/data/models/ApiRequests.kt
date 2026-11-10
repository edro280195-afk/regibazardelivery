package com.example.regibazarapp.data.models

import com.google.gson.annotations.SerializedName

/**
 * ══════════════════════════════════════════════
 *  Request DTOs para la API del chofer
 * ══════════════════════════════════════════════
 */

data class UpdateLocationRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class CompleteDeliveryRequest(
    @SerializedName("notes") val notes: String?,
    @SerializedName("paymentsJson") val paymentsJson: String?
)

data class PaymentInputDto(
    @SerializedName("amount") val amount: Double,
    @SerializedName("method") val method: String,
    @SerializedName("notes") val notes: String?
)

data class FailDeliveryRequest(
    @SerializedName("reason") val reason: String,
    @SerializedName("notes") val notes: String?
)

data class SendMessageRequest(
    @SerializedName("text") val text: String
)

data class DriverCamiRequest(
    @SerializedName("commandText") val commandText: String
)

data class DriverCamiResponse(
    @SerializedName("respuestaCami") val respuestaCami: String
)

// ── Escaneo de paquetes QR ──
data class ScanPackageRequest(
    @SerializedName("qrCodeValue") val qrCodeValue: String,
    @SerializedName("action") val action: String // "Load" | "Deliver"
)

data class ScanPackageResponse(
    @SerializedName("message") val message: String,
    @SerializedName("packageNumber") val packageNumber: Int?,
    @SerializedName("allLoaded") val allLoaded: Boolean?
)

// ── Respuestas genéricas ──
data class SimpleMessageResponse(
    @SerializedName("message") val message: String,
    @SerializedName("nextDeliveryId") val nextDeliveryId: Int?,
    @SerializedName("firstDeliveryId") val firstDeliveryId: Int?
)

data class CreateRouteRequest(
    @SerializedName("orderIds") val orderIds: List<Int>,
    @SerializedName("force") val force: Boolean = false,
    @SerializedName("tandaParticipantIds") val tandaParticipantIds: List<String> = emptyList(),
    @SerializedName("preOptimized") val preOptimized: Boolean = false
)

data class SetPackagesRequest(
    @SerializedName("totalPackages") val totalPackages: Int?,
    @SerializedName("confirmed") val confirmed: Boolean = true
)

data class PreviewRouteRequest(
    @SerializedName("orderIds") val orderIds: List<Int>,
    @SerializedName("tandaParticipantIds") val tandaParticipantIds: List<String>,
    @SerializedName("startLat") val startLat: Double? = null,
    @SerializedName("startLng") val startLng: Double? = null
)

data class RecomposeRouteRequest(
    @SerializedName("orderIds") val orderIds: List<Int>,
    @SerializedName("tandaParticipantIds") val tandaParticipantIds: List<String>
)

data class BulkGeocodeRequest(
    @SerializedName("clientIds") val clientIds: List<Int>
)

data class SetClientCoordinatesRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("address") val address: String?,
    @SerializedName("deliveryInstructions") val deliveryInstructions: String?
)

data class AiRouteSelectionRequest(
    @SerializedName("voiceCommand") val voiceCommand: String,
    @SerializedName("availableOrders") val availableOrders: List<OrderSummaryDto>
)

// ── Solicitudes de caja chica, tandas en ruta y corte de caja ──
data class StartRouteRequest(
    @SerializedName("initialCash") val initialCash: Double = 0.0,
    @SerializedName("exchangeRate") val exchangeRate: Double = 18.0,
    @SerializedName("breakdown") val breakdown: CashDenominations? = null
)

data class DriverTandaPaymentRequest(
    @SerializedName("deliveryId") val deliveryId: Int? = null,
    @SerializedName("tandaParticipantId") val tandaParticipantId: String? = null,
    @SerializedName("clientName") val clientName: String,
    @SerializedName("weekNumber") val weekNumber: Int = 1,
    @SerializedName("amount") val amount: Double,
    @SerializedName("paymentMethod") val paymentMethod: String = "Efectivo",
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("cashReceived") val cashReceived: Double? = null,
    @SerializedName("cashChange") val cashChange: Double? = null
)

data class RouteSettlementRequest(
    @SerializedName("finalCashActual") val finalCashActual: Double,
    @SerializedName("breakdown") val breakdown: CashDenominations? = null,
    @SerializedName("notes") val notes: String? = null
)
