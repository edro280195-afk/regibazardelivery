package com.example.regibazarapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.regibazarapp.data.models.ActiveTandaInfoDto
import com.example.regibazarapp.data.models.DriverTandaPaymentRequest
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import com.example.regibazarapp.ui.utils.formatRouteMoney

@Composable
fun DriverTandaDialog(
    deliveryId: Int?,
    clientName: String,
    activeTandas: List<ActiveTandaInfoDto>?,
    initialSelectedTanda: ActiveTandaInfoDto? = null,
    onDismiss: () -> Unit,
    onConfirm: (DriverTandaPaymentRequest) -> Unit
) {
    val palette = regiBazar2026Palette()

    val firstTanda = initialSelectedTanda ?: activeTandas?.firstOrNull()

    var selectedTandaId by remember { mutableStateOf(firstTanda?.participantId ?: "") }
    var tandaName by remember { mutableStateOf(firstTanda?.tandaName ?: "Tanda") }
    var weekNumber by remember { mutableIntStateOf(firstTanda?.nextPendingWeek ?: 1) }
    var amountText by remember {
        mutableStateOf(
            if (firstTanda != null) {
                if (firstTanda.weeklyAmount % 1.0 == 0.0) firstTanda.weeklyAmount.toInt().toString()
                else firstTanda.weeklyAmount.toString()
            } else ""
        )
    }
    var paymentMethod by remember { mutableStateOf("Efectivo") }
    var cashReceivedText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val cashReceived = cashReceivedText.toDoubleOrNull()
    val change = if (cashReceived != null && amount > 0) cashReceived - amount else null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.accentSoft,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🌸", fontSize = 20.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Registrar Pago de Tanda",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.textPrimary
                        )
                        Text(
                            clientName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.accent
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = palette.textSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Si tiene tandas activas registradas en el sistema
                    if (!activeTandas.isNullOrEmpty()) {
                        item {
                            Text(
                                "Tandas activas de la clienta:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.textSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                activeTandas.forEach { tanda ->
                                    val isSelected = selectedTandaId == tanda.participantId
                                    Surface(
                                        onClick = {
                                            selectedTandaId = tanda.participantId
                                            tandaName = tanda.tandaName
                                            weekNumber = tanda.nextPendingWeek
                                            amountText = if (tanda.weeklyAmount % 1.0 == 0.0) tanda.weeklyAmount.toInt().toString() else tanda.weeklyAmount.toString()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) palette.accentSoft else palette.surfaceMuted,
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) palette.accent else palette.divider),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (isSelected) "🌸" else "⚪", fontSize = 14.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    tanda.tandaName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = palette.textPrimary
                                                )
                                                Text(
                                                    "Semana ${tanda.nextPendingWeek} de ${tanda.totalWeeks} • Cuota: ${formatRouteMoney(tanda.weeklyAmount)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Monto y Semana
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Monto cuota") },
                                prefix = { Text("$") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.2f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.accent,
                                    unfocusedBorderColor = palette.divider
                                )
                            )
                            OutlinedTextField(
                                value = weekNumber.toString(),
                                onValueChange = { v -> weekNumber = v.filter { it.isDigit() }.toIntOrNull() ?: 1 },
                                label = { Text("Semana #") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.8f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.accent,
                                    unfocusedBorderColor = palette.divider
                                )
                            )
                        }
                    }

                    // Método de pago
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("Efectivo", Icons.Rounded.Payments, palette.money),
                                Triple("Transferencia", Icons.Rounded.SwapHoriz, palette.accent)
                            ).forEach { (m, icon, color) ->
                                val selected = paymentMethod == m
                                Surface(
                                    onClick = { paymentMethod = m },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) color.copy(alpha = 0.12f) else palette.surfaceMuted,
                                    border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) color else palette.divider),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = if (selected) color else palette.textSecondary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(m, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) color else palette.textSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Calculadora de cambio si es efectivo
                    if (paymentMethod == "Efectivo") {
                        item {
                            Column {
                                Text(
                                    "Billetes rápidos recibidos:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textSecondary
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(50, 100, 200, 500, 1000).forEach { bill ->
                                        Surface(
                                            onClick = { cashReceivedText = bill.toString() },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (cashReceivedText == bill.toString()) palette.money else palette.surfaceMuted,
                                            contentColor = if (cashReceivedText == bill.toString()) Color.White else palette.money,
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("$$bill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Surface(
                                        onClick = { cashReceivedText = amountText },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (cashReceivedText == amountText && amountText.isNotBlank()) palette.accent else palette.surfaceMuted,
                                        contentColor = if (cashReceivedText == amountText && amountText.isNotBlank()) Color.White else palette.accent,
                                        modifier = Modifier.weight(1.1f).height(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("Exacto", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = cashReceivedText,
                                    onValueChange = { cashReceivedText = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("Efectivo entregado por la clienta") },
                                    prefix = { Text("$") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = palette.money,
                                        unfocusedBorderColor = palette.divider
                                    )
                                )

                                if (change != null) {
                                    Spacer(Modifier.height(6.dp))
                                    if (change >= 0) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFE8F5E9),
                                            border = BorderStroke(1.5.dp, Color(0xFF2E7D32)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("💵", fontSize = 20.sp)
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        "DAR DE CAMBIO:",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                                                        color = Color(0xFF1B5E20)
                                                    )
                                                    Text(
                                                        text = if (change == 0.0) "Cobro exacto ($0.00)" else formatRouteMoney(change),
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = Color(0xFF1B5E20)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFFFEBEE),
                                            border = BorderStroke(1.dp, Color(0xFFC62828)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("⚠️", fontSize = 16.sp)
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "Faltan ${formatRouteMoney(-change)} para cubrir la tanda",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFC62828)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Observaciones
                    item {
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Observaciones / Mensaje") },
                            placeholder = { Text("Ej. Mandó lo de la tanda con el pedido...") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.accent,
                                unfocusedBorderColor = palette.divider
                            )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        val req = DriverTandaPaymentRequest(
                            deliveryId = deliveryId,
                            tandaParticipantId = selectedTandaId.ifBlank { null },
                            clientName = clientName,
                            weekNumber = weekNumber,
                            amount = amount,
                            paymentMethod = paymentMethod,
                            notes = notesText.ifBlank { null },
                            cashReceived = cashReceived,
                            cashChange = if (change != null && change >= 0) change else null
                        )
                        onConfirm(req)
                    },
                    enabled = amount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                ) {
                    Text(
                        if (amount > 0) "🌸 Registrar ${formatRouteMoney(amount)} de Tanda" else "Ingresa el monto",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar", color = palette.textSecondary)
                }
            }
        }
    }
}
