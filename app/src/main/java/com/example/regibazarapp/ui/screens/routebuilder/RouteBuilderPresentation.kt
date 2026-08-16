package com.example.regibazarapp.ui.screens.routebuilder

import com.example.regibazarapp.data.models.AvailableTandaDto
import com.example.regibazarapp.data.models.OrderSummaryDto
import com.example.regibazarapp.data.models.PreviewRouteResponse
import com.example.regibazarapp.data.models.PreviewStopDto
import com.example.regibazarapp.ui.utils.formatRouteMoney

enum class RouteBuilderPaymentTone {
    MONEY,
    TRANSFER,
    SETTLED,
    TANDA
}

data class RouteBuilderPaymentInfo(
    val label: String,
    val amount: Double,
    val tone: RouteBuilderPaymentTone
)

data class RouteBuilderSelectionSummary(
    val selectedOrders: Int,
    val selectedTandas: Int,
    val selectedStops: Int,
    val missingCoordinates: Int,
    val expectedCollection: Double
)

fun orderCandidatePaymentInfo(order: OrderSummaryDto): RouteBuilderPaymentInfo {
    val pendingAmount = when {
        order.balanceDue > 0.01 -> order.balanceDue
        order.amountPaid <= 0.01 -> order.total.coerceAtLeast(0.0)
        else -> 0.0
    }

    return when {
        pendingAmount > 0.01 -> RouteBuilderPaymentInfo(
            label = "${formatRouteMoney(pendingAmount)} por cobrar",
            amount = pendingAmount,
            tone = RouteBuilderPaymentTone.MONEY
        )

        order.paymentMethod?.contains("transfer", ignoreCase = true) == true -> {
            RouteBuilderPaymentInfo(
                label = "Transferencia cubierta",
                amount = 0.0,
                tone = RouteBuilderPaymentTone.TRANSFER
            )
        }

        else -> RouteBuilderPaymentInfo(
            label = "Sin cobro pendiente",
            amount = 0.0,
            tone = RouteBuilderPaymentTone.SETTLED
        )
    }
}

fun tandaCandidatePaymentInfo(tanda: AvailableTandaDto): RouteBuilderPaymentInfo =
    RouteBuilderPaymentInfo(
        label = "${tanda.tandaName} · semana ${tanda.week}/${tanda.totalWeeks}",
        amount = 0.0,
        tone = RouteBuilderPaymentTone.TANDA
    )

fun routeBuilderSelectionSummary(state: RouteBuilderUiState): RouteBuilderSelectionSummary {
    val selectedOrders = state.orders.filter { order -> order.id in state.selectedOrderIds }
    val selectedTandas = state.tandas.filter { tanda -> tanda.tandaParticipantId in state.selectedTandaIds }

    return RouteBuilderSelectionSummary(
        selectedOrders = selectedOrders.size,
        selectedTandas = selectedTandas.size,
        selectedStops = selectedOrders.size + selectedTandas.size,
        missingCoordinates = selectedOrders.count { !it.hasCoordinates } + selectedTandas.count { !it.hasCoordinates },
        expectedCollection = selectedOrders.sumOf { orderCandidatePaymentInfo(it).amount }
    )
}

fun orderProductSummary(order: OrderSummaryDto): String {
    val namedProducts = order.items
        .map { item -> item.productName.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(2)

    return when {
        namedProducts.isNotEmpty() && order.items.size > namedProducts.size ->
            namedProducts.joinToString(" · ") + " +${order.items.size - namedProducts.size}"

        namedProducts.isNotEmpty() -> namedProducts.joinToString(" · ")
        order.itemsCount > 0 -> "${order.itemsCount} productos"
        else -> "Pedido sin detalle de productos"
    }
}

fun tandaProductSummary(tanda: AvailableTandaDto): String =
    listOfNotNull(
        tanda.tandaProductName?.takeIf { it.isNotBlank() },
        tanda.variant?.takeIf { it.isNotBlank() }?.let { "Variante: $it" }
    ).joinToString(" · ").ifBlank { "Tanda sin producto capturado" }

fun previewExpectedCollection(preview: PreviewRouteResponse): Double =
    preview.stops
        .filterNot { stop -> stop.kind.equals("Tanda", ignoreCase = true) }
        .sumOf { stop -> stop.total.coerceAtLeast(0.0) }

fun previewStopPaymentLabel(stop: PreviewStopDto): String =
    if (stop.kind.equals("Tanda", ignoreCase = true)) {
        val name = stop.tandaName?.takeIf { it.isNotBlank() } ?: "Tanda"
        val week = stop.tandaWeek?.let { " · semana $it" }.orEmpty()
        "$name$week"
    } else {
        formatRouteMoney(stop.total)
    }
