package com.vyavsa.app.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Material 3 Expressive Brand Seed & Tonal Palette (Indigo-Slate & Emerald)
val PrimaryLight = Color(0xFF3649D8)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE0E3FF)
val OnPrimaryContainerLight = Color(0xFF061569)

val SecondaryLight = Color(0xFF057A55)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD1FAE5)
val OnSecondaryContainerLight = Color(0xFF022C22)

val TertiaryLight = Color(0xFF0284C7)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFE0F2FE)
val OnTertiaryContainerLight = Color(0xFF082F49)

val ErrorLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF7F1D1D)

// Modern Android 14/15 Tonal Surfaces (Light)
val BackgroundLight = Color(0xFFF5F7FC)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFEEF2F9)
val OnSurfaceVariantLight = Color(0xFF475569)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF8FAFE)
val SurfaceContainerLight = Color(0xFFF1F5FB)
val SurfaceContainerHighLight = Color(0xFFE8EEF7)
val SurfaceContainerHighestLight = Color(0xFFDFE6F2)
val OutlineLight = Color(0xFFCBD5E1)
val OutlineVariantLight = Color(0xFFE2E8F0)

// Material 3 Expressive Dark Palette
val PrimaryDark = Color(0xFFBDC4FF)
val OnPrimaryDark = Color(0xFF0A1D8F)
val PrimaryContainerDark = Color(0xFF2336B8)
val OnPrimaryContainerDark = Color(0xFFE0E3FF)

val SecondaryDark = Color(0xFF6EE7B7)
val OnSecondaryDark = Color(0xFF003826)
val SecondaryContainerDark = Color(0xFF045D40)
val OnSecondaryContainerDark = Color(0xFFD1FAE5)

val TertiaryDark = Color(0xFF7DD3FC)
val OnTertiaryDark = Color(0xFF003549)
val TertiaryContainerDark = Color(0xFF004D68)
val OnTertiaryContainerDark = Color(0xFFE0F2FE)

val ErrorDark = Color(0xFFFCA5A5)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFEE2E2)

// Modern Android 14/15 Tonal Surfaces (Dark)
val BackgroundDark = Color(0xFF090D16)
val OnBackgroundDark = Color(0xFFF1F5F9)
val SurfaceDark = Color(0xFF121826)
val OnSurfaceDark = Color(0xFFF1F5F9)
val SurfaceVariantDark = Color(0xFF1E293B)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val SurfaceContainerLowestDark = Color(0xFF070A12)
val SurfaceContainerLowDark = Color(0xFF0F1522)
val SurfaceContainerDark = Color(0xFF161E2E)
val SurfaceContainerHighDark = Color(0xFF1E293B)
val SurfaceContainerHighestDark = Color(0xFF283548)
val OutlineDark = Color(0xFF334155)
val OutlineVariantDark = Color(0xFF1E293B)

// Legacy & Semantic Financial Tokens (retained for full source compatibility)
val Navy900 = Color(0xFF0F172A)
val Navy800 = Color(0xFF1E293B)
val Navy700 = Color(0xFF334155)
val Navy600 = Color(0xFF475569)

val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

val Emerald600 = Color(0xFF059669) // Income / Positive Balance
val Emerald500 = Color(0xFF10B981)
val Emerald50 = Color(0xFFECFDF5)
val Crimson600 = Color(0xFFDC2626) // Expense / Dues / Negative
val Crimson500 = Color(0xFFEF4444)
val Crimson50 = Color(0xFFFEF2F2)
val Amber600 = Color(0xFFD97706)   // Pending / Warning
val Amber500 = Color(0xFFF59E0B)
val Amber50 = Color(0xFFFFFBEB)
val Blue600 = Color(0xFF2563EB)    // Bank / Info
val Blue500 = Color(0xFF3B82F6)
val Blue50 = Color(0xFFEFF6FF)

/**
 * Reusable Premium Financial Gradients for Hero Banners, Buttons, Pills, and Badges
 */
object VyavsaGradients {
    val PrimaryHeroBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4355E8),
            Color(0xFF2B3CC4),
            Color(0xFF162283)
        ),
        start = Offset(0f, 0f),
        end = Offset(900f, 650f)
    )

    val PrimaryButtonBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF3D50E0),
            Color(0xFF2A3BC2)
        )
    )

    val EmeraldHeroBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF059669),
            Color(0xFF047857),
            Color(0xFF065F46)
        ),
        start = Offset(0f, 0f),
        end = Offset(800f, 500f)
    )

    val BlueHeroBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2563EB),
            Color(0xFF1D4ED8),
            Color(0xFF1E3A8A)
        ),
        start = Offset(0f, 0f),
        end = Offset(800f, 500f)
    )

    val CrimsonButtonBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFEF4444),
            Color(0xFFDC2626)
        )
    )

    val SoftHeaderGlowBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3649D8).copy(alpha = 0.09f),
            Color.Transparent
        )
    )

    val TonalIndigoCardBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFEEF1FF),
            Color(0xFFE2E6FF)
        )
    )
}
