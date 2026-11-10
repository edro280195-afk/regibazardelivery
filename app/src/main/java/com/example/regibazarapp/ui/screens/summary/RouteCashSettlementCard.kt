package com.example.regibazarapp.ui.screens.summary

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.regibazarapp.data.models.CashDenominations
import com.example.regibazarapp.data.models.RouteSettlementDto
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette
import com.example.regibazarapp.ui.utils.formatRouteMoney
import java.net.URLEncoder

@Composable
fun RouteCashSettlementCard(
    token: String,
    initialCash: Double,
    collectedByCash: Double,
    totalExpenses: Double,
    settlement: RouteSettlementDto?,
    isSettling: Boolean,
    palette: RegiBazar2026Palette,
    onSubmitSettlement: ((Double, CashDenominations, String?, (Boolean) -> Unit) -> Unit)?
) {
    val context = LocalContext.current

    var expandedDenominations by remember { mutableStateOf(settlement == null || !settlement.isSettled) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: MXN, 1: USD

    // Pre-cargar valores si ya existe un corte guardado
    val existingBreakdown = settlement?.finalCashBreakdown

    var mxn20 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.mxn20 ?: 0) }
    var mxn50 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.mxn50 ?: 0) }
    var mxn100 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.mxn100 ?: 0) }
    var mxn200 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.mxn200 ?: 0) }
    var mxn500 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.mxn500 ?: 0) }
    var mxn1000 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.mxn1000 ?: 0) }
    var mxnCoinsText by remember(existingBreakdown) {
        mutableStateOf(existingBreakdown?.mxnCoins?.let { if (it > 0) it.toString() else "" } ?: "")
    }

    var usd1 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.usd1 ?: 0) }
    var usd5 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.usd5 ?: 0) }
    var usd10 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.usd10 ?: 0) }
    var usd20 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.usd20 ?: 0) }
    var usd50 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.usd50 ?: 0) }
    var usd100 by remember(existingBreakdown) { mutableIntStateOf(existingBreakdown?.usd100 ?: 0) }
    var exchangeRateText by remember(existingBreakdown) {
        mutableStateOf(existingBreakdown?.exchangeRate?.toString() ?: "18.0")
    }
    var notesText by remember(settlement) { mutableStateOf(settlement?.settlementNotes ?: "") }

    val exchangeRate = exchangeRateText.toDoubleOrNull() ?: 18.0
    val mxnCoins = mxnCoinsText.toDoubleOrNull() ?: 0.0

    val countedMxn = (mxn20 * 20.0) + (mxn50 * 50.0) + (mxn100 * 100.0) +
            (mxn200 * 200.0) + (mxn500 * 500.0) + (mxn1000 * 1000.0) + mxnCoins

    val countedUsd = ((usd1 * 1) + (usd5 * 5) + (usd10 * 10) +
            (usd20 * 20) + (usd50 * 50) + (usd100 * 100)).toDouble()

    val totalContado = countedMxn + (countedUsd * exchangeRate)

    // Efectivo esperado
    val tandasCash = settlement?.totalTandasCash ?: 0.0
    val expectedCash = settlement?.expectedCashInHand ?: (initialCash + collectedByCash + tandasCash - totalExpenses)
    val difference = totalContado - expectedCash

    fun shareWhatsApp() {
        val shareText = settlement?.whatsAppShareText ?: buildLocalWhatsAppShare(
            initialCash = initialCash,
            ordersCash = collectedByCash,
            tandasCash = tandasCash,
            expenses = totalExpenses,
            expectedCash = expectedCash,
            actualCash = totalContado,
            difference = difference,
            token = token
        )
        try {
            val url = "https://wa.me/?text=" + URLEncoder.encode(shareText, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Enviar reporte por WhatsApp"))
        }
    }

    fun openWebReport() {
        val rawUrl = settlement?.webReportUrl ?: "https://regibazar.com/repartidor/$token/corte"
        val webUrl = rawUrl
            .replace("http://regibazarapi.onrender.com", "https://regibazar.com")
            .replace("https://regibazarapi.onrender.com", "https://regibazar.com")
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            // Título
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = palette.money.copy(alpha = 0.14f),
                    contentColor = palette.money,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Corte y Arqueo de Caja",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.textPrimary
                    )
                    Text(
                        if (settlement?.isSettled == true) "Corte guardado oficialmente ✓" else "Liquidación de efectivo del chofer",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (settlement?.isSettled == true) palette.money else palette.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Desglose de Caja Matemática
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.surfaceMuted.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SettlementMathRow("(+) Fondo Inicial (Caja Chica)", initialCash, palette.textSecondary, isAdd = true)
                    SettlementMathRow("(+) Efectivo Cobrado en Pedidos", collectedByCash, palette.textSecondary, isAdd = true)
                    if (tandasCash > 0) {
                        SettlementMathRow("(+) Efectivo Cobrado en Tandas", tandasCash, palette.accent, isAdd = true)
                    }
                    if (totalExpenses > 0) {
                        SettlementMathRow("(-) Gastos en Ruta", totalExpenses, palette.error, isAdd = false)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "EFECTIVO ESPERADO:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.textPrimary
                        )
                        Text(
                            formatRouteMoney(expectedCash),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.textPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Botón para desplegar conteo físico
            Surface(
                onClick = { expandedDenominations = !expandedDenominations },
                shape = RoundedCornerShape(12.dp),
                color = palette.accentSoft.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💵", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Conteo Físico de Billetes",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Text(
                            "Contado: ${formatRouteMoney(totalContado)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.accent
                        )
                    }
                    Icon(
                        if (expandedDenominations) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = palette.accent
                    )
                }
            }

            // Sección desplegable de denominaciones
            AnimatedVisibility(
                visible = expandedDenominations,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pestañas MXN / USD
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { activeTab = 0 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeTab == 0) palette.accent else palette.surfaceMuted,
                            contentColor = if (activeTab == 0) Color.White else palette.textPrimary,
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("MXN (${formatRouteMoney(countedMxn)})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            onClick = { activeTab = 1 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeTab == 1) palette.accent else palette.surfaceMuted,
                            contentColor = if (activeTab == 1) Color.White else palette.textPrimary,
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("USD ($${countedUsd.toInt()})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (activeTab == 0) {
                        CountRow("$20 MXN", mxn20, onInc = { mxn20++ }, onDec = { if (mxn20 > 0) mxn20-- }, palette)
                        CountRow("$50 MXN", mxn50, onInc = { mxn50++ }, onDec = { if (mxn50 > 0) mxn50-- }, palette)
                        CountRow("$100 MXN", mxn100, onInc = { mxn100++ }, onDec = { if (mxn100 > 0) mxn100-- }, palette)
                        CountRow("$200 MXN", mxn200, onInc = { mxn200++ }, onDec = { if (mxn200 > 0) mxn200-- }, palette)
                        CountRow("$500 MXN", mxn500, onInc = { mxn500++ }, onDec = { if (mxn500 > 0) mxn500-- }, palette)
                        CountRow("$1,000 MXN", mxn1000, onInc = { mxn1000++ }, onDec = { if (mxn1000 > 0) mxn1000-- }, palette)
                        OutlinedTextField(
                            value = mxnCoinsText,
                            onValueChange = { mxnCoinsText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Monedas / Feria suelta ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.accent,
                                unfocusedBorderColor = palette.divider
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = exchangeRateText,
                            onValueChange = { exchangeRateText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Tipo de cambio") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.accent,
                                unfocusedBorderColor = palette.divider
                            )
                        )
                        CountRow("$1 USD", usd1, onInc = { usd1++ }, onDec = { if (usd1 > 0) usd1-- }, palette)
                        CountRow("$5 USD", usd5, onInc = { usd5++ }, onDec = { if (usd5 > 0) usd5-- }, palette)
                        CountRow("$10 USD", usd10, onInc = { usd10++ }, onDec = { if (usd10 > 0) usd10-- }, palette)
                        CountRow("$20 USD", usd20, onInc = { usd20++ }, onDec = { if (usd20 > 0) usd20-- }, palette)
                        CountRow("$50 USD", usd50, onInc = { usd50++ }, onDec = { if (usd50 > 0) usd50-- }, palette)
                        CountRow("$100 USD", usd100, onInc = { usd100++ }, onDec = { if (usd100 > 0) usd100-- }, palette)
                    }

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notas u observaciones del corte") },
                        placeholder = { Text("Ej. Todo cuadrado, se entregó en bolsa...") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.divider
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Tarjeta de Estado de Cuadre
            val isCuadrado = Math.abs(difference) < 0.01
            val isSobrante = difference > 0.01
            val isFaltante = difference < -0.01

            val statusColor = when {
                isCuadrado -> palette.money
                isSobrante -> palette.warning
                else -> palette.error
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = statusColor.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isCuadrado) Icons.Rounded.CheckCircle else Icons.Rounded.ReportProblem,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when {
                                isCuadrado -> "¡CAJA CUADRADA PERFECTA!"
                                isSobrante -> "SOBRANTE EN CAJA"
                                else -> "FALTANTE EN CAJA"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp),
                            color = statusColor
                        )
                        Text(
                            when {
                                isCuadrado -> "$0.00 MXN"
                                isSobrante -> "+${formatRouteMoney(difference)}"
                                else -> "-${formatRouteMoney(-difference)}"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = statusColor
                        )
                        if (isFaltante) {
                            Text(
                                "Revisa los cambios entregados y billetes recibidos.",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Botón Guardar Corte (si hay callback disponible)
            if (onSubmitSettlement != null) {
                Button(
                    onClick = {
                        val breakdown = CashDenominations(
                            mxn20 = mxn20,
                            mxn50 = mxn50,
                            mxn100 = mxn100,
                            mxn200 = mxn200,
                            mxn500 = mxn500,
                            mxn1000 = mxn1000,
                            mxnCoins = mxnCoins,
                            usd1 = usd1,
                            usd5 = usd5,
                            usd10 = usd10,
                            usd20 = usd20,
                            usd50 = usd50,
                            usd100 = usd100,
                            exchangeRate = exchangeRate
                        )
                        onSubmitSettlement(totalContado, breakdown, notesText.ifBlank { null }) { success ->
                            if (success) {
                                Toast.makeText(context, "🏁 ¡Corte de caja guardado con éxito!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error al guardar corte", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSettling,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                ) {
                    if (isSettling) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (settlement?.isSettled == true) "Actualizar Corte de Caja" else "💾 Guardar Corte de Caja",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Botones WhatsApp y Reporte Web
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { shareWhatsApp() },
                    modifier = Modifier.weight(1.2f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Enviar a Dueña", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { openWebReport() },
                    modifier = Modifier.weight(0.9f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)
                ) {
                    Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ver Web", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettlementMathRow(
    label: String,
    amount: Double,
    amountColor: Color,
    isAdd: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF555555))
        Text(
            (if (isAdd) "+" else "-") + formatRouteMoney(amount),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = amountColor
        )
    }
}

@Composable
private fun CountRow(
    label: String,
    count: Int,
    onInc: () -> Unit,
    onDec: () -> Unit,
    palette: RegiBazar2026Palette
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.surfaceMuted.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            Surface(
                onClick = onDec,
                shape = CircleShape,
                color = palette.surface,
                border = BorderStroke(1.dp, palette.divider),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Menos", modifier = Modifier.size(14.dp))
                }
            }
            Text(
                count.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )
            Surface(
                onClick = onInc,
                shape = CircleShape,
                color = palette.accent,
                contentColor = Color.White,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, contentDescription = "Más", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

private fun buildLocalWhatsAppShare(
    initialCash: Double,
    ordersCash: Double,
    tandasCash: Double,
    expenses: Double,
    expectedCash: Double,
    actualCash: Double,
    difference: Double,
    token: String
): String {
    val diffText = when {
        Math.abs(difference) < 0.01 -> "✅ *CUADRADO ($0.00)*"
        difference > 0 -> "⚠️ *SOBRANTE: +$${String.format("%.2f", difference)}*"
        else -> "🚨 *FALTANTE: -$${String.format("%.2f", -difference)}*"
    }
    return """
🌸 *REGI BAZAR — CORTE DE CAJA* 🌸
━━━━━━━━━━━━━━━━━━
💰 *Fondo Inicial:* $${String.format("%.2f", initialCash)}
📦 *Cobros Pedidos (Efectivo):* $${String.format("%.2f", ordersCash)}
✨ *Cobros Tandas (Efectivo):* $${String.format("%.2f", tandasCash)}
🧾 *Gastos en Ruta:* -$${String.format("%.2f", expenses)}
━━━━━━━━━━━━━━━━━━
💵 *Efectivo Esperado:* $${String.format("%.2f", expectedCash)}
🔍 *Efectivo Contado:* $${String.format("%.2f", actualCash)}
📊 *Diferencia:* $diffText
━━━━━━━━━━━━━━━━━━
🌐 Ver desglose completo con fotos:
https://regibazar.com/repartidor/$token/corte
    """.trimIndent()
}
