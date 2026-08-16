package com.example.regibazarapp.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Regi Bazar visual system.
 *
 * Product UI, 2026: light-first, restrained, warm neutrals, one muted brand
 * accent and one operational action color. No high-chroma glow, no pure
 * black or pure white surfaces.
 */

// Dark surfaces
val DarkBackground = Color(0xFF12110F)
val DarkSurface = Color(0xFF191713)
val DarkSurfaceVariant = Color(0xFF24211C)
val DarkCard = Color(0xFF1D1A16)

// Light surfaces
val LightBackground = Color(0xFFF6F1EA)
val LightSurface = Color(0xFFFFFBF6)
val LightSurfaceVariant = Color(0xFFEDE4DA)
val LightCard = Color(0xFFFFFBF6)

// Layer and border overlays. Kept under the old component vocabulary for
// compatibility, but visually these are solid, quiet overlays.
val GlassWhite = Color(0x18F6EFE6)
val GlassBorder = Color(0x33D8CBBE)
val GlassOverlay = Color(0x12F6EFE6)
val GlassLight = Color(0xF2FFFBF6)
val GlassLightBorder = Color(0x30281F1A)
val GlassLightOverlay = Color(0x14281F1A)

// Brand and action roles
val BrandAccent = Color(0xFF8F3D56)
val BrandAccentPressed = Color(0xFF6F2F43)
val BrandAccentOverlay = Color(0x338F3D56)
val BrandAccentSoft = Color(0x1F8F3D56)

val ActionPrimary = Color(0xFF416F5A)
val ActionPrimaryPressed = Color(0xFF315744)
val ActionPrimaryOverlay = Color(0x33416F5A)
val ActionPrimarySoft = Color(0x1F416F5A)

// Semantic states
val StatusPending = Color(0xFFC1843A)
val StatusInTransit = Color(0xFF4D6F8F)
val StatusDelivered = ActionPrimary
val StatusFailed = Color(0xFFB54A45)

val StatusPendingLight = Color(0xFF9A641F)
val StatusInTransitLight = Color(0xFF3F6383)
val StatusDeliveredLight = ActionPrimary
val StatusFailedLight = Color(0xFFA53E3A)

// Text
val TextPrimary = Color(0xFFF2EEE8)
val TextSecondary = Color(0xFFC9BFB4)
val TextMuted = Color(0xFF91877C)

val TextPrimaryLight = Color(0xFF251F1A)
val TextSecondaryLight = Color(0xFF62574E)
val TextMutedLight = Color(0xFF8A7D72)

// Content placed over filled controls
val InkOnAccent = Color(0xFFFFFBF6)
val InkOnAction = Color(0xFFFFFBF6)
val InkOnWarning = Color(0xFF241B10)

// Legacy gradient roles now map to the restrained palette.
val GradientAccentStart = Color(0xFF8F3D56)
val GradientAccentEnd = Color(0xFFA75E6E)
val GradientDarkStart = DarkSurfaceVariant
val GradientDarkEnd = DarkBackground

// Payment methods
val CashColor = ActionPrimary
val TransferColor = StatusInTransit
val DepositColor = StatusPending
