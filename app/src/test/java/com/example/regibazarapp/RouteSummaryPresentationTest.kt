package com.example.regibazarapp

import com.example.regibazarapp.data.models.OrderItemDto
import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.ui.screens.summary.buildRouteSummaryStats
import com.example.regibazarapp.ui.screens.summary.completedSummaryDeliveries
import com.example.regibazarapp.ui.screens.summary.deliveryDoorMinutes
import com.example.regibazarapp.ui.screens.summary.routeSummaryShareText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSummaryPresentationTest {
    @Test
    fun summaryStats_calculatesOperationalMetrics() {
        val deliveries = listOf(
            delivery(
                id = 1,
                status = "Delivered",
                arrivedAt = "2026-06-27T09:10:00-05:00",
                deliveredAt = "2026-06-27T09:20:00-05:00",
                quantity = 2
            ),
            delivery(
                id = 2,
                status = "Delivered",
                arrivedAt = "2026-06-27T09:40:00-05:00",
                deliveredAt = "2026-06-27T10:00:00-05:00",
                quantity = 3
            )
        )

        val stats = buildRouteSummaryStats(
            startedAt = "2026-06-27T09:00:00-05:00",
            deliveries = deliveries,
            deliveredCount = 2,
            totalCount = 3,
            fastestDoorTimeSecs = 600,
            slowestDoorTimeSecs = 1_200
        )

        assertEquals(66, stats.successRate)
        assertEquals("1 h 0 min", stats.routeDuration)
        assertEquals(30L, stats.avgMinutesPerDelivery)
        assertEquals(15L, stats.avgDoorTimeMinutes)
        assertEquals(5, stats.totalPiecesDelivered)
        assertEquals(10L, deliveryDoorMinutes(deliveries.first()))
    }

    @Test
    fun completedSummaryDeliveries_sortsClosedStops() {
        val deliveries = listOf(
            delivery(id = 2, deliveredAt = "2026-06-27T10:00:00-05:00"),
            delivery(id = 1, deliveredAt = "2026-06-27T09:20:00-05:00")
        )

        val sorted = completedSummaryDeliveries(deliveries)

        assertEquals(listOf(1, 2), sorted.map { it.deliveryId })
    }

    @Test
    fun shareText_includesMoneyAndCoreCounts() {
        val stats = buildRouteSummaryStats(
            startedAt = null,
            deliveries = emptyList(),
            deliveredCount = 4,
            totalCount = 5,
            fastestDoorTimeSecs = null,
            slowestDoorTimeSecs = null
        )

        val text = routeSummaryShareText(
            routeName = "Ruta Norte",
            deliveredCount = 4,
            failedCount = 1,
            totalCount = 5,
            totalCollected = 1_250.0,
            collectedByCash = 750.0,
            collectedByTransfer = 500.0,
            collectedByDeposit = 0.0,
            stats = stats
        )

        assertTrue(text.contains("Ruta Norte"))
        assertTrue(text.contains("Entregas: 4 de 5"))
        assertTrue(text.contains("Total cobrado"))
        assertTrue(text.contains("Transferencia"))
    }

    private fun delivery(
        id: Int,
        status: String = "Delivered",
        arrivedAt: String? = null,
        deliveredAt: String? = null,
        quantity: Int = 1
    ) = RouteDeliveryDto(
        deliveryId = id,
        orderId = id,
        sortOrder = id,
        clientName = "Clienta $id",
        clientAddress = "Dirección $id",
        latitude = 27.4,
        longitude = -99.5,
        status = status,
        total = 100.0,
        deliveredAt = deliveredAt,
        notes = null,
        failureReason = null,
        evidenceUrls = emptyList(),
        clientPhone = null,
        paymentMethod = null,
        payments = emptyList(),
        items = listOf(
            OrderItemDto(
                id = id,
                productName = "Producto",
                quantity = quantity,
                unitPrice = 100.0,
                lineTotal = 100.0 * quantity
            )
        ),
        amountPaid = 100.0,
        balanceDue = 0.0,
        deliveryInstructions = null,
        arrivedAt = arrivedAt
    )
}
