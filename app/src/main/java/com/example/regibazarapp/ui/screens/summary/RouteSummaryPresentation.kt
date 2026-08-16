package com.example.regibazarapp.ui.screens.summary

import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.ui.utils.formatRouteMoney
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

data class RouteSummaryStats(
    val successRate: Int,
    val routeDuration: String,
    val avgMinutesPerDelivery: Long?,
    val avgDoorTimeMinutes: Long?,
    val fastestDoorTimeMinutes: Long?,
    val slowestDoorTimeMinutes: Long?,
    val totalPiecesDelivered: Int
)

fun buildRouteSummaryStats(
    startedAt: String?,
    deliveries: List<RouteDeliveryDto>,
    deliveredCount: Int,
    totalCount: Int,
    fastestDoorTimeSecs: Long?,
    slowestDoorTimeSecs: Long?
): RouteSummaryStats {
    val routeEndInstant = deliveries
        .mapNotNull { delivery -> delivery.deliveredAt ?: delivery.arrivedAt }
        .mapNotNull(::parseSummaryInstant)
        .maxOrNull()
    val startInstant = startedAt?.takeIf { it.isNotBlank() }?.let(::parseSummaryInstant)
    val routeMinutes = if (startInstant != null && routeEndInstant != null) {
        ChronoUnit.MINUTES.between(startInstant, routeEndInstant).coerceAtLeast(0)
    } else {
        null
    }
    val doorTimes = deliveries.mapNotNull(::deliveryDoorMinutes)

    return RouteSummaryStats(
        successRate = if (totalCount > 0) {
            (deliveredCount.toFloat() / totalCount * 100).toInt()
        } else {
            0
        },
        routeDuration = routeMinutes?.let(::formatMinutes).orEmpty(),
        avgMinutesPerDelivery = if (deliveredCount > 0 && routeMinutes != null) {
            (routeMinutes.coerceAtLeast(1).toDouble() / deliveredCount).toLong()
        } else {
            null
        },
        avgDoorTimeMinutes = doorTimes.takeIf { it.isNotEmpty() }?.average()?.toLong(),
        fastestDoorTimeMinutes = fastestDoorTimeSecs?.let { it / 60 },
        slowestDoorTimeMinutes = slowestDoorTimeSecs?.let { it / 60 },
        totalPiecesDelivered = deliveries
            .filter { it.status.equals("Delivered", ignoreCase = true) }
            .sumOf { delivery -> delivery.items?.sumOf { item -> item.quantity } ?: 0 }
    )
}

fun completedSummaryDeliveries(deliveries: List<RouteDeliveryDto>): List<RouteDeliveryDto> =
    deliveries
        .filter { delivery ->
            delivery.deliveredAt != null || delivery.status.equals("NotDelivered", ignoreCase = true)
        }
        .sortedBy { delivery ->
            parseSummaryInstant(delivery.deliveredAt.orEmpty())?.toEpochMilli() ?: Long.MAX_VALUE
        }

fun deliveryEventTimeLabel(delivery: RouteDeliveryDto): String =
    parseSummaryInstant(delivery.deliveredAt ?: delivery.arrivedAt.orEmpty())
        ?.atZone(ZoneId.systemDefault())
        ?.format(DateTimeFormatter.ofPattern("h:mm a"))
        .orEmpty()

fun deliveryDoorMinutes(delivery: RouteDeliveryDto): Long? {
    val arrived = delivery.arrivedAt?.let(::parseSummaryInstant) ?: return null
    val delivered = delivery.deliveredAt?.let(::parseSummaryInstant) ?: return null
    return ChronoUnit.MINUTES.between(arrived, delivered).coerceAtLeast(0)
}

fun routeSummaryShareText(
    routeName: String,
    deliveredCount: Int,
    failedCount: Int,
    totalCount: Int,
    totalCollected: Double,
    collectedByCash: Double,
    collectedByTransfer: Double,
    collectedByDeposit: Double,
    stats: RouteSummaryStats
): String = buildString {
    appendLine("Resumen de ruta, Regi Bazar")
    if (routeName.isNotBlank()) appendLine(routeName)
    appendLine()
    appendLine("Entregas: $deliveredCount de $totalCount (${stats.successRate}% éxito)")
    if (failedCount > 0) appendLine("Fallidas: $failedCount")
    appendLine("Total cobrado: ${formatRouteMoney(totalCollected)}")
    if (collectedByCash > 0) appendLine("Efectivo: ${formatRouteMoney(collectedByCash)}")
    if (collectedByTransfer > 0) appendLine("Transferencia: ${formatRouteMoney(collectedByTransfer)}")
    if (collectedByDeposit > 0) appendLine("Depósito: ${formatRouteMoney(collectedByDeposit)}")
    if (stats.routeDuration.isNotBlank()) appendLine("Tiempo total: ${stats.routeDuration}")
    stats.avgMinutesPerDelivery?.let { appendLine("Promedio por entrega: $it min") }
    stats.avgDoorTimeMinutes?.let { appendLine("Tiempo en puerta promedio: $it min") }
    if (stats.totalPiecesDelivered > 0) {
        appendLine("Piezas entregadas: ${stats.totalPiecesDelivered}")
    }
}

private fun parseSummaryInstant(value: String): java.time.Instant? {
    if (value.isBlank()) return null
    return try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(value.take(19)).atZone(ZoneId.systemDefault()).toInstant()
        } catch (_: Exception) {
            null
        }
    }
}

private fun formatMinutes(totalMinutes: Long): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours} h ${minutes} min" else "${minutes} min"
}
