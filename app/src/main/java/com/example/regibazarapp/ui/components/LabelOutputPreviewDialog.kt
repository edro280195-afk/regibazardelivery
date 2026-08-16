package com.example.regibazarapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.regibazarapp.labelprint.DirectPrinterSettings
import com.example.regibazarapp.labelprint.RenderedLabelPreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import com.example.regibazarapp.ui.theme.regiBazar2026Palette

@Composable
fun LabelOutputPreviewDialog(
    preview: RenderedLabelPreview,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSystemPrint: () -> Unit,
    directPrinterSettings: DirectPrinterSettings? = null,
    isDirectPrinting: Boolean = false,
    onDirectPrint: (() -> Unit)? = null,
    onConfigurePrinter: (() -> Unit)? = null
) {
    val palette = regiBazar2026Palette()
    val hasDirectPrinter = directPrinterSettings?.isConfigured == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Etiqueta: ${preview.title.ifBlank { "Inventario" }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.accentSoft
                ) {
                    Text(
                        text = "${preview.profile.displayName} · v${preview.context.template.versionNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF4F6FB),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = preview.bitmap.asImageBitmap(),
                            contentDescription = "Vista previa de la etiqueta térmica",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (directPrinterSettings != null && onConfigurePrinter != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hasDirectPrinter) palette.successSoft else palette.accentSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = if (hasDirectPrinter) palette.success else palette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (hasDirectPrinter) {
                                        directPrinterSettings.deviceName ?: "Impresora activa"
                                    } else {
                                        "Sin impresora asignada"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (hasDirectPrinter) "Impresora predeterminada" else "Toca cambiar para asociar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                            TextButton(onClick = onConfigurePrinter, enabled = !isDirectPrinting) {
                                Text(if (hasDirectPrinter) "Cambiar" else "Configurar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasDirectPrinter && onDirectPrint != null) {
                    Button(
                        onClick = onDirectPrint,
                        enabled = !isDirectPrinting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = Color.White
                        )
                    ) {
                        if (isDirectPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Imprimiendo por Bluetooth…", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Rounded.Print, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Imprimir en ${directPrinterSettings?.deviceName ?: "Impresora"}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (onConfigurePrinter != null) {
                    Button(
                        onClick = onConfigurePrinter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                    ) {
                        Icon(Icons.Rounded.Bluetooth, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Configurar impresora Bluetooth", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Compartir a app (NIIMBOT / Label Expert)", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSystemPrint) {
                        Text("Impresión Android", color = palette.textSecondary)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = palette.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
