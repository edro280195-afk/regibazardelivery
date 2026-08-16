package com.example.regibazarapp.ui.screens.routes

import com.example.regibazarapp.data.models.RouteListDto
import com.example.regibazarapp.ui.utils.filterRoutes

enum class RoutesListTab {
    PENDING,
    COMPLETED
}

enum class RouteTagType {
    TRANSFER,
    TANDA,
    MISSING_LOCATION
}

fun routesForTab(
    routes: List<RouteListDto>,
    query: String,
    tab: RoutesListTab
): List<RouteListDto> {
    val searchedRoutes = filterRoutes(routes, query, status = null)
    return searchedRoutes.filter { route ->
        val isClosed = route.status.equals("Completed", ignoreCase = true) ||
            route.status.equals("Canceled", ignoreCase = true)

        when (tab) {
            RoutesListTab.PENDING -> !isClosed
            RoutesListTab.COMPLETED -> isClosed
        }
    }
}

fun routeTagTypes(route: RouteListDto): List<RouteTagType> = buildList {
    if (route.deliveries.any { delivery ->
            delivery.paymentMethod?.contains("transfer", ignoreCase = true) == true
        }
    ) {
        add(RouteTagType.TRANSFER)
    }

    if (route.deliveries.any { it.isTandaDelivery }) {
        add(RouteTagType.TANDA)
    }

    if (route.deliveries.any { it.latitude == null || it.longitude == null }) {
        add(RouteTagType.MISSING_LOCATION)
    }
}

fun expectedRouteCollection(route: RouteListDto): Double =
    route.deliveries.sumOf { delivery -> delivery.balanceDue.coerceAtLeast(0.0) }

fun routeStatusLabel(status: String): String = when {
    status.equals("Active", ignoreCase = true) -> "En curso"
    status.equals("Completed", ignoreCase = true) -> "Completada"
    status.equals("Canceled", ignoreCase = true) -> "Cancelada"
    else -> "Pendiente"
}
