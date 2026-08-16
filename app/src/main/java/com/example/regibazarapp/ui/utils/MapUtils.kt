package com.example.regibazarapp.ui.utils

import android.content.Context
import android.graphics.*

/**
 * Utilidades para personalizar el mapa de Google: pines numerados y estilo diurno.
 */
object MapUtils {

    /**
     * Estilo de mapa CLARO / DIURNO — legible al sol.
     * Oculta POIs y tránsito (menos ruido), calles blancas, autopistas con toque rosa de marca,
     * agua suave. Pensado para entregar de día.
     */
    const val DAY_MAP_STYLE: String = """
[
  {"elementType":"geometry","stylers":[{"color":"#f5f4f7"}]},
  {"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#5b5b66"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#f5f4f7"}]},
  {"featureType":"administrative.land_parcel","elementType":"labels.text.fill","stylers":[{"color":"#bdbdbd"}]},
  {"featureType":"poi","stylers":[{"visibility":"off"}]},
  {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#e3efde"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#ffffff"}]},
  {"featureType":"road.arterial","elementType":"labels.text.fill","stylers":[{"color":"#7a7a85"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#ffd8ea"}]},
  {"featureType":"road.highway","elementType":"labels.text.fill","stylers":[{"color":"#9a9aa3"}]},
  {"featureType":"transit","stylers":[{"visibility":"off"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#c7e4f0"}]},
  {"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#9aa0b0"}]}
]
"""

    /**
     * Crea un pin numerado para Google Maps.
     * @param highlight si es true, el pin se dibuja más grande y con un anillo de realce
     *                  (úsalo para la parada actual del conductor).
     */
    fun createNumberedMarkerBitmap(
        context: Context,
        number: Int,
        color: Int = Color.parseColor("#8F3D56"), // BrandAccent (rosewood) por defecto
        highlight: Boolean = false
    ): Bitmap {
        val size = if (highlight) 150 else 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = size / 2f
        val cy = size / 3.5f
        val headRadius = size / 3.5f

        // Sombra suave bajo el pin
        paint.color = Color.argb(60, 0, 0, 0)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy + 3f, headRadius, paint)

        // Anillo de realce para la parada actual
        if (highlight) {
            paint.color = Color.argb(90, 143, 61, 86) // BrandAccent (rosewood) translúcido
            canvas.drawCircle(cx, cy, headRadius + 10f, paint)
        }

        // Cabeza del pin
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, headRadius, paint)

        // Cola (triángulo)
        val path = Path()
        path.moveTo(cx - size / 4f, cy)
        path.lineTo(cx + size / 4f, cy)
        path.lineTo(cx, size / 1.2f)
        path.close()
        canvas.drawPath(path, paint)

        // Borde blanco para contraste
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (highlight) 6f else 4f
        canvas.drawCircle(cx, cy, headRadius, paint)

        // Número
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.textSize = size / 4f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER

        val textBounds = Rect()
        val text = number.toString()
        paint.getTextBounds(text, 0, text.length, textBounds)
        canvas.drawText(text, cx, cy + textBounds.height() / 2f, paint)

        return bitmap
    }
}
