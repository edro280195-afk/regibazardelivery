package com.example.regibazarapp.ui.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receptor del sistema para el widget.
 * Declarado en AndroidManifest.xml con APPWIDGET_UPDATE.
 */
class DeliveryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeliveryWidget()
}
