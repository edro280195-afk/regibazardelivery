package com.example.regibazarapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.regibazarapp.data.models.CashDenominations
import com.example.regibazarapp.data.models.StartRouteRequest
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import com.example.regibazarapp.ui.utils.formatRouteMoney

@Composable
fun StartRouteFundDialog(
    initialExchangeRate: Double = 18.0,
    onDismiss: () -> Unit,
    onConfirmStart: (StartRouteRequest) -> Unit
) {
    val palette = regiBazar2026Palette()

    var activeTab by remember { mutableIntStateOf(0) } // 0: MXN, 1: USD

    // MXN billetes
    var mxn20 by remember { mutableIntStateOf(0) }
    var mxn50 by remember { mutableIntStateOf(0) }
    var mxn100 by remember { mutableIntStateOf(0) }
    var mxn200 by remember { mutableIntStateOf(0) }
    var mxn500 by remember { mutableIntStateOf(0) }
    var mxn1000 by remember { mutableIntStateOf(0) }
    var mxnCoinsText by remember { mutableStateOf("") }

    // USD billetes
    var usd1 by remember { mutableIntStateOf(0) }
    var usd5 by remember { mutableIntStateOf(0) }
    var usd10 by remember { mutableIntStateOf(0) }
    var usd20 by remember { mutableIntStateOf(0) }
    var usd50 by remember { mutableIntStateOf(0) }
    var usd100 by remember { mutableIntStateOf(0) }
    var exchangeRateText by remember { mutableStateOf(if (initialExchangeRate > 0) initialExchangeRate.toString() else "18.0") }

    val exchangeRate = exchangeRateText.toDoubleOrNull() ?: 18.0
    val mxnCoins = mxnCoinsText.toDoubleOrNull() ?: 0.0

    val totalMxn = (mxn20 * 20.0) + (mxn50 * 50.0) + (mxn100 * 100.0) +
            (mxn200 * 200.0) + (mxn500 * 500.0) + (mxn1000 * 1000.0) + mxnCoins

    val totalUsd = ((usd1 * 1) + (usd5 * 5) + (usd10 * 10) +
            (usd20 * 20) + (usd50 * 50) + (usd100 * 100)).toDouble()

    val grandTotal = totalMxn + (totalUsd * exchangeRate)

    fun buildRequest(): StartRouteRequest {
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
        return StartRouteRequest(
            initialCash = grandTotal,
            exchangeRate = exchangeRate,
            breakdown = breakdown
        )
    }

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
                            Icon(
                                Icons.Rounded.Payments,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Fondo Inicial de Ruta",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.textPrimary
                        )
                        Text(
                            "Caja chica / feria para dar cambio",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = palette.textSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Resumen del total
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.money.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, palette.money.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "TOTAL EN CAJA CHICA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = palette.money
                        )
                        Text(
                            formatRouteMoney(grandTotal),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.money
                        )
                        if (totalUsd > 0) {
                            Text(
                                "Incluye $${totalUsd.toInt()} USD (TC: $exchangeRate)",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Selector de Moneda (MXN / USD)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { activeTab = 0 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTab == 0) palette.accent else palette.surfaceMuted,
                        contentColor = if (activeTab == 0) Color.White else palette.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Pesos MXN (${formatRouteMoney(totalMxn)})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        onClick = { activeTab = 1 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTab == 1) palette.accent else palette.surfaceMuted,
                        contentColor = if (activeTab == 1) Color.White else palette.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Dólares USD ($${totalUsd.toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Contenido de Billetes
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeTab == 0) {
                        item {
                            DenominationRow("$20 MXN", mxn20, onInc = { mxn20++ }, onDec = { if (mxn20 > 0) mxn20-- })
                        }
                        item {
                            DenominationRow("$50 MXN", mxn50, onInc = { mxn50++ }, onDec = { if (mxn50 > 0) mxn50-- })
                        }
                        item {
                            DenominationRow("$100 MXN", mxn100, onInc = { mxn100++ }, onDec = { if (mxn100 > 0) mxn100-- })
                        }
                        item {
                            DenominationRow("$200 MXN", mxn200, onInc = { mxn200++ }, onDec = { if (mxn200 > 0) mxn200-- })
                        }
                        item {
                            DenominationRow("$500 MXN", mxn500, onInc = { mxn500++ }, onDec = { if (mxn500 > 0) mxn500-- })
                        }
                        item {
                            DenominationRow("$1,000 MXN", mxn1000, onInc = { mxn1000++ }, onDec = { if (mxn1000 > 0) mxn1000-- })
                        }
                        item {
                            OutlinedTextField(
                                value = mxnCoinsText,
                                onValueChange = { mxnCoinsText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Monedas / Feria suelta ($)") },
                                placeholder = { Text("Ej. 45.50") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.accent,
                                    unfocusedBorderColor = palette.divider
                                )
                            )
                        }
                    } else {
                        item {
                            OutlinedTextField(
                                value = exchangeRateText,
                                onValueChange = { exchangeRateText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Tipo de cambio (MXN por USD)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.accent,
                                    unfocusedBorderColor = palette.divider
                                )
                            )
                        }
                        item {
                            DenominationRow("$1 USD", usd1, onInc = { usd1++ }, onDec = { if (usd1 > 0) usd1-- })
                        }
                        item {
                            DenominationRow("$5 USD", usd5, onInc = { usd5++ }, onDec = { if (usd5 > 0) usd5-- })
                        }
                        item {
                            DenominationRow("$10 USD", usd10, onInc = { usd10++ }, onDec = { if (usd10 > 0) usd10-- })
                        }
                        item {
                            DenominationRow("$20 USD", usd20, onInc = { usd20++ }, onDec = { if (usd20 > 0) usd20-- })
                        }
                        item {
                            DenominationRow("$50 USD", usd50, onInc = { usd50++ }, onDec = { if (usd50 > 0) usd50-- })
                        }
                        item {
                            DenominationRow("$100 USD", usd100, onInc = { usd100++ }, onDec = { if (usd100 > 0) usd100-- })
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Botones de acción
                Button(
                    onClick = {
                        onConfirmStart(buildRequest())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                ) {
                    Text(
                        if (grandTotal > 0) "🚀 Iniciar Ruta con ${formatRouteMoney(grandTotal)}" else "🚀 Iniciar Ruta",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(6.dp))

                TextButton(
                    onClick = {
                        onConfirmStart(StartRouteRequest(initialCash = 0.0, exchangeRate = 18.0, breakdown = CashDenominations()))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Iniciar sin feria ($0.00)", color = palette.textSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DenominationRow(
    label: String,
    count: Int,
    onInc: () -> Unit,
    onDec: () -> Unit
) {
    val palette = regiBazar2026Palette()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceMuted.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onDec,
                    shape = CircleShape,
                    color = palette.surface,
                    contentColor = palette.textPrimary,
                    border = BorderStroke(1.dp, palette.divider),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    count.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (count > 0) palette.accent else palette.textSecondary,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )

                Surface(
                    onClick = onInc,
                    shape = CircleShape,
                    color = palette.accent,
                    contentColor = Color.White,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
