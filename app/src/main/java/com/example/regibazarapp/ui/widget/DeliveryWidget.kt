package com.example.regibazarapp.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.regibazarapp.MainActivity
import com.example.regibazarapp.R
import com.example.regibazarapp.data.local.WidgetDataStore

/**
 * ══════════════════════════════════════════════════════════════
 *  DeliveryWidget — Widget de pantalla de inicio (Glance)
 *
 *  Muestra la siguiente entrega sin abrir la app:
 *  nombre, dirección, saldo por cobrar y progreso X/Y.
 *
 *  Se actualiza automáticamente cuando RouteViewModel
 *  procesa nuevos datos de ruta.
 * ══════════════════════════════════════════════════════════════
 */
class DeliveryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = WidgetDataStore(context)
        provideContent {
            WidgetContent(
                name = store.getNextName(),
                address = store.getNextAddress(),
                balance = store.getBalance(),
                delivered = store.getDelivered(),
                total = store.getTotal(),
                isComplete = store.isComplete(),
                context = context
            )
        }
    }
}

// ── Colores hard-coded para Glance (no soporta MaterialTheme) ──
private val ColorInk         = ColorProvider(Color(0xFF12110F))
private val ColorAccent      = ColorProvider(Color(0xFF8F3D56))
private val ColorAction      = ColorProvider(Color(0xFF416F5A))
private val ColorTextPrimary = ColorProvider(Color(0xFFF2EEE8))
private val ColorTextMuted   = ColorProvider(Color(0xFF91877C))
private val ColorTextSecond  = ColorProvider(Color(0xFFC9BFB4))
private val ColorSurface     = ColorProvider(Color(0xFF191713))
private val ColorAccentBg    = ColorProvider(Color(0xFF241A1D))
private val ColorDivider     = ColorProvider(Color(0xFF2B2721))

@Composable
private fun WidgetContent(
    name: String,
    address: String,
    balance: Double,
    delivered: Int,
    total: Int,
    isComplete: Boolean,
    context: Context
) {
    val openApp = actionStartActivity(MainActivity::class.java)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorInk)
            .clickable(openApp)
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // ── HEADER: LOGO + MARCA ──────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_logo_regi),
                    contentDescription = null,
                    modifier = GlanceModifier.size(16.dp),
                    colorFilter = ColorFilter.tint(ColorAccent)
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text = "RegiDelivery",
                    style = TextStyle(
                        color = ColorTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.defaultWeight())
                
                // Status Dot
                Box(
                    modifier = GlanceModifier
                        .size(6.dp)
                        .background(ColorAction)
                ) {}
            }

            Spacer(GlanceModifier.height(12.dp))

            // ── BODY: CLIENTE O ESTADO ────────────────────
            when {
                isComplete -> {
                    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🚀 ¡RUTA FINALIZADA!",
                            style = TextStyle(
                                color = ColorAction,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = GlanceModifier.fillMaxWidth()
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = "Hoy completaste $delivered entregas",
                            style = TextStyle(color = ColorTextSecond, fontSize = 12.sp, textAlign = TextAlign.Center),
                            modifier = GlanceModifier.fillMaxWidth()
                        )
                    }
                }

                name.isBlank() -> {
                    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sin ruta activa 😴",
                            style = TextStyle(color = ColorTextMuted, fontSize = 14.sp),
                            modifier = GlanceModifier.fillMaxWidth()
                        )
                    }
                }

                else -> {
                    // SIGUIENTE CLIENTE (TITULAR)
                    Text(
                        text = name.uppercase(),
                        style = TextStyle(
                            color = ColorTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )

                    if (address.isNotBlank()) {
                        Text(
                            text = address,
                            style = TextStyle(color = ColorTextSecond, fontSize = 11.sp),
                            maxLines = 1
                        )
                    }

                    Spacer(GlanceModifier.height(14.dp))

                    // ── BARRA DE PROGRESO PREMIUM ───────────
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            Text(
                                text = "PROGRESO",
                                style = TextStyle(color = ColorTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(GlanceModifier.defaultWeight())
                            Text(
                                text = "$delivered de $total",
                                style = TextStyle(color = ColorTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(GlanceModifier.height(4.dp))
                        
                        // Barra física (Fondo + Llenado)
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(ColorDivider)
                        ) {
                            val ratio = if (total > 0) delivered.toFloat() / total.toFloat() else 0f
                            if (ratio > 0f) {
                                Row(modifier = GlanceModifier.fillMaxSize()) {
                                    Box(
                                        modifier = GlanceModifier
                                            .defaultWeight()
                                            .fillMaxHeight()
                                            .background(ColorAccent)
                                    ) {}
                                    if (ratio < 1f) {
                                        Spacer(GlanceModifier.defaultWeight())
                                    }
                                }
                            }
                        }
                    }

                    Spacer(GlanceModifier.defaultWeight())

                    // ── FOOTER: COBRO + ACCIÓN ──────────────
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "POR COBRAR",
                                style = TextStyle(color = ColorTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "$${String.format("%,.0f", balance)}",
                                style = TextStyle(
                                    color = ColorAction,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(GlanceModifier.defaultWeight())

                        // Botón estilizado
                        Box(
                            modifier = GlanceModifier
                                .background(ColorAccent)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "IR A RUTA",
                                style = TextStyle(
                                    color = ColorInk,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
