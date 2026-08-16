package com.example.regibazarapp.ui.utils

import com.example.regibazarapp.data.models.AvailableTandaDto
import com.example.regibazarapp.data.models.OrderSummaryDto
import com.example.regibazarapp.data.models.RouteListDto
import java.text.NumberFormat
import java.util.Locale

fun eligibleRouteOrders(
    orders: List<OrderSummaryDto>,
    editingRouteId: Int? = null
): List<OrderSummaryDto> = orders.filter { order ->
    order.status != "Canceled"
        && order.status != "Delivered"
        && order.orderType != "PickUp"
        && (order.deliveryRouteId == null || order.deliveryRouteId == editingRouteId)
}

fun filterRouteOrders(
    orders: List<OrderSummaryDto>,
    query: String
): List<OrderSummaryDto> {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return orders

    return orders.filter { order ->
        order.id.toString() == normalized.removePrefix("#")
            || order.clientName.lowercase().contains(normalized)
            || order.clientPhone.orEmpty().contains(normalized)
            || order.effectiveAddress.orEmpty().lowercase().contains(normalized)
            || order.items.any { it.productName.lowercase().contains(normalized) }
    }
}

fun filterAvailableTandas(
    tandas: List<AvailableTandaDto>,
    query: String
): List<AvailableTandaDto> {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return tandas

    return tandas.filter { tanda ->
        tanda.clientName.lowercase().contains(normalized)
            || tanda.tandaName.lowercase().contains(normalized)
            || tanda.tandaProductName.orEmpty().lowercase().contains(normalized)
            || tanda.clientPhone.orEmpty().contains(normalized)
    }
}

fun filterRoutes(routes: List<RouteListDto>, query: String, status: String?): List<RouteListDto> {
    val normalized = query.trim().lowercase()
    return routes.filter { route ->
        (status == null || route.status == status)
            && (
                normalized.isBlank()
                    || route.id.toString() == normalized.removePrefix("#")
                    || route.deliveries.any { delivery ->
                        delivery.clientName.lowercase().contains(normalized)
                            || delivery.clientPhone.orEmpty().contains(normalized)
                            || delivery.clientAddress.orEmpty().lowercase().contains(normalized)
                    }
                )
    }
}

fun formatRouteMoney(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(value)

fun formatRouteDistance(meters: Int): String =
    if (meters < 1_000) {
        "$meters m"
    } else {
        String.format(Locale.forLanguageTag("es-MX"), "%.1f km", meters / 1_000.0)
    }

fun formatRouteDuration(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
}
