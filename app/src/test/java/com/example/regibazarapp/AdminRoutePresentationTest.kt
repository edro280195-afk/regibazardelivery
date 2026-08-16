package com.example.regibazarapp

import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.data.models.RouteListDto
import com.example.regibazarapp.ui.screens.adminroute.StopPaymentTone
import com.example.regibazarapp.ui.screens.adminroute.adminExpectedCollection
import com.example.regibazarapp.ui.screens.adminroute.stopPaymentInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AdminRoutePresentationTest {
    @Test
    fun expectedCollection_ignoresNegativeBalances() {
        val route = route(
            deliveries = listOf(
                delivery(id = 1, balanceDue = 420.0),
                delivery(id = 2, balanceDue = -10.0)
            )
        )

        assertEquals(420.0, adminExpectedCollection(route), 0.0)
    }

    @Test
    fun stopPaymentInfo_prioritizesTandaAndPendingCollection() {
        val tanda = stopPaymentInfo(
            delivery(id = 1, kind = "Tanda", tandaName = "LED Premium", balanceDue = 500.0)
        )
        val collection = stopPaymentInfo(delivery(id = 2, balanceDue = 325.0))

        assertEquals(StopPaymentTone.TANDA, tanda.tone)
        assertEquals("LED Premium · no cobrar", tanda.label)
        assertEquals(StopPaymentTone.MONEY, collection.tone)
    }

    private fun route(deliveries: List<RouteDeliveryDto>) = RouteListDto(
        id = 85,
        driverToken = "token",
        driverLink = null,
        status = "Pending",
        createdAt = "2026-06-27T09:30:00",
        startedAt = null,
        deliveries = deliveries
    )

    private fun delivery(
        id: Int,
        kind: String = "Order",
        tandaName: String? = null,
        balanceDue: Double = 0.0
    ) = RouteDeliveryDto(
        deliveryId = id,
        orderId = id,
        sortOrder = id,
        clientName = "Clienta $id",
        clientAddress = "Dirección $id",
        latitude = 27.4,
        longitude = -99.5,
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
        balanceDue = balanceDue,
        deliveryInstructions = null,
        arrivedAt = null,
        kind = kind,
        tandaName = tandaName
    )
}
