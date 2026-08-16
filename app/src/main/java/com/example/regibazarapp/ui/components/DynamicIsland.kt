package com.example.regibazarapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.regibazarapp.ui.theme.*

/**
 * ═══════════════════════════════════════════════════════════
 *  DYNAMIC ISLAND – Componente flotante superior
 *
 *  Muestra: progreso de entregas, barra animada, total cobrado.
 *  Efecto Glassmorphism. Se expande animadamente cuando
 *  C.A.M.I. está escuchando.
 * ═══════════════════════════════════════════════════════════
 */
@Composable
fun DynamicIsland(
    deliveredCount: Int,
    totalCount: Int,
    totalCollected: Double,
    isCamiListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCount > 0) deliveredCount.toFloat() / totalCount else 0f

    // Animación suave del progreso
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress_anim"
    )

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .statusBarsPadding()
            .clip(RoundedCornerShape(32.dp))
            .background(DarkCard) // Fondo sólido y oscuro
            .border(1.dp, GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
            .clickable { /* Expandir si se desea */ }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        // ── Fila principal: Progreso + Total ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contador de entregas
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$deliveredCount",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = ActionPrimary
                )
                Text(
                    text = "/$totalCount",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Light
                    ),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "entregas",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // Total cobrado
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%,.0f", totalCollected)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = BrandAccent
                )
                Text(
                    text = "cobrado",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Barra de progreso animada ──
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(MaterialTheme.shapes.small),
            color = ActionPrimary,
            trackColor = GlassWhite.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )

        // ── C.A.M.I. Audio Waves (cuando está escuchando) ──
        if (isCamiListening) {
            Spacer(modifier = Modifier.height(12.dp))
            CamiAudioWaves()
        }
    }
}

/**
 * Barras de audio animadas para C.A.M.I.
 */
@Composable
private fun CamiAudioWaves() {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_waves")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✦ C.A.M.I. escuchando...",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp
            ),
            color = BrandAccent
        )
        Spacer(modifier = Modifier.width(8.dp))

        // 5 barras animadas simulando audio
        repeat(5) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = 20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + (index * 100),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(3.dp)
                    .height(height.dp)
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        Modifier.padding(0.dp)
                    )
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            Modifier.padding(0.dp)
                        )
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = BrandAccent)
                    }
                }
            }
        }
    }
}
