package com.example.regibazarapp

import com.example.regibazarapp.data.models.OrderSummaryDto
import com.example.regibazarapp.data.models.RouteListDto
import com.example.regibazarapp.ui.utils.eligibleRouteOrders
import com.example.regibazarapp.ui.utils.filterRouteOrders
import com.example.regibazarapp.ui.utils.filterRoutes
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteAdminUtilsTest {
    @Test
    fun eligibleRouteOrders_excludesDeliveredPickupAndOtherRoutes() {
        val orders = listOf(
            order(id = 1),
            order(id = 2, status = "Delivered"),
            order(id = 3, orderType = "PickUp"),
            order(id = 4, deliveryRouteId = 99),
            order(id = 5, deliveryRouteId = 7)
        )

        val result = eligibleRouteOrders(orders, editingRouteId = 7)

        assertEquals(listOf(1, 5), result.map { it.id })
    }

    @Test
    fun filterRouteOrders_matchesClientPhoneAndId() {
        val orders = listOf(
            order(id = 15, clientName = "Ana López", clientPhone = "8671234567"),
            order(id = 20, clientName = "Rosa")
        )

        assertEquals(listOf(15), filterRouteOrders(orders, "ana").map { it.id })
        assertEquals(listOf(15), filterRouteOrders(orders, "1234").map { it.id })
        assertEquals(listOf(20), filterRouteOrders(orders, "#20").map { it.id })
    }

    @Test
    fun filterRoutes_appliesStatusAndClientSearch() {
        val routes = listOf(
            route(id = 1, status = "Pending", clientName = "Ana"),
            route(id = 2, status = "Completed", clientName = "Rosa")
        )

        val result = filterRoutes(routes, "rosa", "Completed")

        assertEquals(listOf(2), result.map { it.id })
    }

    private fun order(
        id: Int,
        status: String = "Pending",
        orderType: String = "Delivery",
        deliveryRouteId: Int? = null,
        clientName: String = "Clienta",
        clientPhone: String? = null
    ) = OrderSummaryDto(
        id = id,
        clientName = clientName,
        status = status,
        total = 100.0,
        orderType = orderType,
        createdAt = "",
        clientPhone = clientPhone,
        deliveryRouteId = deliveryRouteId
    )

    private fun route(id: Int, status: String, clientName: String) = RouteListDto(
        id = id,
        driverToken = "token",
        driverLink = null,
        status = status,
        createdAt = "",
        startedAt = null,
        deliveries = listOf(
            com.example.regibazarapp.data.models.RouteDeliveryDto(
                deliveryId = id,
                orderId = id,
                sortOrder = 1,
                clientName = clientName,
                clientAddress = null,
                latitude = null,
                longitude = null,
                status = "Pending",
                total = 100.0,
                deliveredAt = null,
                notes = null,
                failureReason = null,
                evidenceUrls = emptyList(),
                clientPhone = null,
                paymentMethod = null,
                payments = emptyList(),
                items = emptyList(),
                amountPaid = 0.0,
                balanceDue = 100.0,
                deliveryInstructions = null,
                arrivedAt = null
            )
        )
    )
}
