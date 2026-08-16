package com.example.regibazarapp.ui.components

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.regibazarapp.data.models.OrderPackageDto
import com.example.regibazarapp.data.models.ScanPackageRequest
import com.example.regibazarapp.data.network.NetworkModule
import com.example.regibazarapp.ui.theme.ActionPrimary
import com.example.regibazarapp.ui.theme.DarkBackground
import com.example.regibazarapp.ui.theme.DarkCard
import com.example.regibazarapp.ui.theme.StatusFailed
import com.example.regibazarapp.ui.theme.StatusPending
import com.example.regibazarapp.ui.theme.TextMuted
import com.example.regibazarapp.ui.theme.TextPrimary
import com.example.regibazarapp.ui.theme.TextSecondary
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════
 *  PackageScannerOverlay
 *
 *  Escáner QR con confirmación real en la API.
 *  Dos modos:
 *    action = "Load"    → al cargar la camioneta antes de salir
 *    action = "Deliver" → al entregar bolsas al cliente
 *
 *  Por cada QR detectado:
 *   1. Busca coincidencia local en la lista de paquetes
 *   2. Llama a POST /api/driver/{token}/packages/scan
 *   3. Muestra ✅ confirmado por API o ⚠️ error con mensaje
 * ═══════════════════════════════════════════════════════════════
 */

private enum class ScanState { SCANNING, SUCCESS, ERROR, RETURNED }
private data class PackageScanResult(
    val qrCodeValue: String,
    val state: ScanState,
    val message: String = ""
)

@android.annotation.SuppressLint("UnsafeOptInUsageError")
@Composable
fun PackageScannerOverlay(
    packages: List<OrderPackageDto>,
    driverToken: String,
    action: String, // "Load" | "Deliver"
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // qrCodeValue → resultado — pre-populated from API-persisted statuses
    val scanResults = remember(packages) {
        mutableStateMapOf<String, PackageScanResult>().also { map ->
            packages.forEach { pkg ->
                when (pkg.status) {
                    "Delivered" -> map[pkg.qrCodeValue] = PackageScanResult(pkg.qrCodeValue, ScanState.SUCCESS, "Ya entregada ✓")
                    "Returned"  -> map[pkg.qrCodeValue] = PackageScanResult(pkg.qrCodeValue, ScanState.RETURNED, "Devuelta ↩")
                    else -> { /* Loaded/Packed — pending scan */ }
                }
            }
        }
    }

    // Paquete pendiente de confirmación de devolución
    var returnConfirmPkg by remember { mutableStateOf<OrderPackageDto?>(null) }

    val confirmedCount = scanResults.values.count { it.state == ScanState.SUCCESS }
    val totalExpected = packages.size
    val allConfirmed = totalExpected > 0 && confirmedCount >= totalExpected

    // Último mensaje de feedback (desaparece tras unos segundos)
    var lastFeedback by remember { mutableStateOf<PackageScanResult?>(null) }

    fun handleReturnPackage(pkg: OrderPackageDto) {
        val qr = pkg.qrCodeValue
        if (scanResults[qr]?.state == ScanState.RETURNED) return
        scanResults[qr] = PackageScanResult(qr, ScanState.SCANNING)
        scope.launch {
            try {
                val response = NetworkModule.api.scanPackage(
                    driverToken = driverToken,
                    request = ScanPackageRequest(qr, "Return")
                )
                val result = if (response.isSuccessful && response.body() != null) {
                    PackageScanResult(qr, ScanState.RETURNED, "Bolsa #${pkg.packageNumber} devuelta ↩")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error ${response.code()}"
                    val clean = errorBody.substringAfter("\"message\":\"").substringBefore("\"")
                        .ifBlank { "Error al devolver" }
                    PackageScanResult(qr, ScanState.ERROR, clean)
                }
                scanResults[qr] = result
                lastFeedback = result
            } catch (e: Exception) {
                val result = PackageScanResult(qr, ScanState.ERROR, "Sin conexión")
                scanResults[qr] = result
                lastFeedback = result
            }
        }
    }

    fun handleScannedCode(qrCodeValue: String) {
        if (qrCodeValue in scanResults) return // Ya procesado

        // Marcar como escaneando
        scanResults[qrCodeValue] = PackageScanResult(qrCodeValue, ScanState.SCANNING)

        scope.launch {
            try {
                val response = NetworkModule.api.scanPackage(
                    driverToken = driverToken,
                    request = ScanPackageRequest(qrCodeValue, action)
                )
                val result = if (response.isSuccessful && response.body() != null) {
                    PackageScanResult(qrCodeValue, ScanState.SUCCESS, response.body()!!.message)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error ${response.code()}"
                    // Extraer mensaje limpio del JSON {"message":"..."}
                    val clean = errorBody.substringAfter("\"message\":\"").substringBefore("\"")
                        .ifBlank { "Error al confirmar" }
                    PackageScanResult(qrCodeValue, ScanState.ERROR, clean)
                }
                scanResults[qrCodeValue] = result
                lastFeedback = result
            } catch (e: Exception) {
                val result = PackageScanResult(qrCodeValue, ScanState.ERROR, "Sin conexión")
                scanResults[qrCodeValue] = result
                lastFeedback = result
                Log.e("PackageScanner", "Error scanning package: ${e.message}")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ── Camera Preview ──
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val barcodeScanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_CODE_39,
                                Barcode.FORMAT_ALL_FORMATS
                            )
                            .build()
                    )
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val value = barcode.rawValue ?: continue
                                        handleScannedCode(value)
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("PackageScanner", "Camera bind failed: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground.copy(alpha = 0.8f))
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = TextPrimary)
            }
            Icon(
                Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                tint = ActionPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (action == "Load") "Cargar bolsas" else "Verificar entrega",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            val counterColor by animateColorAsState(
                targetValue = when {
                    allConfirmed -> ActionPrimary
                    confirmedCount > 0 -> StatusPending
                    else -> TextPrimary
                },
                animationSpec = tween(300),
                label = "counter"
            )
            Text(
                text = "$confirmedCount/$totalExpected",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = counterColor
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        // ── Scanning frame (center) ──
        val frameColor = when {
            allConfirmed -> ActionPrimary
            lastFeedback?.state == ScanState.ERROR -> StatusFailed
            else -> TextPrimary.copy(alpha = 0.6f)
        }
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Box(Modifier.size(36.dp, 4.dp).background(frameColor).align(Alignment.TopStart))
            Box(Modifier.size(4.dp, 36.dp).background(frameColor).align(Alignment.TopStart))
            Box(Modifier.size(36.dp, 4.dp).background(frameColor).align(Alignment.TopEnd))
            Box(Modifier.size(4.dp, 36.dp).background(frameColor).align(Alignment.TopEnd))
            Box(Modifier.size(36.dp, 4.dp).background(frameColor).align(Alignment.BottomStart))
            Box(Modifier.size(4.dp, 36.dp).background(frameColor).align(Alignment.BottomStart))
            Box(Modifier.size(36.dp, 4.dp).background(frameColor).align(Alignment.BottomEnd))
            Box(Modifier.size(4.dp, 36.dp).background(frameColor).align(Alignment.BottomEnd))
        }

        // ── Feedback del último escaneo (centro-bajo del encuadre) ──
        lastFeedback?.let { fb ->
            val feedbackBg = when (fb.state) {
                ScanState.SUCCESS -> ActionPrimary.copy(alpha = 0.9f)
                ScanState.ERROR -> StatusFailed.copy(alpha = 0.9f)
                ScanState.SCANNING -> TextSecondary.copy(alpha = 0.8f)
                ScanState.RETURNED -> StatusPending.copy(alpha = 0.9f)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 140.dp)
                    .background(feedbackBg, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (fb.state == ScanState.SCANNING) "Verificando..." else fb.message,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // ── Bottom panel ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground.copy(alpha = 0.88f))
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            if (packages.isEmpty()) {
                Text(
                    text = "Apunta la cámara al código QR del paquete",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Text(
                    text = if (allConfirmed) "✅ Todos los paquetes confirmados por el sistema"
                    else "Apunta al código QR de cada bolsa",
                    color = if (allConfirmed) ActionPrimary else TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(packages) { pkg ->
                        val result = scanResults[pkg.qrCodeValue]
                        val isSuccess = result?.state == ScanState.SUCCESS
                        val isError = result?.state == ScanState.ERROR
                        val isScanning = result?.state == ScanState.SCANNING
                        val isReturned = result?.state == ScanState.RETURNED
                        val isPending = result == null

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    when {
                                        isSuccess -> ActionPrimary.copy(alpha = 0.12f)
                                        isReturned -> StatusPending.copy(alpha = 0.10f)
                                        isError -> StatusFailed.copy(alpha = 0.10f)
                                        else -> Color.Transparent
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            when {
                                isSuccess -> Icon(Icons.Filled.CheckCircle, null, tint = ActionPrimary, modifier = Modifier.size(18.dp))
                                isReturned -> Icon(Icons.Filled.Undo, null, tint = StatusPending, modifier = Modifier.size(18.dp))
                                isError -> Icon(Icons.Filled.ErrorOutline, null, tint = StatusFailed, modifier = Modifier.size(18.dp))
                                isScanning -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextPrimary)
                                else -> Icon(Icons.Outlined.RadioButtonUnchecked, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bolsa #${pkg.packageNumber}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSuccess || isReturned) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = when {
                                        isSuccess -> TextPrimary
                                        isReturned -> StatusPending
                                        isError -> StatusFailed
                                        else -> TextSecondary
                                    }
                                )
                                if (isError && result != null) {
                                    Text(result.message, style = MaterialTheme.typography.labelSmall, color = StatusFailed.copy(alpha = 0.8f))
                                }
                                if (isReturned) {
                                    Text("Devuelta", style = MaterialTheme.typography.labelSmall, color = StatusPending.copy(alpha = 0.8f))
                                }
                            }
                            when {
                                isSuccess -> Text("✓ API", style = MaterialTheme.typography.labelSmall, color = ActionPrimary)
                                isPending || isError -> {
                                    TextButton(
                                        onClick = { returnConfirmPkg = pkg },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("↩ Devolver", style = MaterialTheme.typography.labelSmall, color = StatusPending)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    // Paquetes escaneados que NO estaban en la lista esperada
                    val unknownScans = scanResults.values.filter { result ->
                        packages.none { it.qrCodeValue == result.qrCodeValue }
                    }
                    if (unknownScans.isNotEmpty()) {
                        item {
                            Text(
                                "Otros escaneados:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(unknownScans) { result ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                when (result.state) {
                                    ScanState.SUCCESS -> Icon(Icons.Filled.CheckCircle, null, tint = ActionPrimary, modifier = Modifier.size(16.dp))
                                    ScanState.RETURNED -> Icon(Icons.Filled.Undo, null, tint = StatusPending, modifier = Modifier.size(16.dp))
                                    ScanState.ERROR -> Icon(Icons.Filled.ErrorOutline, null, tint = StatusFailed, modifier = Modifier.size(16.dp))
                                    ScanState.SCANNING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TextPrimary)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (result.state) {
                                        ScanState.SUCCESS -> ActionPrimary
                                        ScanState.RETURNED -> StatusPending
                                        else -> StatusFailed
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allConfirmed) ActionPrimary else TextPrimary.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (allConfirmed) "Listo ✓" else "Cerrar",
                    color = if (allConfirmed) DarkBackground else TextPrimary
                )
            }
        }

        // ── Diálogo de confirmación de devolución ──
        returnConfirmPkg?.let { pkg ->
            AlertDialog(
                onDismissRequest = { returnConfirmPkg = null },
                containerColor = DarkCard,
                title = {
                    Text(
                        "¿Devolver bolsa #${pkg.packageNumber}?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        "La clienta rechazó esta bolsa. Se registrará como devolución y se notificará al admin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            handleReturnPackage(pkg)
                            returnConfirmPkg = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPending)
                    ) {
                        Text("↩ Confirmar devolución", color = DarkBackground)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { returnConfirmPkg = null }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            )
        }
    }
}
