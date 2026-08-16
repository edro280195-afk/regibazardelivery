package com.example.regibazarapp.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.regibazarapp.ui.theme.regiBazar2026Palette
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/** Cámara para bodega: acepta QR de caja y códigos lineales de artículos. */
@android.annotation.SuppressLint("UnsafeOptInUsageError")
@Composable
fun InventoryCodeScannerOverlay(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = regiBazar2026Palette()
    val cameraProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }
    var isLocked by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, barcodeScanner) {
        onDispose {
            cameraProvider.value?.unbindAll()
            barcodeScanner.close()
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF2B1020))) {
        AndroidView(
            factory = { viewContext ->
                PreviewView(viewContext).also { previewView ->
                    val future = ProcessCameraProvider.getInstance(viewContext)
                    future.addListener({
                        val provider = runCatching { future.get() }.getOrElse { error ->
                            Log.e("InventoryScanner", "No se pudo abrir la cámara", error)
                            errorMessage = "No pudimos abrir la cámara."
                            return@addListener
                        }
                        cameraProvider.value = provider
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(ContextCompat.getMainExecutor(viewContext)) { imageProxy ->
                            val image = imageProxy.image
                            if (image == null) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            val input = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                            barcodeScanner.process(input)
                                .addOnSuccessListener { barcodes ->
                                    if (isLocked) return@addOnSuccessListener
                                    val value = barcodes.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf(String::isNotEmpty) }
                                    if (value != null) {
                                        isLocked = true
                                        onCodeScanned(value)
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        }
                        runCatching {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        }.onFailure { error ->
                            Log.e("InventoryScanner", "No se pudo vincular la cámara", error)
                            errorMessage = "No pudimos iniciar el lector."
                        }
                    }, ContextCompat.getMainExecutor(viewContext))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar escáner", tint = Color.White)
            }
            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = palette.accent)
            Text(
                text = "Escanear caja o artículo",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(start = 10.dp).weight(1f)
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(28.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xDDFFFFFF)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(38.dp)
                )
                Text("Apunta al QR de la caja o al código de barras del artículo")
                errorMessage?.let { Text(it, color = palette.error) }
                if (errorMessage != null) {
                    Button(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        }
    }
}
