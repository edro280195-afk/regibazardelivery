package com.example.regibazarapp.labelprint

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.PageRange
import android.print.pdf.PrintedPdfDocument
import androidx.core.content.FileProvider
import com.example.regibazarapp.data.models.LabelPrinterProfile
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Salidas nativas seguras: comparte PNG con la app de la impresora (NIIMBOT o
 * Label Expert) o abre el diálogo Android para una impresora USB/Bluetooth ya
 * instalada. No intenta adivinar un protocolo Bluetooth propietario.
 */
class LabelOutputController(private val context: Context) {
    fun shareWithPrinterApp(bitmap: Bitmap, title: String): Uri {
        val uri = writeBitmap(bitmap, title)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("Etiqueta Regi Bazar", uri)
        }
        context.startActivity(Intent.createChooser(intent, "Enviar etiqueta a la app de impresión"))
        return uri
    }

    fun openSystemPrint(bitmap: Bitmap, profile: LabelPrinterProfile, title: String) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: throw IllegalStateException("Este dispositivo no tiene disponible el servicio de impresión de Android.")
        manager.print(title, ThermalLabelPrintAdapter(context, bitmap), printAttributes(profile))
    }

    private fun writeBitmap(bitmap: Bitmap, rawTitle: String): Uri {
        val directory = File(context.cacheDir, "labels").apply { mkdirs() }
        val safeName = rawTitle.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_-]+"), "-")
            .trim('-')
            .ifBlank { "etiqueta-regi-bazar" }
        val file = File(directory, "$safeName-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "No pudimos preparar el PNG de la etiqueta." }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun printAttributes(profile: LabelPrinterProfile): PrintAttributes {
        val mediaSize = when (profile) {
            LabelPrinterProfile.NiimbotB1_50x50 -> PrintAttributes.MediaSize(
                "REGIBAZAR_B1_50X50", "Regi Bazar · 50 × 50 mm", 1969, 1969
            )
            LabelPrinterProfile.AiyinE40_4x6 -> PrintAttributes.MediaSize(
                "REGIBAZAR_E40_4X6", "Regi Bazar · 4 × 6 in", 4000, 6000
            )
        }
        return PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
    }
}

private class ThermalLabelPrintAdapter(
    private val context: Context,
    private val bitmap: Bitmap
) : PrintDocumentAdapter() {
    private var attributes: PrintAttributes? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        attributes = newAttributes
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("etiqueta-regi-bazar.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                .setPageCount(1)
                .build(),
            oldAttributes != newAttributes
        )
    }

    override fun onWrite(
        pages: Array<PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val activeAttributes = attributes
        if (activeAttributes == null || cancellationSignal.isCanceled) {
            callback.onWriteCancelled()
            return
        }
        val document = PrintedPdfDocument(context, activeAttributes)
        try {
            val page = document.startPage(0)
            val content = page.info.contentRect
            val destinationRect = fitInside(bitmap.width.toFloat(), bitmap.height.toFloat(), content)
            page.canvas.drawBitmap(bitmap, null, destinationRect, null)
            document.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use(document::writeTo)
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (exception: Exception) {
            callback.onWriteFailed(exception.localizedMessage ?: "No pudimos generar el documento de impresión.")
        } finally {
            document.close()
        }
    }

    private fun fitInside(width: Float, height: Float, bounds: android.graphics.Rect): RectF {
        val scale = minOf(bounds.width() / width, bounds.height() / height)
        val drawWidth = width * scale
        val drawHeight = height * scale
        return RectF(
            bounds.centerX() - drawWidth / 2f,
            bounds.centerY() - drawHeight / 2f,
            bounds.centerX() + drawWidth / 2f,
            bounds.centerY() + drawHeight / 2f
        )
    }
}
