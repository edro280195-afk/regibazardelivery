package com.example.regibazarapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.example.regibazarapp.ui.theme.LocalThemeController

/**
 * Botón circular sol/luna para alternar tema claro/oscuro.
 * Usa [LocalThemeController]; persiste la preferencia automáticamente.
 */
@Composable
fun ThemeToggleButton(modifier: Modifier = Modifier) {
    val controller = LocalThemeController.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Surface(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { controller.toggle() }
            .semantics { contentDescription = if (isDark) "Cambiar a modo claro" else "Cambiar a modo oscuro" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        AnimatedContent(
            targetState = isDark,
            transitionSpec = {
                (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
            },
            label = "theme-icon"
        ) { dark ->
            Icon(
                imageVector = if (dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
