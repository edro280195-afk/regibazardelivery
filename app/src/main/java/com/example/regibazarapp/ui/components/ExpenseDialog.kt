package com.example.regibazarapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.regibazarapp.data.network.NetworkModule
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

/**
 * ═══════════════════════════════════════════════════════════
 *  EXPENSE DIALOG – Registro de Gastos del Repartidor
 *
 *  Dialog tipo bottom-sheet para capturar gastos de ruta:
 *  tipo, monto, notas y foto de evidencia.
 * ═══════════════════════════════════════════════════════════
 */

private data class ExpenseType(
    val id: String,
    val label: String,
    val emoji: String
)

private val expenseTypes = listOf(
    ExpenseType("Gasolina", "Gasolina", "⛽"),
    ExpenseType("Caseta", "Caseta", "🛣️"),
    ExpenseType("Comida", "Comida", "🍔"),
    ExpenseType("Refresco", "Refresco", "🥤"),
    ExpenseType("Otro", "Otro", "📦")
)

@Composable
fun ExpenseDialog(
    driverToken: String,
    onDismiss: () -> Unit,
    onExpenseAdded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = regiBazar2026Palette()

    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<ExpenseType?>(null) }
    var notes by remember { mutableStateOf("") }
    var capturedPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        capturedPhoto = bitmap
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (granted) cameraLauncher.launch(null)
        else Toast.makeText(context, "Se necesita permiso de cámara para tomar evidencias", Toast.LENGTH_LONG).show()
    }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val canSubmit = amountValue > 0.0 && selectedType != null && !isSubmitting

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // ── Título ──
                Text(
                    text = "Registrar Gasto",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = palette.textPrimary
                )
                Text(
                    text = "Agrega el gasto de tu jornada",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Monto ──
                Text(
                    text = "MONTO",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        // Solo permitir dígitos y un punto decimal
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0.00", color = palette.textSecondary) },
                    prefix = { Text("$", color = palette.textSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.divider,
                        cursorColor = palette.accent,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Tipo de gasto ──
                Text(
                    text = "TIPO DE GASTO",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Primera fila: 3 chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    expenseTypes.take(3).forEach { type ->
                        ExpenseTypeChip(
                            expenseType = type,
                            isSelected = selectedType?.id == type.id,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedType = type }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Segunda fila: 2 chips centrados
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    expenseTypes.drop(3).forEach { type ->
                        ExpenseTypeChip(
                            expenseType = type,
                            isSelected = selectedType?.id == type.id,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedType = type }
                        )
                    }
                    // Spacer para centrar si hay número impar
                    if (expenseTypes.drop(3).size < 3) {
                        repeat(3 - expenseTypes.drop(3).size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Notas (opcional) ──
                Text(
                    text = "NOTAS (OPCIONAL)",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Descripción adicional...", color = palette.textSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.divider,
                        cursorColor = palette.accent,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Foto de evidencia ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón de cámara
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surfaceMuted)
                            .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
                            .clickable {
                                if (cameraPermissionGranted) cameraLauncher.launch(null)
                                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Tomar foto",
                            tint = if (capturedPhoto != null) palette.accent else palette.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Thumbnail si hay foto capturada
                    if (capturedPhoto != null) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, palette.accent, RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = capturedPhoto!!.asImageBitmap(),
                                contentDescription = "Foto capturada",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            // Checkmark overlay
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Foto lista",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = palette.accent
                            )
                            Text(
                                text = "Toca la cámara para retomar",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    } else {
                        Text(
                            text = "Foto de evidencia (opcional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }

                // ── Error message ──
                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Botones ──
                Button(
                    onClick = {
                        val type = selectedType ?: return@Button
                        val amount = amountValue

                        scope.launch {
                            isSubmitting = true
                            errorMessage = null
                            try {
                                val amountBody = amount.toString()
                                    .toRequestBody("text/plain".toMediaTypeOrNull())
                                val typeBody = type.id
                                    .toRequestBody("text/plain".toMediaTypeOrNull())
                                val notesBody = notes.ifBlank { null }?.toRequestBody(
                                    "text/plain".toMediaTypeOrNull()
                                )

                                // Construir parte de la foto si existe
                                val photoPart: MultipartBody.Part? = capturedPhoto?.let { bitmap ->
                                    val stream = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                    val photoBytes = stream.toByteArray()
                                    val photoBody = photoBytes.toRequestBody(
                                        "image/jpeg".toMediaTypeOrNull()
                                    )
                                    MultipartBody.Part.createFormData("photo", "expense.jpg", photoBody)
                                }

                                val response = NetworkModule.api.addExpense(
                                    driverToken = driverToken,
                                    amount = amountBody,
                                    expenseType = typeBody,
                                    notes = notesBody,
                                    photo = photoPart
                                )

                                if (response.isSuccessful) {
                                    onExpenseAdded()
                                } else {
                                    errorMessage = "Error del servidor: ${response.code()}"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error de conexión: ${e.localizedMessage}"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = Color(0xFFF8FAFF),
                        disabledContainerColor = palette.accent.copy(alpha = 0.3f),
                        disabledContentColor = Color(0xFFF8FAFF).copy(alpha = 0.4f)
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFF8FAFF),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Registrando...",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Text(
                            text = "Registrar",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = { if (!isSubmitting) onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancelar",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ─── Chip de tipo de gasto ───

@Composable
private fun ExpenseTypeChip(
    expenseType: ExpenseType,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = regiBazar2026Palette()
    val borderColor = if (isSelected) palette.accent else palette.divider
    val backgroundColor = if (isSelected) palette.accentSoft else Color.Transparent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = expenseType.emoji,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = expenseType.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) palette.accent else palette.textSecondary
            )
        }
    }
}
