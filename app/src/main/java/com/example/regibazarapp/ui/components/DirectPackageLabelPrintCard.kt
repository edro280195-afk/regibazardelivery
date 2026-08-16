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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.regibazarapp.labelprint.DirectPrinterSettings
import com.example.regibazarapp.labelprint.PackageLabelPrintQueue
import com.example.regibazarapp.labelprint.PackageLabelPrintStatus
import com.example.regibazarapp.labelprint.PairedBluetoothPrinter
import com.example.regibazarapp.ui.theme.RegiBazar2026Palette

/** Panel operativo para imprimir todas las bolsas de una ruta sin abrir apps externas. */
@Composable
fun DirectPackageLabelPrintCard(
    packageCount: Int,
    settings: DirectPrinterSettings,
    queue: PackageLabelPrintQueue?,
    isPrinting: Boolean,
    palette: RegiBazar2026Palette,
    onPrintAll: () -> Unit,
    onRetryFailed: () -> Unit,
    onConfigurePrinter: () -> Unit
) {
    if (packageCount == 0) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Print, contentDescription = null, tint = palette.accent)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Etiquetas de bolsas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "$packageCount etiqueta(s) listas para esta ruta",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (settings.isConfigured) palette.successSoft else palette.warningSoft
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = if (settings.isConfigured) palette.success else palette.warning
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (settings.isConfigured) {
                                "AIYIN E40 Pro · ${settings.deviceName ?: "configurada"}"
                            } else {
                                "AIYIN E40 Pro sin configurar"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (settings.isConfigured) {
                                "Envío directo por Bluetooth"
                            } else {
                                "Empareja la impresora una sola vez"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    TextButton(onClick = onConfigurePrinter, enabled = !isPrinting) {
                        Text(if (settings.isConfigured) "Cambiar" else "Configurar")
                    }
                }
            }

            if (queue != null) {
                PackagePrintQueueSummary(queue = queue, isPrinting = isPrinting, palette = palette)
            }

            Button(
                onClick = onPrintAll,
                enabled = settings.isConfigured && !isPrinting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color(0xFFF8FAFF),
                    disabledContainerColor = palette.accentSoft,
                    disabledContentColor = palette.textSecondary
                )
            ) {
                if (isPrinting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = Color(0xFFF8FAFF),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Enviando etiquetas…", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Imprimir $packageCount etiqueta(s)", fontWeight = FontWeight.Bold)
                }
            }

            if ((queue?.failedCount ?: 0) > 0) {
                OutlinedButton(
                    onClick = onRetryFailed,
                    enabled = settings.isConfigured && !isPrinting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reintentar ${queue?.failedCount} pendiente(s)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PackagePrintQueueSummary(
    queue: PackageLabelPrintQueue,
    isPrinting: Boolean,
    palette: RegiBazar2026Palette
) {
    val finished = queue.sentCount + queue.failedCount
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider(color = palette.divider)
        Text(
            text = if (isPrinting) {
                "$finished de ${queue.items.size} procesadas"
            } else {
                "${queue.sentCount} enviada(s) · ${queue.failedCount} pendiente(s)"
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textSecondary
        )
        queue.items
            .filter { it.status != PackageLabelPrintStatus.SENT || queue.items.size <= 4 }
            .take(4)
            .forEach { item ->
                Text(
                    text = "Bolsa ${item.packageNumber} · ${queueStatusLabel(item.status)}${item.detail?.let { ": $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (item.status) {
                        PackageLabelPrintStatus.SENT -> palette.success
                        PackageLabelPrintStatus.FAILED -> palette.error
                        else -> palette.textSecondary
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
    }
}

private fun queueStatusLabel(status: PackageLabelPrintStatus): String = when (status) {
    PackageLabelPrintStatus.PENDING -> "En espera"
    PackageLabelPrintStatus.PREPARING -> "Preparando"
    PackageLabelPrintStatus.SENDING -> "Enviando"
    PackageLabelPrintStatus.SENT -> "Enviada"
    PackageLabelPrintStatus.FAILED -> "Pendiente"
}

@Composable
fun DirectPrinterPickerDialog(
    title: String = "Configurar impresora Bluetooth",
    printers: List<PairedBluetoothPrinter>,
    isLoading: Boolean,
    settings: DirectPrinterSettings,
    palette: RegiBazar2026Palette,
    onSelect: (PairedBluetoothPrinter) -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.accentSoft,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Impresión directa por Bluetooth",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Empareja primero tu impresora térmica desde los Ajustes Bluetooth del teléfono. Al seleccionarla aquí, la app guardará la preferencia para este módulo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    lineHeight = 18.sp
                )

                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = palette.accent
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Buscando impresoras emparejadas…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                } else if (printers.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.warningSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Sin dispositivos emparejados",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Activa el Bluetooth de tu teléfono y empareja la impresora térmica en Ajustes > Bluetooth. Luego toca 'Buscar dispositivos'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        printers.forEach { printer ->
                            val isSelected = printer.address == settings.address
                            Surface(
                                onClick = { onSelect(printer) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) palette.successSoft else palette.surfaceMuted,
                                border = if (isSelected) BorderStroke(1.5.dp, palette.success) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) palette.success else palette.surface,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Rounded.Print,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else palette.textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = printer.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = printer.address,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = palette.success,
                                            modifier = Modifier.padding(start = 6.dp)
                                        ) {
                                            Text(
                                                text = "Activa",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (settings.isConfigured) {
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quitar impresora predeterminada", color = palette.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onRefresh, enabled = !isLoading) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Buscar dispositivos")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        containerColor = palette.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
