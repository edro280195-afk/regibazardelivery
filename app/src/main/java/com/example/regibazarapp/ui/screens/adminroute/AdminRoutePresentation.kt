package com.example.regibazarapp.ui.screens.adminroute

import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.data.models.RouteListDto
import com.example.regibazarapp.ui.utils.formatRouteMoney

enum class AdminRouteStatusTone {
    ACTIVE,
    COMPLETED,
    CANCELED,
    PENDING
}

enum class StopPaymentTone {
    MONEY,
    SUCCESS,
    TANDA
}

data class StopPaymentInfo(
    val label: String,
    val tone: StopPaymentTone
)

fun adminRouteStatusLabel(status: String): String = when {
    status.equals("Active", ignoreCase = true) -> "En curso"
    status.equals("Completed", ignoreCase = true) -> "Completada"
    status.equals("Canceled", ignoreCase = true) -> "Cancelada"
    else -> "Pendiente"
}

fun adminRouteStatusTone(status: String): AdminRouteStatusTone = when {
    status.equals("Active", ignoreCase = true) -> AdminRouteStatusTone.ACTIVE
    status.equals("Completed", ignoreCase = true) -> AdminRouteStatusTone.COMPLETED
    status.equals("Canceled", ignoreCase = true) -> AdminRouteStatusTone.CANCELED
    else -> AdminRouteStatusTone.PENDING
}

fun adminExpectedCollection(route: RouteListDto): Double =
    route.deliveries.sumOf { delivery -> delivery.balanceDue.coerceAtLeast(0.0) }

fun stopPaymentInfo(delivery: RouteDeliveryDto): StopPaymentInfo = when {
    delivery.isTandaDelivery -> StopPaymentInfo(
        label = "${delivery.tandaName?.takeIf(String::isNotBlank) ?: "Tanda"} · no cobrar",
        tone = StopPaymentTone.TANDA
    )

    delivery.balanceDue > 0.01 -> StopPaymentInfo(
        label = "${formatRouteMoney(delivery.balanceDue)} por cobrar",
        tone = StopPaymentTone.MONEY
    )

    delivery.paymentMethod?.contains("transfer", ignoreCase = true) == true -> StopPaymentInfo(
        label = "${formatRouteMoney(0.0)} · transferencia",
        tone = StopPaymentTone.SUCCESS
    )

    else -> StopPaymentInfo(
        label = "Sin cobro pendiente",
        tone = StopPaymentTone.SUCCESS
    )
}

fun stopDisplayAddress(delivery: RouteDeliveryDto): String =
    delivery.displayAddress ?: "Sin dirección registrada"
