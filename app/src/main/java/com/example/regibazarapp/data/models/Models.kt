package com.example.regibazarapp.data.models

import com.google.gson.annotations.SerializedName

/**
 * ══════════════════════════════════════════════════
 *  Regi Bazar – Data Models (mirroring API DTOs)
 * ══════════════════════════════════════════════════
 */

// ── Respuesta principal de GET /api/driver/{token} ──
data class RouteResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("status") val status: String,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("initialCash") val initialCash: Double? = null,
    @SerializedName("initialExchangeRate") val initialExchangeRate: Double? = null,
    @SerializedName("dailyExchangeRate") val dailyExchangeRate: Double? = null,
    @SerializedName("exchangeRateUpdatedAt") val exchangeRateUpdatedAt: String? = null,
    @SerializedName("finalCashActual") val finalCashActual: Double? = null,
    @SerializedName("settlementDifference") val settlementDifference: Double? = null,
    @SerializedName("settledAt") val settledAt: String? = null,
    @SerializedName("deliveries") val deliveries: List<RouteDeliveryDto>? = emptyList(),
    @SerializedName("expenses") val expenses: List<DriverExpenseDto>? = emptyList()
) {
    val safeDeliveries: List<RouteDeliveryDto> get() = deliveries.orEmpty()
    val safeExpenses: List<DriverExpenseDto> get() = expenses.orEmpty()
}

// ── Cada entrega dentro de la ruta ──
data class RouteDeliveryDto(
    @SerializedName(value = "deliveryId", alternate = ["id"]) val deliveryId: Int,
    @SerializedName("orderId") val orderId: Int?,
    @SerializedName("sortOrder") val sortOrder: Int,
    @SerializedName("clientName") val clientName: String,
    @SerializedName(value = "clientAddress", alternate = ["address"]) val clientAddress: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Double,
    @SerializedName("deliveredAt") val deliveredAt: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("failureReason") val failureReason: String?,
    @SerializedName("evidenceUrls") val evidenceUrls: List<String>? = emptyList(),
    @SerializedName("clientPhone") val clientPhone: String?,
    @SerializedName(value = "paymentMethod", alternate = ["paymentType"]) val paymentMethod: String?,
    @SerializedName("payments") val payments: List<OrderPaymentDto>?,
    @SerializedName("items") val items: List<OrderItemDto>?,
    @SerializedName("amountPaid") val amountPaid: Double,
    @SerializedName("balanceDue") val balanceDue: Double,
    @SerializedName("deliveryInstructions") val deliveryInstructions: String?,
    @SerializedName("arrivedAt") val arrivedAt: String?,
    @SerializedName("packages") val packages: List<OrderPackageDto>? = null,
    // ── Etiqueta y tipo de cliente (Feature #5) ──
    @SerializedName("clientTag") val clientTag: String? = null,
    @SerializedName("clientType") val clientType: String? = null,
    @SerializedName("alternativeAddress") val alternativeAddress: String? = null,
    @SerializedName("kind") val kind: String = "Order",
    @SerializedName("tandaParticipantId") val tandaParticipantId: String? = null,
    @SerializedName("tandaId") val tandaId: String? = null,
    @SerializedName("tandaName") val tandaName: String? = null,
    @SerializedName("tandaProductName") val tandaProductName: String? = null,
    @SerializedName("tandaWeek") val tandaWeek: Int? = null,
    @SerializedName("tandaTotalWeeks") val tandaTotalWeeks: Int? = null,
    @SerializedName("tandaVariant") val tandaVariant: String? = null,
    @SerializedName("clientId") val clientId: Int? = null,
    @SerializedName("activeTandas") val activeTandas: List<ActiveTandaInfoDto>? = null,
    @SerializedName("deliveredWithoutPayment") val deliveredWithoutPayment: Boolean = false,
    @SerializedName("unpaidReason") val unpaidReason: String? = null
) {
    val displayAddress: String?
        get() = alternativeAddress?.takeIf { it.isNotBlank() }
            ?: clientAddress?.takeIf { it.isNotBlank() }

    val isTandaDelivery: Boolean
        get() = kind.equals("Tanda", ignoreCase = true)

    val requiresPayment: Boolean
        get() = !isTandaDelivery && balanceDue > 0.01
}

// ── Artículo del pedido ──
data class OrderItemDto(
    @SerializedName("id") val id: Int,
    @SerializedName("productName") val productName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unitPrice") val unitPrice: Double,
    @SerializedName("lineTotal") val lineTotal: Double
)

// ── Paquete físico con QR para verificación ──
data class OrderPackageDto(
    @SerializedName("id") val id: String,
    @SerializedName("packageNumber") val packageNumber: Int,
    @SerializedName("qrCodeValue") val qrCodeValue: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("loadedAt") val loadedAt: String?,
    @SerializedName("deliveredAt") val deliveredAt: String?,
    @SerializedName("returnedAt") val returnedAt: String? = null
)

// ── Pago registrado ──
data class OrderPaymentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("orderId") val orderId: Int,
    @SerializedName("amount") val amount: Double,
    @SerializedName("method") val method: String,
    @SerializedName("date") val date: String,
    @SerializedName("registeredBy") val registeredBy: String,
    @SerializedName("notes") val notes: String?
)

// ── Mensaje de chat ──
data class ChatMessageDto(
    @SerializedName("id") val id: Int,
    @SerializedName("orderId") val orderId: Int? = null,
    @SerializedName("deliveryId") val deliveryId: Int? = null,
    @SerializedName("sender") val sender: String,
    @SerializedName(value = "text", alternate = ["message"]) val text: String,
    @SerializedName("timestamp") val timestamp: String
)

// ── Productos frecuentes (Feature #14) ──
data class CommonProductDto(
    @SerializedName("name") val name: String,
    @SerializedName("count") val count: Int,
    @SerializedName("typicalPrice") val price: Double
)

// ── Alerta proactiva de CAMI (Feature #7) ──
data class CamiAlertDto(
    @SerializedName("type") val type: String,
    @SerializedName("message") val message: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("relatedId") val relatedId: Int? = null
)

// ── Respuesta del briefing de ruta (Feature #6) ──
data class RouteBriefingResponse(
    @SerializedName("text") val text: String,
    @SerializedName("audioBase64") val audioBase64: String? = null
)

// ── Admin Auth ──
data class AdminLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AdminLoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("name") val name: String,
    @SerializedName("expiresAt") val expiresAt: String
)

// ── Inventario físico / cajas NFC ──
data class InventoryBoxSummaryDto(
    @SerializedName("id") val id: String,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("location") val location: String?,
    @SerializedName("isNfcBound") val isNfcBound: Boolean,
    @SerializedName("articleTypesCount") val articleTypesCount: Int,
    @SerializedName("totalUnits") val totalUnits: Int,
    @SerializedName("updatedAt") val updatedAt: String
)

data class InventoryItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("variant") val variant: String?,
    @SerializedName("barcode") val barcode: String?,
    @SerializedName("labelCode") val labelCode: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("updatedAt") val updatedAt: String
)

data class InventoryMovementDto(
    @SerializedName("id") val id: String,
    @SerializedName("inventoryItemId") val inventoryItemId: String?,
    @SerializedName("itemName") val itemName: String?,
    @SerializedName("type") val type: String,
    @SerializedName("quantityDelta") val quantityDelta: Int,
    @SerializedName("quantityAfter") val quantityAfter: Int,
    @SerializedName("note") val note: String?,
    @SerializedName("performedBy") val performedBy: String,
    @SerializedName("occurredAt") val occurredAt: String
)

data class InventoryBoxDto(
    @SerializedName("id") val id: String,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("location") val location: String?,
    @SerializedName("isNfcBound") val isNfcBound: Boolean,
    @SerializedName("nfcUrl") val nfcUrl: String,
    @SerializedName("items") val items: List<InventoryItemDto>,
    @SerializedName("movements") val movements: List<InventoryMovementDto>,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class CreateInventoryBoxDto(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("location") val location: String?
)

data class CreateInventoryItemDto(
    @SerializedName("name") val name: String,
    @SerializedName("variant") val variant: String?,
    @SerializedName("barcode") val barcode: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("note") val note: String?
)

data class AdjustInventoryItemDto(
    @SerializedName("quantityDelta") val quantityDelta: Int,
    @SerializedName("note") val note: String?
)

data class BindInventoryNfcDto(
    @SerializedName("tagUid") val tagUid: String
)

data class TransferInventoryItemsDto(
    @SerializedName("sourceBoxId") val sourceBoxId: String,
    @SerializedName("destinationBoxId") val destinationBoxId: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("note") val note: String?
)

data class InventoryBarcodeMatchDto(
    @SerializedName("boxId") val boxId: String,
    @SerializedName("boxCode") val boxCode: String,
    @SerializedName("boxName") val boxName: String,
    @SerializedName("location") val location: String?,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("variant") val variant: String?,
    @SerializedName("barcode") val barcode: String?,
    @SerializedName("scannableCode") val scannableCode: String,
    @SerializedName("quantity") val quantity: Int
)

data class InventoryCountItemDto(
    @SerializedName("inventoryItemId") val inventoryItemId: String,
    @SerializedName("actualQuantity") val actualQuantity: Int
)

data class CompleteInventoryCountDto(
    @SerializedName("items") val items: List<InventoryCountItemDto>,
    @SerializedName("note") val note: String?
)

// ── Lista de Rutas (Admin) ──
data class RouteListDto(
    @SerializedName("id") val id: Int,
    @SerializedName("driverToken") val driverToken: String,
    @SerializedName("driverLink") val driverLink: String?,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("deliveries") val deliveries: List<RouteDeliveryDto> = emptyList(),
    @SerializedName("expenses") val expenses: List<DriverExpenseDto>? = emptyList()
) {
    val safeExpenses: List<DriverExpenseDto> get() = expenses.orEmpty()
    val deliveredCount: Int get() = deliveries.count { it.status == "Delivered" }
    val failedCount: Int get() = deliveries.count { it.status == "NotDelivered" }
    val pendingCount: Int get() = deliveries.count { it.status == "Pending" || it.status == "InTransit" }
    val totalCount: Int get() = deliveries.size
    val totalAmount: Double get() = deliveries.sumOf { it.total }
}

data class DriverExpenseDto(
    @SerializedName("id") val id: Int,
    @SerializedName("deliveryRouteId") val deliveryRouteId: Int?,
    @SerializedName("amount") val amount: Double,
    @SerializedName("expenseType") val expenseType: String,
    @SerializedName("date") val date: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("evidencePath") val evidencePath: String?,
    @SerializedName("createdAt") val createdAt: String
)

data class OrderSummaryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("clientName") val clientName: String,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Double,
    @SerializedName("orderType") val orderType: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("link") val link: String = "",
    @SerializedName("itemsCount") val itemsCount: Int = 0,
    @SerializedName("type") val type: String = "",
    @SerializedName("clientPhone") val clientPhone: String? = null,
    @SerializedName("clientAddress") val clientAddress: String? = null,
    @SerializedName("alternativeAddress") val alternativeAddress: String? = null,
    @SerializedName("postponedAt") val postponedAt: String? = null,
    @SerializedName("postponedNote") val postponedNote: String? = null,
    @SerializedName("subtotal") val subtotal: Double = 0.0,
    @SerializedName("shippingCost") val shippingCost: Double = 0.0,
    @SerializedName("accessToken") val accessToken: String = "",
    @SerializedName("expiresAt") val expiresAt: String = "",
    @SerializedName("clientId") val clientId: Int? = null,
    @SerializedName("clientLatitude") val clientLatitude: Double? = null,
    @SerializedName("clientLongitude") val clientLongitude: Double? = null,
    @SerializedName("deliveryInstructions") val deliveryInstructions: String? = null,
    @SerializedName("deliveryRouteId") val deliveryRouteId: Int? = null,
    @SerializedName("items") val items: List<OrderItemDto> = emptyList(),
    @SerializedName("payments") val payments: List<OrderPaymentDto> = emptyList(),
    @SerializedName("amountPaid") val amountPaid: Double = 0.0,
    @SerializedName("balanceDue") val balanceDue: Double = 0.0,
    @SerializedName("advancePayment") val advancePayment: Double = 0.0,
    @SerializedName("paymentMethod") val paymentMethod: String? = null,
    @SerializedName("salesPeriodId") val salesPeriodId: Int? = null,
    @SerializedName("salesPeriodName") val salesPeriodName: String? = null,
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("discountAmount") val discountAmount: Double? = null,
    @SerializedName("scheduledDeliveryDate") val scheduledDeliveryDate: String? = null,
    @SerializedName("totalPackages") val totalPackages: Int? = null,
    @SerializedName("packagesConfirmed") val packagesConfirmed: Boolean = false,
    @SerializedName("clientFacebookProfileUrl") val clientFacebookProfileUrl: String? = null,
    @SerializedName("notifiedAt") val notifiedAt: String? = null,
    @SerializedName("clientPoints") val clientPoints: Int? = null,
    @SerializedName("deliveredWithoutPayment") val deliveredWithoutPayment: Boolean = false,
    @SerializedName("unpaidDeliveryReason") val unpaidDeliveryReason: String? = null,
    @SerializedName("deliveredWithoutPaymentAt") val deliveredWithoutPaymentAt: String? = null
) {
    val effectiveAddress: String?
        get() = alternativeAddress?.takeIf { it.isNotBlank() }
            ?: clientAddress?.takeIf { it.isNotBlank() }

    val hasCoordinates: Boolean
        get() = clientLatitude != null && clientLongitude != null
}

data class AvailableTandaDto(
    @SerializedName("tandaParticipantId") val tandaParticipantId: String,
    @SerializedName("tandaId") val tandaId: String,
    @SerializedName("tandaName") val tandaName: String,
    @SerializedName("tandaProductName") val tandaProductName: String?,
    @SerializedName("week") val week: Int,
    @SerializedName("totalWeeks") val totalWeeks: Int,
    @SerializedName("variant") val variant: String?,
    @SerializedName("clientId") val clientId: Int,
    @SerializedName("clientName") val clientName: String,
    @SerializedName("clientAddress") val clientAddress: String?,
    @SerializedName("clientPhone") val clientPhone: String?,
    @SerializedName("clientLatitude") val clientLatitude: Double?,
    @SerializedName("clientLongitude") val clientLongitude: Double?,
    @SerializedName("deliveryInstructions") val deliveryInstructions: String?
) {
    val hasCoordinates: Boolean
        get() = clientLatitude != null && clientLongitude != null
}

data class PreviewStopDto(
    @SerializedName("kind") val kind: String,
    @SerializedName("orderId") val orderId: Int?,
    @SerializedName("tandaParticipantId") val tandaParticipantId: String?,
    @SerializedName("sortOrder") val sortOrder: Int,
    @SerializedName("clientName") val clientName: String,
    @SerializedName("clientAddress") val clientAddress: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("total") val total: Double,
    @SerializedName("hasCoords") val hasCoords: Boolean,
    @SerializedName("tandaName") val tandaName: String?,
    @SerializedName("tandaWeek") val tandaWeek: Int?
)

data class SkippedStopDto(
    @SerializedName("kind") val kind: String,
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("reason") val reason: String
)

data class PreviewRouteResponse(
    @SerializedName("stops") val stops: List<PreviewStopDto>,
    @SerializedName("totalDistanceMeters") val totalDistanceMeters: Int,
    @SerializedName("totalDurationSeconds") val totalDurationSeconds: Int,
    @SerializedName("optimizerSource") val optimizerSource: String,
    @SerializedName("skipped") val skipped: List<SkippedStopDto>,
    @SerializedName("stopsWithoutCoords") val stopsWithoutCoords: Int,
    @SerializedName("polylineEncoded") val polylineEncoded: String? = null,
    @SerializedName("depotLatitude") val depotLatitude: Double? = null,
    @SerializedName("depotLongitude") val depotLongitude: Double? = null
)

data class CreateRouteResponse(
    @SerializedName("route") val route: RouteListDto,
    @SerializedName("skipped") val skipped: List<SkippedStopDto>
)

data class RoutePackageRequirementDto(
    @SerializedName("id") val id: Int,
    @SerializedName("clientName") val clientName: String,
    @SerializedName("totalPackages") val totalPackages: Int? = null
)

data class RecomposeRouteResponse(
    @SerializedName("route") val route: RouteListDto,
    @SerializedName("skipped") val skipped: List<SkippedStopDto>
)

data class BulkGeocodeResultDto(
    @SerializedName("clientId") val clientId: Int,
    @SerializedName("success") val success: Boolean,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("formattedAddress") val formattedAddress: String?,
    @SerializedName("error") val error: String?
)

data class ClientDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("tag") val tag: String,
    @SerializedName("ordersCount") val ordersCount: Int,
    @SerializedName("totalSpent") val totalSpent: Double,
    @SerializedName("type") val type: String,
    @SerializedName("deliveryInstructions") val deliveryInstructions: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?
)

data class AiRouteSelectionResponse(
    @SerializedName("selectedOrderIds") val selectedOrderIds: List<Int>,
    @SerializedName("aiConfirmationMessage") val aiConfirmationMessage: String,
    @SerializedName("audioBase64") val audioBase64: String? = null
)

// ── Desglose de billetes y efectivo (MXN y USD) ──
data class CashDenominations(
    @SerializedName("mxn20") var mxn20: Int = 0,
    @SerializedName("mxn50") var mxn50: Int = 0,
    @SerializedName("mxn100") var mxn100: Int = 0,
    @SerializedName("mxn200") var mxn200: Int = 0,
    @SerializedName("mxn500") var mxn500: Int = 0,
    @SerializedName("mxn1000") var mxn1000: Int = 0,
    @SerializedName("mxnCoins") var mxnCoins: Double = 0.0,
    @SerializedName("usd1") var usd1: Int = 0,
    @SerializedName("usd5") var usd5: Int = 0,
    @SerializedName("usd10") var usd10: Int = 0,
    @SerializedName("usd20") var usd20: Int = 0,
    @SerializedName("usd50") var usd50: Int = 0,
    @SerializedName("usd100") var usd100: Int = 0,
    @SerializedName("exchangeRate") var exchangeRate: Double = 18.0
) {
    val totalMxn: Double
        get() = (mxn20 * 20.0) + (mxn50 * 50.0) + (mxn100 * 100.0) +
                (mxn200 * 200.0) + (mxn500 * 500.0) + (mxn1000 * 1000.0) + mxnCoins

    val totalUsd: Double
        get() = ((usd1 * 1) + (usd5 * 5) + (usd10 * 10) +
                (usd20 * 20) + (usd50 * 50) + (usd100 * 100)).toDouble()

    val totalInMxn: Double
        get() = totalMxn + (totalUsd * exchangeRate)
}

data class ActiveTandaInfoDto(
    @SerializedName("participantId") val participantId: String,
    @SerializedName("tandaId") val tandaId: String,
    @SerializedName("tandaName") val tandaName: String,
    @SerializedName("weeklyAmount") val weeklyAmount: Double,
    @SerializedName("nextPendingWeek") val nextPendingWeek: Int,
    @SerializedName("totalWeeks") val totalWeeks: Int,
    @SerializedName("productName") val productName: String?,
    @SerializedName("variant") val variant: String?
)

data class RouteSettlementOrderPaymentDto(
    @SerializedName("orderId") val orderId: Int? = null,
    @SerializedName("deliveryId") val deliveryId: Int? = null,
    @SerializedName("clientName") val clientName: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("total") val total: Double? = 0.0,
    @SerializedName("amountPaid", alternate = ["amount"]) val amountPaid: Double? = 0.0,
    @SerializedName("method") val method: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("date") val date: String? = null
) {
    val amount: Double get() = amountPaid ?: 0.0
}

data class RouteSettlementTandaPaymentDto(
    @SerializedName("paymentId", alternate = ["id"]) val paymentId: String? = null,
    @SerializedName("clientName") val clientName: String? = null,
    @SerializedName("tandaName") val tandaName: String? = null,
    @SerializedName("weekNumber") val weekNumber: Int? = null,
    @SerializedName("amount") val amount: Double? = 0.0,
    @SerializedName("method") val method: String? = null,
    @SerializedName("isExtraIncome") val isExtraIncome: Boolean? = false,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("date") val date: String? = null
)

data class RouteSettlementDto(
    @SerializedName("routeId") val routeId: Int? = null,
    @SerializedName("routeName", alternate = ["driverName"]) val routeName: String? = null,
    @SerializedName("driverToken") val driverToken: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("settledAt") val settledAt: String? = null,
    @SerializedName("settlementStatus") val settlementStatus: String? = null,
    @SerializedName("initialCash") val initialCash: Double? = 0.0,
    @SerializedName("initialBreakdown", alternate = ["initialCashBreakdown"]) val initialBreakdown: CashDenominations? = null,
    @SerializedName("initialExchangeRate") val initialExchangeRate: Double? = 18.0,
    @SerializedName("expectedCash", alternate = ["expectedCashInHand"]) val expectedCash: Double? = 0.0,
    @SerializedName("actualCash", alternate = ["actualCashInHand"]) val actualCash: Double? = null,
    @SerializedName("finalBreakdown", alternate = ["finalCashBreakdown"]) val finalBreakdown: CashDenominations? = null,
    @SerializedName("difference") val difference: Double? = null,
    @SerializedName("notes", alternate = ["settlementNotes"]) val notes: String? = null,
    @SerializedName("totalDeliveries", alternate = ["ordersTotalCount"]) val totalDeliveries: Int? = 0,
    @SerializedName("deliveredCount", alternate = ["ordersDeliveredCount"]) val deliveredCount: Int? = 0,
    @SerializedName("failedCount") val failedCount: Int? = 0,
    @SerializedName("orderCashTotal", alternate = ["totalOrdersCash"]) val orderCashTotal: Double? = 0.0,
    @SerializedName("orderTransferTotal", alternate = ["totalOrdersTransfer"]) val orderTransferTotal: Double? = 0.0,
    @SerializedName("orderDepositTotal") val orderDepositTotal: Double? = 0.0,
    @SerializedName("orderTotal", alternate = ["totalOrdersCollected"]) val orderTotal: Double? = 0.0,
    @SerializedName("tandaCashTotal", alternate = ["totalTandasCash"]) val tandaCashTotal: Double? = 0.0,
    @SerializedName("tandaTransferTotal", alternate = ["totalTandasTransfer"]) val tandaTransferTotal: Double? = 0.0,
    @SerializedName("tandaTotal", alternate = ["totalTandasCollected"]) val tandaTotal: Double? = 0.0,
    @SerializedName("expensesTotal", alternate = ["totalExpenses"]) val expensesTotal: Double? = 0.0,
    @SerializedName("orderPayments") val orderPayments: List<RouteSettlementOrderPaymentDto>? = emptyList(),
    @SerializedName("tandaPayments") val tandaPayments: List<RouteSettlementTandaPaymentDto>? = emptyList(),
    @SerializedName("expenses") val expenses: List<DriverExpenseDto>? = emptyList(),
    @SerializedName("whatsAppShareText") val whatsAppShareText: String? = null,
    @SerializedName("webReportUrl") val webReportUrl: String? = null
) {
    val isSettled: Boolean
        get() = settledAt != null || settlementStatus == "Cuadrado" || settlementStatus == "Faltante" || settlementStatus == "Sobrante"
    val driverName: String? get() = routeName
    val initialCashBreakdown: CashDenominations? get() = initialBreakdown
    val finalCashBreakdown: CashDenominations? get() = finalBreakdown
    val settlementNotes: String? get() = notes
    val totalOrdersCash: Double get() = orderCashTotal ?: 0.0
    val totalOrdersTransfer: Double get() = orderTransferTotal ?: 0.0
    val totalOrdersCollected: Double get() = orderTotal ?: 0.0
    val totalTandasCash: Double get() = tandaCashTotal ?: 0.0
    val totalTandasTransfer: Double get() = tandaTransferTotal ?: 0.0
    val totalTandasCollected: Double get() = tandaTotal ?: 0.0
    val totalExpenses: Double get() = expensesTotal ?: 0.0
    val expectedCashInHand: Double get() = expectedCash ?: 0.0
    val actualCashInHand: Double get() = actualCash ?: 0.0
    val ordersDeliveredCount: Int get() = deliveredCount ?: 0
    val ordersTotalCount: Int get() = totalDeliveries ?: 0
}
