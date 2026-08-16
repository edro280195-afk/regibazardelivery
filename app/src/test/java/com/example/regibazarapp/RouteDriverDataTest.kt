package com.example.regibazarapp

import com.example.regibazarapp.data.PendingAction
import com.example.regibazarapp.data.models.ChatMessageDto
import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteDriverDataTest {
    private val gson = Gson()

    @Test
    fun routeDelivery_acceptsWebAliasesForIdAddressAndPaymentType() {
        val delivery = gson.fromJson(
            """
            {
              "id": 42,
              "orderId": 7,
              "sortOrder": 1,
              "clientName": "Ana",
              "address": "Calle 1",
              "latitude": null,
              "longitude": null,
              "status": "Pending",
              "total": 250.0,
              "deliveredAt": null,
              "notes": null,
              "failureReason": null,
              "evidenceUrls": [],
              "clientPhone": "8671234567",
              "paymentType": "Transferencia",
              "payments": [],
              "items": [],
              "amountPaid": 50.0,
              "balanceDue": 200.0,
              "deliveryInstructions": null,
              "arrivedAt": null
            }
            """.trimIndent(),
            RouteDeliveryDto::class.java
        )

        assertEquals(42, delivery.deliveryId)
        assertEquals("Calle 1", delivery.clientAddress)
        assertEquals("Calle 1", delivery.displayAddress)
        assertEquals("Transferencia", delivery.paymentMethod)
    }

    @Test
    fun displayAddress_prefersAlternativeAddress() {
        val delivery = routeDelivery(clientAddress = "Direccion original", alternativeAddress = "Direccion corregida")

        assertEquals("Direccion corregida", delivery.displayAddress)
    }

    @Test
    fun requiresPayment_isFalseForTandaAndPaidDeliveries() {
        assertFalse(routeDelivery(kind = "Tanda", balanceDue = 500.0).requiresPayment)
        assertFalse(routeDelivery(balanceDue = 0.0).requiresPayment)
        assertTrue(routeDelivery(kind = "Order", balanceDue = 120.0).requiresPayment)
    }

    @Test
    fun chatMessage_acceptsMessageAlias() {
        val message = gson.fromJson(
            """{"id":1,"sender":"Client","message":"Hola","timestamp":"2026-06-22T10:00:00"}""",
            ChatMessageDto::class.java
        )

        assertEquals("Hola", message.text)
    }

    @Test
    fun pendingAction_readsLegacyOfflinePayloadWithoutEvidenceFields() {
        val listType = object : TypeToken<List<PendingAction>>() {}.type
        val actions: List<PendingAction> = gson.fromJson(
            """
            [
              {
                "id": "legacy-1",
                "type": "DELIVER",
                "driverToken": "driver",
                "deliveryId": 5,
                "reason": null,
                "paymentsJson": "[]",
                "timestamp": 100
              }
            ]
            """.trimIndent(),
            listType
        )

        val action = actions.single()
        assertNull(action.evidencePaths)
        assertNull(action.signatureSvgPath)
        assertEquals(emptyList<String>(), action.allEvidencePaths)
    }

    private fun routeDelivery(
        clientAddress: String? = "Calle 1",
        alternativeAddress: String? = null,
        kind: String = "Order",
        balanceDue: Double = 100.0
    ) = RouteDeliveryDto(
        deliveryId = 1,
        orderId = 10,
        sortOrder = 1,
        clientName = "Ana",
        clientAddress = clientAddress,
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
        balanceDue = balanceDue,
        deliveryInstructions = null,
        arrivedAt = null,
        alternativeAddress = alternativeAddress,
        kind = kind
    )
}
