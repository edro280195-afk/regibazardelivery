package com.example.regibazarapp.labelprint

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.coroutineContext

/** Impresora Bluetooth disponible tras emparejarla desde los ajustes de Android. */
data class PairedBluetoothPrinter(
    val address: String,
    val name: String
)

/** Resultado explícito para que la pantalla pueda explicar qué ocurrió sin adivinar. */
sealed interface DirectPrinterResult<out T> {
    data class Success<T>(val value: T) : DirectPrinterResult<T>
    data class Error(val message: String) : DirectPrinterResult<Nothing>
}

/** Confirmación de que la impresora aceptó los bytes; no garantiza la salida física. */
data class DirectPrintReceipt(val printerName: String)

/**
 * Cliente nativo para la AIYIN E40 Pro por Bluetooth Classic (SPP) y comandos TSPL.
 *
 * La E40 acepta TSPL y CPCL. Usamos TSPL porque permite enviar la misma etiqueta
 * monocromática que genera la app sin depender de Label Expert ni del diálogo del sistema.
 */
class DirectBluetoothLabelPrinter(private val context: Context) {
    fun listPairedPrinters(): DirectPrinterResult<List<PairedBluetoothPrinter>> {
        if (!hasBluetoothPermission()) {
            return DirectPrinterResult.Error("Autoriza Bluetooth para elegir la impresora.")
        }
        val adapter = bluetoothAdapter()
            ?: return DirectPrinterResult.Error("Este dispositivo no tiene Bluetooth disponible.")
        if (!adapter.isEnabled) {
            return DirectPrinterResult.Error("Activa Bluetooth para conectar la impresora.")
        }

        return runCatching {
            adapter.bondedDevices
                .orEmpty()
                .map { device ->
                    PairedBluetoothPrinter(
                        address = device.address,
                        name = device.name?.takeIf(String::isNotBlank) ?: "Impresora sin nombre"
                    )
                }
                .sortedBy(PairedBluetoothPrinter::name)
        }.fold(
            onSuccess = { printers -> DirectPrinterResult.Success(printers) },
            onFailure = { DirectPrinterResult.Error("No pudimos consultar las impresoras emparejadas.") }
        )
    }

    /** Envía una etiqueta 4 × 6 a la E40 y cierra la conexión al terminar. */
    suspend fun printE40(
        bitmap: Bitmap,
        printerAddress: String
    ): DirectPrinterResult<DirectPrintReceipt> = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission()) {
            return@withContext DirectPrinterResult.Error("Autoriza Bluetooth para imprimir directamente.")
        }
        val adapter = bluetoothAdapter()
            ?: return@withContext DirectPrinterResult.Error("Este dispositivo no tiene Bluetooth disponible.")
        if (!adapter.isEnabled) {
            return@withContext DirectPrinterResult.Error("Activa Bluetooth y vuelve a intentar.")
        }

        val printer = runCatching {
            adapter.bondedDevices.orEmpty().firstOrNull { it.address == printerAddress }
        }.getOrNull()
            ?: return@withContext DirectPrinterResult.Error(
                "La impresora ya no está emparejada. Vuelve a configurarla desde Ajustes Bluetooth."
            )

        val command = E40TsplCommandBuilder.build(MonochromeRaster.fromBitmap(bitmap))
        coroutineContext.ensureActive()

        try {
            openSocket(printer).use { socket ->
                coroutineContext.ensureActive()
                socket.outputStream.use { output ->
                    output.write(command)
                    output.flush()
                }
            }
            DirectPrinterResult.Success(
                DirectPrintReceipt(printer.name?.takeIf(String::isNotBlank) ?: "AIYIN E40 Pro")
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DirectPrinterResult.Error(
                "No pudimos enviar la etiqueta a la impresora: ${error.localizedMessage ?: "revisa que esté encendida y cerca."}"
            )
        }
    }

    private fun bluetoothAdapter(): BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    private fun hasBluetoothPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun openSocket(device: BluetoothDevice): BluetoothSocket {
        val secureSocket = device.createRfcommSocketToServiceRecord(SERIAL_PORT_PROFILE_UUID)
        try {
            secureSocket.connect()
            return secureSocket
        } catch (secureError: IOException) {
            runCatching { secureSocket.close() }
            val insecureSocket = device.createInsecureRfcommSocketToServiceRecord(SERIAL_PORT_PROFILE_UUID)
            try {
                insecureSocket.connect()
                return insecureSocket
            } catch (insecureError: IOException) {
                runCatching { insecureSocket.close() }
                throw IOException("No fue posible abrir el canal Bluetooth SPP.", insecureError)
            }
        }
    }

    private companion object {
        val SERIAL_PORT_PROFILE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}

/** Raster monocromático independiente de Android para poder validar el protocolo en pruebas unitarias. */
data class MonochromeRaster(
    val width: Int,
    val height: Int,
    val blackPixels: BooleanArray
) {
    init {
        require(width > 0 && height > 0) { "El raster debe tener una medida válida." }
        require(blackPixels.size == width * height) { "Los píxeles no coinciden con la medida del raster." }
    }

    fun isBlack(x: Int, y: Int): Boolean = blackPixels[y * width + x]

    fun scaleTo(targetWidth: Int, targetHeight: Int): MonochromeRaster {
        require(targetWidth > 0 && targetHeight > 0) { "La medida de destino debe ser válida." }
        if (width == targetWidth && height == targetHeight) return this

        val scaled = BooleanArray(targetWidth * targetHeight)
        for (y in 0 until targetHeight) {
            val sourceY = (y * height / targetHeight).coerceAtMost(height - 1)
            for (x in 0 until targetWidth) {
                val sourceX = (x * width / targetWidth).coerceAtMost(width - 1)
                scaled[y * targetWidth + x] = isBlack(sourceX, sourceY)
            }
        }
        return MonochromeRaster(targetWidth, targetHeight, scaled)
    }

    companion object {
        fun fromBitmap(bitmap: Bitmap): MonochromeRaster {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val blackPixels = BooleanArray(pixels.size) { index ->
                val color = pixels[index]
                val luminance = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
                Color.alpha(color) >= 110 && luminance < 172
            }
            return MonochromeRaster(bitmap.width, bitmap.height, blackPixels)
        }
    }
}

/** Construye un trabajo TSPL para la zona imprimible de 100 × 150 mm de la AIYIN E40 Pro. */
object E40TsplCommandBuilder {
    private const val PRINT_WIDTH_DOTS = 800
    private const val PRINT_HEIGHT_DOTS = 1200
    private const val BYTES_PER_ROW = PRINT_WIDTH_DOTS / 8

    fun build(source: MonochromeRaster): ByteArray {
        val raster = source.scaleTo(PRINT_WIDTH_DOTS, PRINT_HEIGHT_DOTS)
        val imageBytes = ByteArray(BYTES_PER_ROW * PRINT_HEIGHT_DOTS)

        for (y in 0 until PRINT_HEIGHT_DOTS) {
            for (x in 0 until PRINT_WIDTH_DOTS) {
                if (raster.isBlack(x, y)) {
                    val byteIndex = y * BYTES_PER_ROW + (x / 8)
                    imageBytes[byteIndex] = (imageBytes[byteIndex].toInt() or (1 shl (7 - (x % 8)))).toByte()
                }
            }
        }

        val header = "SIZE 100 mm,150 mm\r\n" +
            "GAP 3 mm,0 mm\r\n" +
            "DIRECTION 1\r\n" +
            "REFERENCE 0,0\r\n" +
            "CLS\r\n" +
            "BITMAP 0,0,$BYTES_PER_ROW,$PRINT_HEIGHT_DOTS,0,"
        val footer = "\r\nPRINT 1,1\r\n"
        return header.encodeToByteArray() + imageBytes + footer.encodeToByteArray()
    }
}
