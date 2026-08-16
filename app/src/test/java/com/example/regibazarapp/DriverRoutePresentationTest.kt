package com.example.regibazarapp

import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.ui.screens.route.DriverPaymentTone
import com.example.regibazarapp.ui.screens.route.completedStops
import com.example.regibazarapp.ui.screens.route.deliveryProductSummary
import com.example.regibazarapp.ui.screens.route.driverPaymentSummary
import com.example.regibazarapp.ui.screens.route.nextRouteDelivery
import com.example.regibazarapp.ui.screens.route.routeProgressFraction
import org.junit.Assert.assertEquals
import org.junit.Test

class DriverRoutePresentationTest {
    @Test
    fun progress_countsDeliveredAndFailedAsProcessedStops() {
        assertEquals(7, completedStops(deliveredCount = 5, failedCount = 2, totalCount = 10))
        assertEquals(0.7f, routeProgressFraction(5, 2, 10), 0.001f)
        assertEquals(0f, routeProgressFraction(0, 0, 0), 0.001f)
    }

    @Test
    fun paymentSummary_distinguishesCollectionTandaAndSettledStops() {
        val collection = driverPaymentSummary(delivery(id = 1, balanceDue = 480.0))
        val tanda = driverPaymentSummary(delivery(id = 2, kind = "Tanda", tandaName = "LED semanal"))
        val settled = driverPaymentSummary(
            delivery(id = 3, balanceDue = 0.0, amountPaid = 580.0, paymentMethod = "Transferencia")
        )

        assertEquals(DriverPaymentTone.CASH_DUE, collection.tone)
        assertEquals(DriverPaymentTone.TANDA, tanda.tone)
        assertEquals("No cobrar", tanda.value)
        assertEquals(DriverPaymentTone.SETTLED, settled.tone)
    }

    @Test
    fun nextRouteDelivery_usesVisibleRouteOrderOverPreviousInTransit() {
        val previousInTransit = delivery(id = 1, sortOrder = 2, status = "InTransit")
        val promotedPending = delivery(id = 2, sortOrder = 1, status = "Pending")
        val delivered = delivery(id = 3, sortOrder = 0, status = "Delivered")

        assertEquals(2, nextRouteDelivery(listOf(previousInTransit, promotedPending, delivered))?.deliveryId)
    }

    @Test
    fun productSummary_prefersPhysicalPackages() {
        val delivery = delivery(id = 4).copy(
            packages = listOf(
                com.example.regibazarapp.data.models.OrderPackageDto(
                    id = "p1",
                    packageNumber = 1,
                    qrCodeValue = "QR-1",
                    status = "Loaded",
                    createdAt = "",
                    loadedAt = null,
                    deliveredAt = null
                )
            )
        )

        assertEquals("1 paquete", deliveryProductSummary(delivery))
    }

    private fun delivery(
        id: Int,
        balanceDue: Double = 0.0,
        amountPaid: Double = 0.0,
        paymentMethod: String? = null,
        kind: String = "Order",
        tandaName: String? = null,
        status: String = "Pending",
        sortOrder: Int = id
    ) = RouteDeliveryDto(
        deliveryId = id,
        orderId = id,
        sortOrder = sortOrder,
        clientName = "Clienta $id",
        clientAddress = "Dirección $id",
        latitude = 27.4,
        longitude = -99.5,
        status = status,
        total = 100.0,
        deliveredAt = null,
        notes = null,
        failureReason = null,
        evidenceUrls = emptyList(),
        clientPhone = null,
        paymentMethod = paymentMethod,
        payments = emptyList(),
        items = emptyList(),
        amountPaid = amountPaid,
        balanceDue = balanceDue,
        deliveryInstructions = null,
        arrivedAt = null,
        kind = kind,
        tandaName = tandaName
    )
}
