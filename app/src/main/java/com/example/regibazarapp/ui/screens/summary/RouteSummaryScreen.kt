package com.example.regibazarapp.ui.screens.summary

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.regibazarapp.data.models.CashDenominations
import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.data.models.RouteSettlementDto
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.theme.RegiBazar2026Tokens
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import com.example.regibazarapp.ui.utils.formatRouteMoney

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RouteSummaryScreen(
    routeName: String,
    deliveredCount: Int,
    failedCount: Int,
    totalCount: Int,
    totalCollected: Double,
    collectedByCash: Double,
    collectedByTransfer: Double,
    collectedByDeposit: Double,
    startedAt: String?,
    deliveries: List<RouteDeliveryDto> = emptyList(),
    fastestDoorTimeSecs: Long? = null,
    slowestDoorTimeSecs: Long? = null,
    token: String = "",
    initialCash: Double = 0.0,
    totalExpenses: Double = 0.0,
    settlement: RouteSettlementDto? = null,
    isSettling: Boolean = false,
    onSubmitSettlement: ((Double, CashDenominations, String?, (Boolean) -> Unit) -> Unit)? = null,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val palette = regiBazar2026Palette()
    val stats = remember(startedAt, deliveries, deliveredCount, totalCount, fastestDoorTimeSecs, slowestDoorTimeSecs) {
        buildRouteSummaryStats(
            startedAt = startedAt,
            deliveries = deliveries,
            deliveredCount = deliveredCount,
            totalCount = totalCount,
            fastestDoorTimeSecs = fastestDoorTimeSecs,
            slowestDoorTimeSecs = slowestDoorTimeSecs
        )
    }
    val completedDeliveries = remember(deliveries) { completedSummaryDeliveries(deliveries) }
    val shareText = remember(
        routeName,
        deliveredCount,
        failedCount,
        totalCount,
        totalCollected,
        collectedByCash,
        collectedByTransfer,
        collectedByDeposit,
        stats
    ) {
        routeSummaryShareText(
            routeName = routeName,
            deliveredCount = deliveredCount,
            failedCount = failedCount,
            totalCount = totalCount,
            totalCollected = totalCollected,
            collectedByCash = collectedByCash,
            collectedByTransfer = collectedByTransfer,
            collectedByDeposit = collectedByDeposit,
            stats = stats
        )
    }

    Scaffold(
        bottomBar = {
            SummaryBottomBar(
                palette = palette,
                onFinish = onFinish,
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Resumen de ruta, Regi Bazar")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir resumen"))
                }
            )
        },
        containerColor = palette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(palette.background),
            contentPadding = PaddingValues(
                start = RegiBazar2026Tokens.ScreenPadding,
                end = RegiBazar2026Tokens.ScreenPadding,
                top = 16.dp,
                bottom = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "summary-header") {
                SummaryHeader(
                    routeName = routeName,
                    palette = palette,
                    successRate = stats.successRate
                )
            }

            item(key = "summary-outcome") {
                OutcomeCard(
                    deliveredCount = deliveredCount,
                    failedCount = failedCount,
                    totalCount = totalCount,
                    stats = stats,
                    palette = palette
                )
            }

            item(key = "summary-collections") {
                CollectionCard(
                    totalCollected = totalCollected,
                    collectedByCash = collectedByCash,
                    collectedByTransfer = collectedByTransfer,
                    collectedByDeposit = collectedByDeposit,
                    palette = palette
                )
            }

            item(key = "summary-cash-settlement") {
                RouteCashSettlementCard(
                    token = token,
                    initialCash = initialCash,
                    collectedByCash = collectedByCash,
                    totalExpenses = totalExpenses,
                    settlement = settlement,
                    isSettling = isSettling,
                    palette = palette,
                    onSubmitSettlement = onSubmitSettlement
                )
            }

            item(key = "summary-performance") {
                PerformanceCard(
                    totalCount = totalCount,
                    stats = stats,
                    palette = palette
                )
            }

            if (completedDeliveries.isNotEmpty()) {
                item(key = "timeline-label") {
                    SectionLabel(
                        title = "CIERRE DE PARADAS",
                        count = completedDeliveries.size,
                        palette = palette
                    )
                }
                itemsIndexed(
                    items = completedDeliveries,
                    key = { _, delivery -> delivery.deliveryId }
                ) { index, delivery ->
                    TimelineItem(
                        delivery = delivery,
                        isLast = index == completedDeliveries.lastIndex,
                        palette = palette
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(
    routeName: String,
    successRate: Int,
    palette: RegiBazar2026Palette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.successSoft,
            contentColor = palette.success,
            modifier = Modifier.size(82.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Ruta completada",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp
            ),
            color = palette.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            "$successRate% de efectividad",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center
        )
        if (routeName.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                routeName,
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OutcomeCard(
    deliveredCount: Int,
    failedCount: Int,
    totalCount: Int,
    stats: RouteSummaryStats,
    palette: RegiBazar2026Palette
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Resultado",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                SummaryMetric(
                    label = "Entregadas",
                    value = deliveredCount.toString(),
                    icon = Icons.Rounded.CheckCircle,
                    color = palette.success,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = "Fallidas",
                    value = failedCount.toString(),
                    icon = Icons.Rounded.Cancel,
                    color = if (failedCount > 0) palette.error else palette.textSecondary,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = "Total",
                    value = totalCount.toString(),
                    icon = Icons.Rounded.LocalShipping,
                    color = palette.accent,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            ProgressBar(
                fraction = stats.successRate / 100f,
                color = if (stats.successRate >= 90) palette.success else palette.warning,
                palette = palette
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    palette: RegiBazar2026Palette,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CollectionCard(
    totalCollected: Double,
    collectedByCash: Double,
    collectedByTransfer: Double,
    collectedByDeposit: Double,
    palette: RegiBazar2026Palette
) {
    val totalForRows = (collectedByCash + collectedByTransfer + collectedByDeposit).coerceAtLeast(1.0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = palette.moneySoft,
                    contentColor = palette.money,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(23.dp))
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Cobros de la jornada",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Separado por método de pago",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                Text(
                    formatRouteMoney(totalCollected),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = palette.money
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = palette.divider
            )

            if (totalCollected <= 0.0) {
                Text(
                    "No se registraron cobros en esta ruta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (collectedByCash > 0) {
                        CollectionRow(
                            label = "Efectivo",
                            amount = collectedByCash,
                            fraction = (collectedByCash / totalForRows).toFloat(),
                            color = palette.money,
                            palette = palette
                        )
                    }
                    if (collectedByTransfer > 0) {
                        CollectionRow(
                            label = "Transferencia",
                            amount = collectedByTransfer,
                            fraction = (collectedByTransfer / totalForRows).toFloat(),
                            color = palette.accent,
                            palette = palette
                        )
                    }
                    if (collectedByDeposit > 0) {
                        CollectionRow(
                            label = "Depósito",
                            amount = collectedByDeposit,
                            fraction = (collectedByDeposit / totalForRows).toFloat(),
                            color = palette.success,
                            palette = palette
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionRow(
    label: String,
    amount: Double,
    fraction: Float,
    color: Color,
    palette: RegiBazar2026Palette
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatRouteMoney(amount),
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(palette.surfaceMuted, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                    .fillMaxHeight()
                    .background(color, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerformanceCard(
    totalCount: Int,
    stats: RouteSummaryStats,
    palette: RegiBazar2026Palette
) {
    val metrics = buildList {
        if (stats.routeDuration.isNotBlank()) {
            add(SummaryChip(Icons.Rounded.Timer, "Tiempo total", stats.routeDuration, palette.textPrimary))
        }
        stats.avgMinutesPerDelivery?.let {
            add(SummaryChip(Icons.Rounded.Speed, "Promedio", "$it min", palette.accent))
        }
        stats.avgDoorTimeMinutes?.let {
            add(SummaryChip(Icons.Rounded.DoorFront, "En puerta", "$it min", palette.success))
        }
        stats.fastestDoorTimeMinutes?.let {
            add(SummaryChip(Icons.Rounded.FlashOn, "Más rápida", "$it min", palette.money))
        }
        stats.slowestDoorTimeMinutes?.let {
            add(SummaryChip(Icons.Rounded.HourglassBottom, "Más lenta", "$it min", palette.error))
        }
        if (stats.totalPiecesDelivered > 0) {
            add(SummaryChip(Icons.Rounded.Inventory2, "Piezas", stats.totalPiecesDelivered.toString(), palette.accent))
        }
        add(SummaryChip(Icons.AutoMirrored.Rounded.ReceiptLong, "Paradas", totalCount.toString(), palette.textPrimary))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Rendimiento",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metrics.forEach { metric ->
                    PerformanceChip(metric, palette)
                }
            }
        }
    }
}

private data class SummaryChip(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val color: Color
)

@Composable
private fun PerformanceChip(metric: SummaryChip, palette: RegiBazar2026Palette) {
    Row(
        modifier = Modifier
            .background(metric.color.copy(alpha = 0.11f), RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(metric.icon, contentDescription = null, tint = metric.color, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Column {
            Text(
                metric.value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = metric.color
            )
            Text(
                metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun SectionLabel(
    title: String,
    count: Int,
    palette: RegiBazar2026Palette
) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = palette.textSecondary
        )
        Spacer(Modifier.weight(1f))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.accent
        )
    }
}

@Composable
private fun TimelineItem(
    delivery: RouteDeliveryDto,
    isLast: Boolean,
    palette: RegiBazar2026Palette
) {
    val isDelivered = delivery.status.equals("Delivered", ignoreCase = true)
    val color = if (isDelivered) palette.success else palette.error
    val timeText = remember(delivery.deliveredAt, delivery.arrivedAt) {
        deliveryEventTimeLabel(delivery)
    }
    val doorMinutes = remember(delivery.arrivedAt, delivery.deliveredAt) {
        deliveryDoorMinutes(delivery)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, CircleShape)
                )
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(42.dp)
                            .background(palette.divider)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    delivery.clientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (timeText.isNotBlank()) {
                        Text(
                            timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Text(
                        if (isDelivered) "Entregado" else "No entregado",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
                doorMinutes?.let {
                    Text(
                        "$it min en puerta",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            if (isDelivered && delivery.balanceDue <= 0.0) {
                Text(
                    formatRouteMoney(delivery.total),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.money
                )
            }
        }
    }
}

@Composable
private fun SummaryBottomBar(
    palette: RegiBazar2026Palette,
    onFinish: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        color = palette.surface,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color(0xFFF8FAFF)
                )
            ) {
                Text("Finalizar jornada", fontWeight = FontWeight.ExtraBold)
            }

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(RegiBazar2026Tokens.ControlRadius),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.accent.copy(alpha = 0.32f))
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartir resumen", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProgressBar(
    fraction: Float,
    color: Color,
    palette: RegiBazar2026Palette
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(palette.surfaceMuted, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, CircleShape)
        )
    }
}
