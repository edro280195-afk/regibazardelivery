package com.example.regibazarapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.regibazarapp.ui.theme.GlassBorder
import com.example.regibazarapp.ui.theme.GlassLightBorder

/**
 * Shared framed surface. The name stays for compatibility with existing
 * screens, but the visual treatment is solid and restrained.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderAlpha: Float = 0.22f,
    backgroundAlpha: Float = 1f,
    containerColor: Color = Color.Unspecified,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val shape = RoundedCornerShape(cornerRadius)
    val bg = when {
        containerColor != Color.Unspecified -> containerColor.copy(alpha = backgroundAlpha.coerceIn(0.90f, 1f))
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isDark) {
        GlassBorder.copy(alpha = borderAlpha)
    } else {
        GlassLightBorder.copy(alpha = borderAlpha + 0.12f)
    }

    Box(
        modifier = modifier
            .then(if (isDark) Modifier else Modifier.shadow(4.dp, shape, clip = false))
            .clip(shape)
            .background(bg)
            .border(width = 1.dp, color = borderColor, shape = shape),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    innerPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = cornerRadius
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            content = content
        )
    }
}
