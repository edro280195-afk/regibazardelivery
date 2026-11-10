package com.example.regibazarapp.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.regibazarapp.data.models.PaymentInputDto
import com.example.regibazarapp.data.models.RouteDeliveryDto
import com.example.regibazarapp.ui.screens.route.DriverPaymentTone
import com.example.regibazarapp.ui.screens.route.deliveryProductSummary
import com.example.regibazarapp.ui.screens.route.deliveryReference
import com.example.regibazarapp.ui.screens.route.driverPaymentSummary
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.theme.RegiBazar2026Tokens
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import com.example.regibazarapp.ui.utils.formatRouteMoney
import kotlinx.coroutines.launch

private data class PaymentEntryUi(
    val amount: String,
    val method: String,
    val cashReceived: String = "",
    val notes: String = "",
    val billBreakdownSummary: String = ""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeliveryBottomSheetContent(
    delivery: RouteDeliveryDto?,
    driverToken: String,
    onDeliver: (List<PaymentInputDto>) -> Unit,
    onFail: () -> Unit,
    onTakePhoto: () -> Unit,
    onChat: () -> Unit,
    onNavigate: () -> Unit,
    onSignatureRequested: () -> Unit = {},
    hasSignature: Boolean = false,
    hasPhoto: Boolean = false,
    hasNewChatMessage: Boolean = false,
    totalStops: Int = 0,
    isDelivering: Boolean = false,
    onMarkInTransit: () -> Unit = {},
    onScanCompleted: () -> Unit = {},
    onExpense: () -> Unit = {},
    onRecordTandaPayment: ((com.example.regibazarapp.data.models.DriverTandaPaymentRequest) -> Unit)? = null,
    onDeliverWithoutPayment: ((String) -> Unit)? = null,
    exchangeRate: Double = 18.0,
    modifier: Modifier = Modifier
) {
    val palette = regiBazar2026Palette()

    if (delivery == null) {
        CompletedRouteSheet(palette, modifier)
        return
    }

    val context = LocalContext.current
    val requiresPayment = delivery.requiresPayment
    val initialMethod = remember(delivery.deliveryId) {
        when {
            delivery.paymentMethod?.contains("transfer", ignoreCase = true) == true ->
                "Transferencia"
            delivery.paymentMethod?.contains("dep", ignoreCase = true) == true ->
                "Depósito"
            else -> "Efectivo"
        }
    }
    val initialAmount = remember(delivery.deliveryId, delivery.balanceDue) {
        when {
            !requiresPayment -> ""
            delivery.balanceDue % 1.0 == 0.0 -> delivery.balanceDue.toInt().toString()
            else -> delivery.balanceDue.toString()
        }
    }
    val paymentEntries = remember(delivery.deliveryId) {
        mutableStateListOf(PaymentEntryUi(initialAmount, initialMethod))
    }
    var showScanner by remember(delivery.deliveryId) { mutableStateOf(false) }
    var detailsExpanded by remember(delivery.deliveryId) { mutableStateOf(false) }
    var showTandaDialog by remember(delivery.deliveryId) { mutableStateOf(false) }
    var selectedActiveTanda by remember(delivery.deliveryId) { mutableStateOf<com.example.regibazarapp.data.models.ActiveTandaInfoDto?>(null) }
    var showUnpaidDeliveryDialog by remember(delivery.deliveryId) { mutableStateOf(false) }
    var unpaidReasonText by remember(delivery.deliveryId) { mutableStateOf("") }
    val paymentSummary = remember(delivery) { driverPaymentSummary(delivery) }
    val parsedPayments = if (requiresPayment) {
        paymentEntries.mapNotNull { entry ->
            val amount = entry.amount.toDoubleOrNull() ?: return@mapNotNull null
            val cashR = entry.cashReceived.toDoubleOrNull()
            val change = if (cashR != null && entry.method == "Efectivo") (cashR - amount).coerceAtLeast(0.0) else null
            val note = when {
                cashR != null && change != null && entry.billBreakdownSummary.isNotBlank() ->
                    "Recibió: $" + cashR.toInt() + " (" + entry.billBreakdownSummary + ") | Cambio: $" + change.toInt()
                cashR != null && change != null ->
                    "Recibió: $" + cashR.toInt() + " | Cambio: $" + change.toInt()
                entry.notes.isNotBlank() -> entry.notes
                else -> null
            }
            amount.takeIf { it > 0 }?.let {
                PaymentInputDto(amount = it, method = entry.method, notes = note)
            }


        }
    } else {
        emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .animateContentSize()
        ) {
            SheetGrip(palette)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.accentSoft,
                    contentColor = palette.accent
                ) {
                    Text(
                        text = if (totalStops > 0) {
                            "Parada ${delivery.sortOrder} de $totalStops"
                        } else {
                            "Parada ${delivery.sortOrder}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                if (
                    delivery.clientType?.equals("Frecuente", ignoreCase = true) == true ||
                    delivery.clientTag == "VIP"
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.warningSoft,
                        contentColor = palette.warning
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Star,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (delivery.clientTag == "VIP") "VIP" else "Frecuente",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = onChat,
                        modifier = Modifier.size(RegiBazar2026Tokens.TouchTarget)
                    ) {
                        Icon(
                            Icons.Rounded.ChatBubbleOutline,
                            contentDescription = "Abrir chat con ${delivery.clientName}",
                            tint = palette.textSecondary
                        )
                    }
                    if (hasNewChatMessage) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-7).dp, y = 7.dp)
                                .size(9.dp)
                                .background(palette.error, CircleShape)
                        )
                    }
                }
            }

            Text(
                text = delivery.clientName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 25.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp
                ),
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp)
            )

            Text(
                text = delivery.displayAddress ?: "Sin dirección registrada",
                style = MaterialTheme.typography.bodyMedium,
                color = if (delivery.displayAddress == null) palette.warning else palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            PaymentSummaryCard(paymentSummary, palette)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeliveryMeta(
                    label = "Productos",
                    value = deliveryProductSummary(delivery),
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                DeliveryMeta(
                    label = "Estado",
                    value = when (delivery.status) {
                        "InTransit" -> "En camino"
                        "Delivered" -> "Entregada"
                        else -> "Pendiente"
                    },
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                DeliveryMeta(
                    label = "Referencia",
                    value = deliveryReference(delivery),
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryStopAction(
                    label = "Navegar",
                    icon = Icons.Rounded.Navigation,
                    foreground = palette.textPrimary,
                    background = palette.surfaceMuted,
                    onClick = onNavigate,
                    modifier = Modifier.weight(1f)
                )
                PrimaryStopAction(
                    label = "Avisar llegada",
                    icon = Icons.Rounded.ChatBubbleOutline,
                    foreground = Color(0xFF168A49),
                    background = Color(0xFFEAF7EF),
                    onClick = {
                        if (delivery.status == "Pending") onMarkInTransit()
                        notifyArrival(
                            clientName = delivery.clientName,
                            phone = delivery.clientPhone,
                            onFallback = onChat,
                            context = context
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!delivery.packages.isNullOrEmpty()) {
                PackageVerificationRow(
                    delivery = delivery,
                    palette = palette,
                    onScan = { showScanner = true }
                )
            }

            if (!delivery.activeTandas.isNullOrEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.accentSoft.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\uD83C\uDF38", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "CLIENTA EN TANDA ACTIVA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.accent
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        for (tanda in delivery.activeTandas) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        tanda.tandaName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        "Semana " + tanda.nextPendingWeek + "/" + tanda.totalWeeks + " · Cuota: $" + tanda.weeklyAmount.toInt(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }
                                Button(
                                    onClick = {
                                        selectedActiveTanda = tanda
                                        showTandaDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Abonar", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            if (requiresPayment) {
                PaymentEditor(
                    entries = paymentEntries,
                    balanceDue = delivery.balanceDue,
                    exchangeRate = exchangeRate,
                    palette = palette
                )
            }

            if (!delivery.items.isNullOrEmpty() || !delivery.deliveryInstructions.isNullOrBlank()) {
                TextButton(
                    onClick = { detailsExpanded = !detailsExpanded },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.textSecondary)
                ) {
                    Text(
                        text = if (detailsExpanded) "Ocultar detalle" else "Ver productos y notas",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        if (detailsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null
                    )
                }
                AnimatedVisibility(
                    visible = detailsExpanded,
                    enter = fadeIn(tween(160)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(170))
                ) {
                    DeliveryDetails(delivery, palette)
                }
            }

            Spacer(Modifier.height(10.dp))
        }

        val confirmationColor = if (paymentSummary.tone == DriverPaymentTone.CASH_DUE) {
            palette.money
        } else {
            palette.success
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 22.dp)
        ) {
            SwipeToDeliverButton(
                enabled = (!requiresPayment || parsedPayments.isNotEmpty()) && !isDelivering,
                isDelivering = isDelivering,
                label = if (requiresPayment) {
                    "Desliza para cobrar y entregar"
                } else {
                    "Desliza para confirmar entrega"
                },
                disabledLabel = if (isDelivering) "Confirmando entrega" else "Ingresa un pago",
                color = confirmationColor,
                palette = palette,
                onDeliver = { onDeliver(parsedPayments) }
            )

            if (requiresPayment && onDeliverWithoutPayment != null) {
                OutlinedButton(
                    onClick = {
                        unpaidReasonText = ""
                        showUnpaidDeliveryDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE11D48)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFDA4AF)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFE11D48)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Entregar sin recibir pago (Fiar)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE11D48)
                    )
                }
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 4
            ) {
                SecondaryStopAction(
                    label = if (hasPhoto) "Foto lista" else "Foto",
                    icon = if (hasPhoto) Icons.Rounded.Verified else Icons.Rounded.CameraAlt,
                    selected = hasPhoto,
                    palette = palette,
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f)
                )
                SecondaryStopAction(
                    label = if (hasSignature) "Firmada" else "Firma",
                    icon = if (hasSignature) Icons.Rounded.Verified else Icons.Rounded.Draw,
                    selected = hasSignature,
                    palette = palette,
                    onClick = onSignatureRequested,
                    modifier = Modifier.weight(1f)
                )
                SecondaryStopAction(
                    label = "Gasto",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    selected = false,
                    palette = palette,
                    onClick = onExpense,
                    modifier = Modifier.weight(1f)
                )
                SecondaryStopAction(
                    label = "Falla",
                    icon = Icons.Rounded.ReportProblem,
                    selected = false,
                    palette = palette,
                    danger = true,
                    onClick = onFail,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showScanner && !delivery.packages.isNullOrEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showScanner = false
                onScanCompleted()
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            PackageScannerOverlay(
                packages = delivery.packages.orEmpty(),
                driverToken = driverToken,
                action = "Deliver",
                onDismiss = {
                    showScanner = false
                    onScanCompleted()
                }
            )
        }
    }
    if (showTandaDialog) {
        DriverTandaDialog(
            deliveryId = delivery.deliveryId,
            clientName = delivery.clientName,
            activeTandas = delivery.activeTandas,
            initialSelectedTanda = selectedActiveTanda,
            onDismiss = { showTandaDialog = false },
            onConfirm = { req ->
                showTandaDialog = false
                onRecordTandaPayment?.invoke(req)
            }
        )
    }

    if (showUnpaidDeliveryDialog) {
        AlertDialog(
            onDismissRequest = { showUnpaidDeliveryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u26A0\uFE0F", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Entregar sin recibir pago",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF9F1239)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Se registrará la entrega y se notificará inmediatamente a la dueña de Regi Bazar que el pedido queda con saldo pendiente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = unpaidReasonText,
                        onValueChange = { unpaidReasonText = it },
                        label = { Text("Motivo / Observación (Ej. Paga por transf. después)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnpaidDeliveryDialog = false
                        onDeliverWithoutPayment?.invoke(unpaidReasonText.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirmar Entrega Sin Pago", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpaidDeliveryDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
@Composable
private fun SheetGrip(palette: RegiBazar2026Palette) {
    Box(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 10.dp)
            .width(42.dp)
            .height(5.dp)
            .clip(CircleShape)
            .background(palette.surfaceMuted)
    )
}

@Composable
private fun CompletedRouteSheet(
    palette: RegiBazar2026Palette,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(palette.successSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = palette.success,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Todas las paradas están procesadas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textPrimary
        )
        Text(
            "Revisa el resumen para cerrar la jornada.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )
    }
}

@Composable
private fun PaymentSummaryCard(
    summary: com.example.regibazarapp.ui.screens.route.DriverPaymentSummary,
    palette: RegiBazar2026Palette
) {
    val foreground: Color
    val background: Color
    val border: Color
    val icon: ImageVector

    when (summary.tone) {
        DriverPaymentTone.CASH_DUE -> {
            foreground = palette.money
            background = palette.moneySoft
            border = palette.money.copy(alpha = 0.22f)
            icon = Icons.Rounded.Payments
        }
        DriverPaymentTone.TANDA -> {
            foreground = palette.money
            background = palette.moneySoft
            border = palette.money.copy(alpha = 0.22f)
            icon = Icons.Rounded.Groups
        }
        DriverPaymentTone.SETTLED -> {
            foreground = palette.accent
            background = palette.accentSoft
            border = palette.accent.copy(alpha = 0.18f)
            icon = Icons.Rounded.SwapHoriz
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(foreground.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = foreground)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                summary.title,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )
            Text(
                summary.value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (summary.tone == DriverPaymentTone.SETTLED) 16.sp else 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = foreground
            )
        }
    }
}

@Composable
private fun DeliveryMeta(
    label: String,
    value: String,
    palette: RegiBazar2026Palette,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .height(58.dp)
            .background(palette.surfaceMuted.copy(alpha = 0.62f), RoundedCornerShape(13.dp))
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = palette.textSecondary,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PrimaryStopAction(
    label: String,
    icon: ImageVector,
    foreground: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = background,
        contentColor = foreground
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PackageVerificationRow(
    delivery: RouteDeliveryDto,
    palette: RegiBazar2026Palette,
    onScan: () -> Unit
) {
    val packages = delivery.packages.orEmpty()
    val unconfirmed = packages.count {
        it.status != "Delivered" && it.status != "Returned"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (unconfirmed == 0) palette.successSoft else palette.surfaceMuted.copy(alpha = 0.7f),
        contentColor = palette.textPrimary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (unconfirmed == 0) Icons.Rounded.Verified else Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = if (unconfirmed == 0) palette.success else palette.accent
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${packages.size} paquete${if (packages.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (unconfirmed == 0) "Verificación completa" else "$unconfirmed sin verificar",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
            TextButton(
                onClick = onScan,
                colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)
            ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text("Escanear", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PaymentEditor(
    entries: MutableList<PaymentEntryUi>,
    balanceDue: Double,
    exchangeRate: Double,
    palette: RegiBazar2026Palette
) {
    val totalEntered = entries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val remaining = balanceDue - totalEntered

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "PAGO A REGISTRAR",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = palette.textSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = when {
                    remaining > 0.01 -> "Faltan ${formatRouteMoney(remaining)}"
                    remaining < -0.01 -> "Excede ${formatRouteMoney(-remaining)}"
                    else -> "Monto completo"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    remaining > 0.01 -> palette.warning
                    remaining < -0.01 -> palette.error
                    else -> palette.success
                }
            )
        }

        entries.forEachIndexed { index, entry ->
            PaymentEntry(
                entry = entry,
                exchangeRate = exchangeRate,
                palette = palette,
                canRemove = entries.size > 1,
                onEntryChange = { entries[index] = it },
                onRemove = { entries.removeAt(index) },
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        TextButton(
            onClick = {
                val nextAmount = remaining.coerceAtLeast(0.0)
                entries.add(
                    PaymentEntryUi(
                        amount = if (nextAmount % 1.0 == 0.0) {
                            nextAmount.toInt().toString()
                        } else {
                            nextAmount.toString()
                        },
                        method = "Efectivo"
                    )
                )
            },
            colors = ButtonDefaults.textButtonColors(contentColor = palette.accent),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text("Dividir pago", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PaymentEntry(
    entry: PaymentEntryUi,
    exchangeRate: Double,
    palette: RegiBazar2026Palette,
    canRemove: Boolean,
    onEntryChange: (PaymentEntryUi) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier
) {
    var currencyTab by remember { mutableIntStateOf(0) } // 0: MXN, 1: USD

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surfaceMuted.copy(alpha = 0.44f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = entry.amount,
                onValueChange = { value ->
                    onEntryChange(entry.copy(amount = value.filter { it.isDigit() || it == '.' }))
                },
                modifier = Modifier.weight(1f),
                prefix = { Text("$", fontWeight = FontWeight.Bold) },
                label = { Text("Monto") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.surface,
                    unfocusedContainerColor = palette.surface,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.divider,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    cursorColor = palette.accent
                )
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Eliminar pago",
                        tint = palette.textSecondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Triple("Efectivo", Icons.Rounded.Payments, palette.money),
                Triple("Transferencia", Icons.Rounded.SwapHoriz, palette.accent),
                Triple("Depósito", Icons.Rounded.AccountBalance, palette.warning)
            ).forEach { (method, icon, color) ->
                val selected = entry.method == method
                OutlinedCard(
                    onClick = { onEntryChange(entry.copy(method = method)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(
                        if (selected) 1.5.dp else 1.dp,
                        if (selected) color else palette.divider
                    ),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selected) color.copy(alpha = 0.1f) else palette.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (selected) color else palette.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            if (method == "Transferencia") "Transfer." else method,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (selected) color else palette.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (entry.method == "Efectivo") {
            Spacer(Modifier.height(10.dp))

            // Selector MXN / USD y Tipo de Cambio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "¿Con cuánto paga la clienta?",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )

                Row(
                    modifier = Modifier
                        .background(palette.surface, RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        onClick = { currencyTab = 0 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (currencyTab == 0) palette.accent else Color.Transparent,
                        contentColor = if (currencyTab == 0) Color.White else palette.textSecondary
                    ) {
                        Text(
                            "MXN $",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        onClick = { currencyTab = 1 },
                        shape = RoundedCornerShape(8.dp),
                        color = if (currencyTab == 1) palette.accent else Color.Transparent,
                        contentColor = if (currencyTab == 1) Color.White else palette.textSecondary
                    ) {
                        Text(
                            "USD $",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (currencyTab == 1) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tipo de cambio: $" + exchangeRate.toInt() + " MXN por cada dólar",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = palette.accent
                )
            }

            Spacer(Modifier.height(8.dp))

            // Botones de billetes que SUMAN / ACUMULAN al presionar
            val denominations = if (currencyTab == 0) {
                listOf(20, 50, 100, 200, 500, 1000)
            } else {
                listOf(1, 5, 10, 20, 50, 100)
            }

            // Fila 1: Primeros 3 billetes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                denominations.take(3).forEach { denom ->
                    val label = if (currencyTab == 0) "+$" + denom else "+$" + denom + " U"
                    OutlinedButton(
                        onClick = {
                            val current = entry.cashReceived.toDoubleOrNull() ?: 0.0
                            val addVal = if (currencyTab == 0) denom.toDouble() else (denom * exchangeRate)
                            val updated = (current + addVal).toInt().toString()
                            val tag = if (currencyTab == 0) "+$" + denom else "+$" + denom + " USD"
                            val nextSummary = if (entry.billBreakdownSummary.isBlank()) tag else entry.billBreakdownSummary + ", " + tag
                            onEntryChange(entry.copy(cashReceived = updated, billBreakdownSummary = nextSummary))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary
                        ),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Fila 2: Siguientes 3 billetes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                denominations.drop(3).forEach { denom ->
                    val label = if (currencyTab == 0) {
                        if (denom >= 1000) "+$1,000" else "+$" + denom
                    } else {
                        "+$" + denom + " U"
                    }
                    OutlinedButton(
                        onClick = {
                            val current = entry.cashReceived.toDoubleOrNull() ?: 0.0
                            val addVal = if (currencyTab == 0) denom.toDouble() else (denom * exchangeRate)
                            val updated = (current + addVal).toInt().toString()
                            val tag = if (currencyTab == 0) (if (denom >= 1000) "+$1,000" else "+$" + denom) else "+$" + denom + " USD"
                            val nextSummary = if (entry.billBreakdownSummary.isBlank()) tag else entry.billBreakdownSummary + ", " + tag
                            onEntryChange(entry.copy(cashReceived = updated, billBreakdownSummary = nextSummary))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = palette.surface,
                            contentColor = palette.textPrimary
                        ),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Fila de acciones: "Cobro Exacto" y "Borrar"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        onEntryChange(entry.copy(cashReceived = entry.amount, billBreakdownSummary = "Exacto"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accentSoft,
                        contentColor = palette.accent
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Pago exacto ($" + entry.amount + ")", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onEntryChange(entry.copy(cashReceived = "", billBreakdownSummary = ""))
                    },
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = palette.textSecondary
                    ),
                    border = BorderStroke(1.dp, palette.divider),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Borrar (0)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Resumen de billetes ingresados si hay varios taps
            if (entry.billBreakdownSummary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Billetes: " + entry.billBreakdownSummary,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 2
                        )
                        IconButton(
                            onClick = { onEntryChange(entry.copy(billBreakdownSummary = "", cashReceived = "")) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(12.dp), tint = palette.textSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Campo numérico editable
            OutlinedTextField(
                value = entry.cashReceived,
                onValueChange = { value ->
                    onEntryChange(entry.copy(cashReceived = value.filter { it.isDigit() || it == '.' }))
                },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("$", fontWeight = FontWeight.Bold) },
                label = { Text("Efectivo recibido de la clienta") },
                placeholder = { Text("Ej. 550") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.surface,
                    unfocusedContainerColor = palette.surface,
                    focusedBorderColor = palette.money,
                    unfocusedBorderColor = palette.divider,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    cursorColor = palette.money
                )
            )

            // Indicador de cambio destacado
            val amountVal = entry.amount.toDoubleOrNull() ?: 0.0
            val receivedVal = entry.cashReceived.toDoubleOrNull()
            if (receivedVal != null && amountVal > 0) {
                val change = receivedVal - amountVal
                Spacer(Modifier.height(8.dp))
                if (change >= 0) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(1.5.dp, Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("\uD83D\uDCB5", fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "DAR DE CAMBIO:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = if (change == 0.0) "Cobro exacto (Sin cambio)" else formatRouteMoney(change),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp
                                    ),
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFEBEE),
                        border = BorderStroke(1.dp, Color(0xFFC62828)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("\u26A0\uFE0F", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Faltan " + formatRouteMoney(-change) + " para cubrir el monto",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryDetails(
    delivery: RouteDeliveryDto,
    palette: RegiBazar2026Palette
) {
    val hasNotes = !delivery.deliveryInstructions.isNullOrBlank()
    val productList = delivery.items.orEmpty()
    val hasContent = hasNotes || productList.isNotEmpty()
    if (!hasContent) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceMuted.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (hasNotes) {
                item(key = "delivery-notes") {
                    Text(
                        "Notas: ${delivery.deliveryInstructions.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.warning,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            items(
                items = productList,
                key = { item -> "${item.productName}-${item.lineTotal}" }
            ) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${item.quantity}×",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                        modifier = Modifier.width(30.dp)
                    )
                    Text(
                        item.productName,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatRouteMoney(item.lineTotal),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryStopAction(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    palette: RegiBazar2026Palette,
    danger: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val color = when {
        danger -> palette.error
        selected -> palette.success
        else -> palette.textSecondary
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) palette.successSoft else palette.surfaceMuted.copy(alpha = 0.55f),
        contentColor = color
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SwipeToDeliverButton(
    enabled: Boolean,
    isDelivering: Boolean,
    label: String,
    disabledLabel: String,
    color: Color,
    palette: RegiBazar2026Palette,
    onDeliver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val offsetX = remember { Animatable(0f) }
    var trackWidth by remember { mutableFloatStateOf(0f) }
    val handleSize = 52.dp
    val handleSizePx = with(density) { handleSize.toPx() }
    val maxOffset = (trackWidth - handleSizePx - 8f).coerceAtLeast(0f)
    val progress = if (maxOffset > 0f) {
        (offsetX.value / maxOffset).coerceIn(0f, 1f)
    } else {
        0f
    }

    LaunchedEffect(enabled, isDelivering) {
        if (!enabled || isDelivering) offsetX.snapTo(0f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) color.copy(alpha = 0.1f) else palette.surfaceMuted)
            .border(
                1.dp,
                if (enabled) color.copy(alpha = 0.2f) else palette.divider,
                RoundedCornerShape(16.dp)
            )
            .then(
                if (enabled && !isDelivering) {
                    Modifier.pointerInput(enabled) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val thresholdReached = maxOffset > 0f && offsetX.value >= maxOffset * 0.82f
                                if (thresholdReached) {
                                    onDeliver()
                                    scope.launch { offsetX.snapTo(0f) }
                                } else {
                                    scope.launch {
                                        offsetX.animateTo(0f, tween(220))
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f, tween(180)) }
                            },
                            onHorizontalDrag = { _, delta ->
                                scope.launch {
                                    offsetX.snapTo((offsetX.value + delta).coerceIn(0f, maxOffset))
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .onGloballyPositioned { trackWidth = it.size.width.toFloat() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { (offsetX.value + handleSizePx + 8f).toDp() })
                .background(color.copy(alpha = 0.08f + progress * 0.18f))
        )

        if (isDelivering) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(21.dp),
                    color = color,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    disabledLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        } else {
            Text(
                text = if (enabled) label else disabledLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) color else palette.textSecondary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(if (enabled) (1f - progress * 1.4f).coerceAtLeast(0f) else 0.65f)
                    .padding(start = 48.dp)
            )
        }

        if (!isDelivering) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .offset(x = with(density) { offsetX.value.toDp() })
                    .align(Alignment.CenterStart)
                    .size(handleSize)
                    .background(if (enabled) palette.surface else palette.surfaceMuted, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (progress >= 0.82f) {
                        Icons.Rounded.Check
                    } else {
                        Icons.AutoMirrored.Rounded.ArrowForward
                    },
                    contentDescription = "Deslizar para confirmar",
                    tint = if (enabled) color else palette.textSecondary,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

private fun notifyArrival(
    clientName: String,
    phone: String?,
    onFallback: () -> Unit,
    context: android.content.Context
) {
    val cleanPhone = phone?.filter(Char::isDigit).orEmpty()
    if (cleanPhone.isBlank()) {
        onFallback()
        return
    }

    val message = Uri.encode(
        "¡Hola $clientName! Soy tu repartidor de Regi Bazar. Estoy por llegar con tu pedido."
    )
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://wa.me/$cleanPhone?text=$message")
    )
    runCatching {
        context.startActivity(intent)
    }.getOrElse {
        Toast.makeText(context, "No pude abrir WhatsApp", Toast.LENGTH_SHORT).show()
        onFallback()
    }
}
