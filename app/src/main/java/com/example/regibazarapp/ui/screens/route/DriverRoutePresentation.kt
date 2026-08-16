package com.example.regibazarapp.ui.screens.route

import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.ui.utils.formatRouteMoney

enum class DriverPaymentTone {
    CASH_DUE,
    TANDA,
    SETTLED
}

data class DriverPaymentSummary(
    val title: String,
    val value: String,
    val tone: DriverPaymentTone
)

fun driverPaymentSummary(delivery: RouteDeliveryDto): DriverPaymentSummary = when {
    delivery.isTandaDelivery -> DriverPaymentSummary(
        title = delivery.tandaName?.takeIf(String::isNotBlank) ?: "Tanda",
        value = "No cobrar",
        tone = DriverPaymentTone.TANDA
    )

    delivery.requiresPayment -> DriverPaymentSummary(
        title = "A cobrar en esta parada",
        value = formatRouteMoney(delivery.balanceDue),
        tone = DriverPaymentTone.CASH_DUE
    )

    else -> DriverPaymentSummary(
        title = if (delivery.paymentMethod?.contains("transfer", ignoreCase = true) == true) {
            "Pagado por transferencia"
        } else {
            "Saldo cubierto"
        },
        value = if (delivery.amountPaid > 0.01) {
            "${formatRouteMoney(delivery.amountPaid)} · liquidado"
        } else {
            "Sin cobro pendiente"
        },
        tone = DriverPaymentTone.SETTLED
    )
}

fun completedStops(deliveredCount: Int, failedCount: Int, totalCount: Int): Int =
    (deliveredCount + failedCount).coerceIn(0, totalCount.coerceAtLeast(0))

fun routeProgressFraction(
    deliveredCount: Int,
    failedCount: Int,
    totalCount: Int
): Float {
    if (totalCount <= 0) return 0f
    return completedStops(deliveredCount, failedCount, totalCount).toFloat() / totalCount
}

fun deliveryProductSummary(delivery: RouteDeliveryDto): String {
    val packageCount = delivery.packages?.size ?: 0
    if (packageCount > 0) {
        return "$packageCount paquete${if (packageCount == 1) "" else "s"}"
    }

    val itemCount = delivery.items?.sumOf { it.quantity } ?: 0
    return if (itemCount > 0) {
        "$itemCount pieza${if (itemCount == 1) "" else "s"}"
    } else {
        "Sin detalle"
    }
}

fun deliveryReference(delivery: RouteDeliveryDto): String =
    delivery.deliveryInstructions?.takeIf(String::isNotBlank)
        ?: delivery.notes?.takeIf(String::isNotBlank)
        ?: "Sin referencia"

fun openRouteDeliveries(deliveries: List<RouteDeliveryDto>): List<RouteDeliveryDto> =
    deliveries
        .filter { it.status == "Pending" || it.status == "InTransit" }
        .sortedWith(compareBy<RouteDeliveryDto> { it.sortOrder }.thenBy { it.deliveryId })

fun nextRouteDelivery(deliveries: List<RouteDeliveryDto>): RouteDeliveryDto? =
    openRouteDeliveries(deliveries).firstOrNull()
