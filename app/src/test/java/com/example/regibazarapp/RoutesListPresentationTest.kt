package com.example.regibazarapp

import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.data.models.RouteListDto
import com.example.regibazarapp.ui.screens.routes.RouteTagType
import com.example.regibazarapp.ui.screens.routes.RoutesListTab
import com.example.regibazarapp.ui.screens.routes.expectedRouteCollection
import com.example.regibazarapp.ui.screens.routes.routeTagTypes
import com.example.regibazarapp.ui.screens.routes.routesForTab
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesListPresentationTest {
    @Test
    fun routesForTab_groupsActiveWithPendingAndCanceledWithCompleted() {
        val routes = listOf(
            route(1, "Pending"),
            route(2, "Active"),
            route(3, "Completed"),
            route(4, "Canceled")
        )

        assertEquals(
            listOf(1, 2),
            routesForTab(routes, "", RoutesListTab.PENDING).map { it.id }
        )
        assertEquals(
            listOf(3, 4),
            routesForTab(routes, "", RoutesListTab.COMPLETED).map { it.id }
        )
    }

    @Test
    fun routePresentation_derivesTagsAndExpectedCollectionFromDeliveries() {
        val route = route(
            id = 7,
            status = "Pending",
            deliveries = listOf(
                delivery(
                    id = 1,
                    paymentMethod = "Transferencia",
                    balanceDue = 250.0
                ),
                delivery(
                    id = 2,
                    kind = "Tanda",
                    latitude = 27.4,
                    longitude = -99.5,
                    balanceDue = -20.0
                )
            )
        )

        assertEquals(
            listOf(
                RouteTagType.TRANSFER,
                RouteTagType.TANDA,
                RouteTagType.MISSING_LOCATION
            ),
            routeTagTypes(route)
        )
        assertEquals(250.0, expectedRouteCollection(route), 0.0)
    }

    private fun route(
        id: Int,
        status: String,
        deliveries: List<RouteDeliveryDto> = listOf(delivery(id))
    ) = RouteListDto(
        id = id,
        driverToken = "token-$id",
        driverLink = null,
        status = status,
        createdAt = "2026-06-27T09:30:00",
        startedAt = null,
        deliveries = deliveries
    )

    private fun delivery(
        id: Int,
        paymentMethod: String? = null,
        kind: String = "Order",
        latitude: Double? = null,
        longitude: Double? = null,
        balanceDue: Double = 100.0
    ) = RouteDeliveryDto(
        deliveryId = id,
        orderId = id,
        sortOrder = id,
        clientName = "Clienta $id",
        clientAddress = "Dirección $id",
        latitude = latitude,
        longitude = longitude,
        status = "Pending",
        total = 100.0,
        deliveredAt = null,
        notes = null,
        failureReason = null,
        evidenceUrls = emptyList(),
        clientPhone = null,
        paymentMethod = paymentMethod,
        payments = emptyList(),
        items = emptyList(),
        amountPaid = 0.0,
        balanceDue = balanceDue,
        deliveryInstructions = null,
        arrivedAt = null,
        kind = kind
    )
}
