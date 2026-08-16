package com.example.regibazarapp.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import java.util.Locale

data class SignatureCaptureResult(
    val bitmap: Bitmap,
    val svg: String,
    val signedByName: String?
)

@Composable
fun SignatureDialog(
    clientName: String,
    onConfirm: (SignatureCaptureResult) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = regiBazar2026Palette()
    val completedPaths = remember { mutableStateListOf<List<Offset>>() }
    val currentPath = remember { mutableStateListOf<Offset>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var signedByName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Gesture,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "FIRMA DE RECIBIDO",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.accent
                )
            }

            if (clientName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(clientName, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath.clear()
                                currentPath.add(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath.add(change.position)
                            },
                            onDragEnd = {
                                if (currentPath.size >= 2) {
                                    completedPaths.add(currentPath.toList())
                                }
                                currentPath.clear()
                            },
                            onDragCancel = { currentPath.clear() }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeStyle = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )

                    (completedPaths + listOf(currentPath.toList())).forEach { points ->
                        if (points.size >= 2) {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                points.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(path, Color.Black, style = strokeStyle)
                        }
                    }
                }

                if (completedPaths.isEmpty() && currentPath.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Firma aqui",
                            color = Color(0xFFCCCCCC),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Desliza el dedo para firmar",
                            color = Color(0xFFDDDDDD),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = signedByName,
                onValueChange = { signedByName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre de quien recibe (opcional)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.divider,
                    focusedLabelColor = palette.accent,
                    unfocusedLabelColor = palette.textSecondary,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    cursorColor = palette.accent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        completedPaths.clear()
                        currentPath.clear()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textSecondary),
                    border = BorderStroke(1.dp, palette.divider)
                ) {
                    Text("Limpiar")
                }

                Button(
                    onClick = {
                        val width = canvasSize.width.coerceAtLeast(400)
                        val height = canvasSize.height.coerceAtLeast(200)
                        val paths = completedPaths.toList()
                        onConfirm(
                            SignatureCaptureResult(
                                bitmap = renderToBitmap(paths, width, height),
                                svg = renderToSvg(paths, width, height),
                                signedByName = signedByName.trim().ifBlank { null }
                            )
                        )
                    },
                    enabled = completedPaths.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = Color(0xFFF8FAFF),
                        disabledContainerColor = palette.accentPressed.copy(alpha = 0.3f)
                    )
                ) {
                    Text("Guardar firma", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar", color = palette.textSecondary)
            }
        }
    }
}

private fun renderToBitmap(paths: List<List<Offset>>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 6f
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }

    paths.forEach { points ->
        if (points.size >= 2) {
            val path = android.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            canvas.drawPath(path, paint)
        }
    }

    return bitmap
}

private fun renderToSvg(paths: List<List<Offset>>, width: Int, height: Int): String {
    val pathData = paths
        .filter { it.size >= 2 }
        .joinToString(" ") { points ->
            buildString {
                append("M ${points.first().x.formatSvg()} ${points.first().y.formatSvg()}")
                points.drop(1).forEach { point ->
                    append(" L ${point.x.formatSvg()} ${point.y.formatSvg()}")
                }
            }
        }

    return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $width $height"><path d="$pathData" fill="none" stroke="#111111" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/></svg>"""
}

private fun Float.formatSvg(): String = String.format(Locale.US, "%.2f", this)
