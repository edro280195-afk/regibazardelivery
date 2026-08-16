package com.example.regibazarapp

import com.example.regibazarapp.data.models.AvailableTandaDto
import com.example.regibazarapp.data.models.OrderSummaryDto
import com.example.regibazarapp.data.models.PreviewRouteResponse
import com.example.regibazarapp.data.models.PreviewStopDto
import com.example.regibazarapp.ui.screens.routebuilder.RouteBuilderPaymentTone
import com.example.regibazarapp.ui.screens.routebuilder.RouteBuilderUiState
import com.example.regibazarapp.ui.screens.routebuilder.orderCandidatePaymentInfo
import com.example.regibazarapp.ui.screens.routebuilder.previewExpectedCollection
import com.example.regibazarapp.ui.screens.routebuilder.previewStopPaymentLabel
import com.example.regibazarapp.ui.screens.routebuilder.routeBuilderSelectionSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteBuilderPresentationTest {
    @Test
    fun selectionSummary_countsSelectedStopsAndMissingCoordinates() {
        val state = RouteBuilderUiState(
            orders = listOf(
                order(id = 1, balanceDue = 240.0, hasCoordinates = true),
                order(id = 2, balanceDue = 180.0, hasCoordinates = false)
            ),
            tandas = listOf(tanda(id = "tanda-1", hasCoordinates = false)),
            selectedOrderIds = setOf(1, 2),
            selectedTandaIds = setOf("tanda-1")
        )

        val summary = routeBuilderSelectionSummary(state)

        assertEquals(2, summary.selectedOrders)
        assertEquals(1, summary.selectedTandas)
        assertEquals(3, summary.selectedStops)
        assertEquals(2, summary.missingCoordinates)
        assertEquals(420.0, summary.expectedCollection, 0.0)
    }

    @Test
    fun orderCandidatePaymentInfo_prioritizesPendingBalance() {
        val pending = order(id = 7, total = 900.0, balanceDue = 250.0, amountPaid = 650.0)
        val transfer = order(id = 8, total = 500.0, amountPaid = 500.0, paymentMethod = "Transferencia")

        assertEquals(250.0, orderCandidatePaymentInfo(pending).amount, 0.0)
        assertEquals(RouteBuilderPaymentTone.MONEY, orderCandidatePaymentInfo(pending).tone)
        assertEquals(RouteBuilderPaymentTone.TRANSFER, orderCandidatePaymentInfo(transfer).tone)
    }

    @Test
    fun previewExpectedCollection_ignoresTandas() {
        val preview = PreviewRouteResponse(
            stops = listOf(
                previewStop(kind = "Order", total = 300.0),
                previewStop(kind = "Tanda", total = 999.0)
            ),
            totalDistanceMeters = 1_200,
            totalDurationSeconds = 900,
            optimizerSource = "test",
            skipped = emptyList(),
            stopsWithoutCoords = 0
        )

        assertEquals(300.0, previewExpectedCollection(preview), 0.0)
        assertEquals("Tanda LED · semana 4", previewStopPaymentLabel(preview.stops[1]))
    }

    private fun order(
        id: Int,
        total: Double = 100.0,
        balanceDue: Double = 0.0,
        amountPaid: Double = 0.0,
        paymentMethod: String? = null,
        hasCoordinates: Boolean = true
    ) = OrderSummaryDto(
        id = id,
        clientName = "Clienta $id",
        status = "Pending",
        total = total,
        orderType = "Delivery",
        createdAt = "2026-06-27T09:00:00",
        amountPaid = amountPaid,
        balanceDue = balanceDue,
        paymentMethod = paymentMethod,
        clientLatitude = if (hasCoordinates) 27.4 else null,
        clientLongitude = if (hasCoordinates) -99.5 else null
    )

    private fun tanda(
        id: String,
        hasCoordinates: Boolean
    ) = AvailableTandaDto(
        tandaParticipantId = id,
        tandaId = "tanda",
        tandaName = "Tanda LED",
        tandaProductName = "Luces LED",
        week = 4,
        totalWeeks = 10,
        variant = null,
        clientId = 11,
        clientName = "Participante",
        clientAddress = "Dirección",
        clientPhone = null,
        clientLatitude = if (hasCoordinates) 27.4 else null,
        clientLongitude = if (hasCoordinates) -99.5 else null,
        deliveryInstructions = null
    )

    private fun previewStop(
        kind: String,
        total: Double
    ) = PreviewStopDto(
        kind = kind,
        orderId = if (kind == "Order") 1 else null,
        tandaParticipantId = if (kind == "Tanda") "tanda-1" else null,
        sortOrder = if (kind == "Order") 1 else 2,
        clientName = "Clienta",
        clientAddress = "Dirección",
        latitude = 27.4,
        longitude = -99.5,
        total = total,
        hasCoords = true,
        tandaName = if (kind == "Tanda") "Tanda LED" else null,
        tandaWeek = if (kind == "Tanda") 4 else null
    )
}
