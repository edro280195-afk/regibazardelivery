package com.example.regibazarapp.labelprint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.regibazarapp.data.models.LabelPrinterProfile
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.roundToInt

private const val THERMAL_DPI = 203f
private const val MM_PER_INCH = 25.4f

/** JSON v1 generado por el diseñador web; coordenadas físicas siempre en milímetros. */
internal data class LabelDesignDocument(
    val schemaVersion: Int = 1,
    val canvas: LabelCanvasDocument = LabelCanvasDocument(),
    val elements: List<LabelElementDocument> = emptyList()
)

internal data class LabelCanvasDocument(
    val widthMm: Float = 50f,
    val heightMm: Float = 50f,
    val background: String = "#FFFFFF"
)

internal data class LabelElementDocument(
    val id: String = "",
    val type: String = "text",
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 1f,
    val height: Float = 1f,
    val rotation: Float = 0f,
    val visible: Boolean = true,
    val zIndex: Int = 0,
    val properties: LabelElementPropertiesDocument = LabelElementPropertiesDocument()
)

internal data class LabelElementPropertiesDocument(
    val text: String? = null,
    val binding: String? = null,
    val assetId: String? = null,
    val fontSize: Float? = null,
    val fontWeight: Int? = null,
    val align: String? = null,
    val letterSpacing: Float? = null,
    val wrap: Boolean? = null,
    val prefix: String? = null,
    val suffix: String? = null,
    val format: String? = null,
    val displayValue: Boolean? = null,
    val fill: String? = null,
    val stroke: String? = null,
    val strokeWidth: Float? = null,
    val radius: Float? = null
)

/**
 * Renderer nativo del mismo documento que publica web. No reutiliza HTML ni una
 * plantilla local: genera un PNG a 203 DPI con la medida física del perfil.
 */
class ThermalLabelRenderer(private val gson: Gson = Gson()) {
    private val assetCache = mutableMapOf<String, Bitmap?>()

    suspend fun render(
        designJson: String,
        profile: LabelPrinterProfile,
        data: Map<String, String>,
        assetUrls: Map<String, String>
    ): Bitmap {
        val design = runCatching { gson.fromJson(designJson, LabelDesignDocument::class.java) }
            .getOrElse { throw IllegalArgumentException("No pudimos leer el diseño publicado de la etiqueta.") }
        require(design.schemaVersion == 1) { "Esta versión del diseño todavía no es compatible con la app." }
        require(closeEnough(design.canvas.widthMm, profile.widthMm) && closeEnough(design.canvas.heightMm, profile.heightMm)) {
            "La medida del diseño no coincide con la impresora seleccionada."
        }

        val images = withContext(Dispatchers.IO) {
            design.elements.asSequence()
                .mapNotNull { it.properties.assetId }
                .distinct()
                .associateWith { assetId -> loadAsset(assetId, assetUrls[assetId]) }
        }

        return withContext(Dispatchers.Default) {
        val widthPx = mmToPixelInt(profile.widthMm)
        val heightPx = mmToPixelInt(profile.heightMm)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        design.elements
            .asSequence()
            .filter { it.visible && it.width > 0f && it.height > 0f }
            .sortedBy { it.zIndex }
            .forEach { element ->
                val rect = element.toPixels()
                if (rect.width() < 1f || rect.height() < 1f) return@forEach
                canvas.save()
                canvas.rotate(element.rotation, rect.centerX(), rect.centerY())
                drawElement(canvas, element, rect, data, images)
                canvas.restore()
            }

        bitmap.toThermalMonochrome()
        }
    }

    private fun drawElement(
        canvas: Canvas,
        element: LabelElementDocument,
        rect: RectF,
        data: Map<String, String>,
        images: Map<String, Bitmap?>
    ) {
        when (element.type.lowercase()) {
            "text", "data" -> drawText(canvas, element, rect, resolveValue(element, data))
            "qr" -> resolveValue(element, data).takeIf { it.isNotBlank() }?.let { drawQr(canvas, it, rect) }
            "barcode" -> resolveValue(element, data).takeIf { it.isNotBlank() }?.let { drawBarcode(canvas, it, rect, element.properties.displayValue == true) }
            "image" -> element.properties.assetId?.let { assetId ->
                val image = images[assetId] ?: return@let
                drawImage(canvas, image, rect)
            }
            "shape" -> drawShape(canvas, element, rect)
            "line" -> drawLine(canvas, element, rect)
        }
    }

    private fun drawText(canvas: Canvas, element: LabelElementDocument, rect: RectF, value: String) {
        if (value.isBlank()) return
        val properties = element.properties
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(properties.fill, Color.BLACK)
            textSize = pointsToPx(properties.fontSize ?: 10f)
            typeface = Typeface.create(Typeface.SANS_SERIF, if ((properties.fontWeight ?: 400) >= 600) Typeface.BOLD else Typeface.NORMAL)
            letterSpacing = ((properties.letterSpacing ?: 0f) / 1000f).coerceIn(-0.05f, 0.2f)
        }
        val alignment = when (properties.align?.lowercase()) {
            "center" -> Layout.Alignment.ALIGN_CENTER
            "right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        val availableWidth = max(1, rect.width().roundToInt())
        val layout = StaticLayout.Builder.obtain(value, 0, value.length, textPaint, availableWidth)
            .setAlignment(alignment)
            .setIncludePad(false)
            .setMaxLines(if (properties.wrap == true) Int.MAX_VALUE else 1)
            .setEllipsize(null)
            .build()
        canvas.save()
        canvas.clipRect(rect)
        canvas.translate(rect.left, rect.top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawQr(canvas: Canvas, value: String, rect: RectF) {
        val size = max(1, minOf(rect.width(), rect.height()).roundToInt())
        val matrix = runCatching {
            MultiFormatWriter().encode(
                value,
                BarcodeFormat.QR_CODE,
                size,
                size,
                mapOf(EncodeHintType.MARGIN to 0)
            )
        }.getOrNull() ?: return
        val image = matrix.toBitmap()
        val square = RectF(rect.left, rect.top, rect.left + size, rect.top + size)
        canvas.drawBitmap(image, null, square, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawBarcode(canvas: Canvas, value: String, rect: RectF, displayValue: Boolean) {
        val textHeight = if (displayValue) (rect.height() * 0.18f).coerceAtLeast(pointsToPx(6f)) else 0f
        val barcodeHeight = (rect.height() - textHeight).coerceAtLeast(1f).roundToInt()
        val barcodeWidth = max(1, rect.width().roundToInt())
        val matrix = runCatching {
            MultiFormatWriter().encode(
                value,
                BarcodeFormat.CODE_128,
                barcodeWidth,
                barcodeHeight,
                mapOf(EncodeHintType.MARGIN to 0)
            )
        }.getOrNull() ?: return
        val image = matrix.toBitmap()
        canvas.drawBitmap(image, null, RectF(rect.left, rect.top, rect.right, rect.top + barcodeHeight), Paint(Paint.FILTER_BITMAP_FLAG))
        if (displayValue) {
            drawText(
                canvas,
                LabelElementDocument(properties = LabelElementPropertiesDocument(fontSize = 7f, align = "center")),
                RectF(rect.left, rect.bottom - textHeight, rect.right, rect.bottom),
                value
            )
        }
    }

    private fun drawImage(canvas: Canvas, image: Bitmap, rect: RectF) {
        val scale = minOf(rect.width() / image.width, rect.height() / image.height)
        val drawWidth = image.width * scale
        val drawHeight = image.height * scale
        val destination = RectF(
            rect.centerX() - drawWidth / 2f,
            rect.centerY() - drawHeight / 2f,
            rect.centerX() + drawWidth / 2f,
            rect.centerY() + drawHeight / 2f
        )
        canvas.drawBitmap(image, null, destination, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawShape(canvas: Canvas, element: LabelElementDocument, rect: RectF) {
        val properties = element.properties
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = parseColor(properties.fill, Color.BLACK)
        }
        val radius = mmToPx(properties.radius ?: 0f)
        if (radius > 0f) canvas.drawRoundRect(rect, radius, radius, paint) else canvas.drawRect(rect, paint)
        properties.stroke?.let { stroke ->
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = mmToPx(properties.strokeWidth ?: 0.25f)
            paint.color = parseColor(stroke, Color.BLACK)
            if (radius > 0f) canvas.drawRoundRect(rect, radius, radius, paint) else canvas.drawRect(rect, paint)
        }
    }

    private fun drawLine(canvas: Canvas, element: LabelElementDocument, rect: RectF) {
        val properties = element.properties
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = parseColor(properties.stroke ?: properties.fill, Color.BLACK)
            strokeWidth = mmToPx(properties.strokeWidth ?: 0.3f)
        }
        canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), paint)
    }

    private fun resolveValue(element: LabelElementDocument, data: Map<String, String>): String {
        val base = when (element.type.lowercase()) {
            "text" -> element.properties.text.orEmpty()
            else -> element.properties.binding?.let { data[it].orEmpty() }.orEmpty()
        }
        return "${element.properties.prefix.orEmpty()}$base${element.properties.suffix.orEmpty()}"
    }

    private fun loadAsset(assetId: String, url: String?): Bitmap? {
        if (url.isNullOrBlank() || !url.startsWith("https://")) return null
        return assetCache.getOrPut(assetId) {
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 8_000
                connection.readTimeout = 12_000
                connection.instanceFollowRedirects = false
                try {
                    if (connection.responseCode !in 200..299) return@runCatching null
                    connection.inputStream.use(BitmapFactory::decodeStream)
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }
    }

    private fun LabelElementDocument.toPixels(): RectF = RectF(
        mmToPx(x), mmToPx(y), mmToPx(x + width), mmToPx(y + height)
    )

    private fun BitMatrix.toBitmap(): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        for (y in 0 until height) for (x in 0 until width) {
            bitmap.setPixel(x, y, if (get(x, y)) Color.BLACK else Color.WHITE)
        }
    }

    private fun Bitmap.toThermalMonochrome(): Bitmap {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        for (index in pixels.indices) {
            val color = pixels[index]
            val alpha = Color.alpha(color)
            val luminance = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
            pixels[index] = if (alpha < 110 || luminance >= 172) Color.WHITE else Color.BLACK
        }
        setPixels(pixels, 0, width, 0, 0, width, height)
        return this
    }

    private fun closeEnough(left: Float, right: Float): Boolean = kotlin.math.abs(left - right) < 0.1f
    private fun mmToPx(mm: Float): Float = mm / MM_PER_INCH * THERMAL_DPI
    private fun mmToPixelInt(mm: Float): Int = (mm / MM_PER_INCH * THERMAL_DPI).roundToInt()
    private fun pointsToPx(points: Float): Float = points / 72f * THERMAL_DPI
    private fun parseColor(value: String?, fallback: Int): Int = runCatching { if (value.isNullOrBlank()) fallback else Color.parseColor(value) }.getOrDefault(fallback)
}
