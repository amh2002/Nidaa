package com.mawaqiti.plus.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


internal val LocalSkyPalette = staticCompositionLocalOf { SkyPalette.Day }

/** The Living Sky tokens for the current phase (animated during transitions). */
val skyPalette: SkyPalette
    @Composable get() = LocalSkyPalette.current

private const val PHASE_ANIMATION_MS = 500

/**
 * Applies the Living Sky palette for [phase], or [paletteOverride] when the
 * caller already resolved a full palette (beta 9 hands in one of the five
 * Golden Night palettes). Gradient stops, text, accent and the glass fills
 * animate over 500 ms, so a phase change reads as the sky turning rather than
 * a hard cut - including the cards, whose translucency is part of the look.
 */
@Composable
fun MawaqitiTheme(
    phase: SkyPhase = SkyPhase.NIGHT,
    paletteOverride: SkyPalette? = null,
    content: @Composable () -> Unit
) {
    val target = paletteOverride ?: SkyPalette.of(phase)
    val spec = tween<Color>(PHASE_ANIMATION_MS, easing = FastOutSlowInEasing)

    val start by animateColorAsState(target.gradient[0], spec, label = "skyStart")
    val middle by animateColorAsState(target.gradient[1], spec, label = "skyMiddle")
    val end by animateColorAsState(target.gradient[2], spec, label = "skyEnd")
    val text by animateColorAsState(target.text, spec, label = "skyText")
    val accent by animateColorAsState(target.accent, spec, label = "skyAccent")
    val muted by animateColorAsState(target.textMuted, spec, label = "skyMuted")
    val cardFill by animateColorAsState(target.cardFill, spec, label = "skyCard")
    val cardBorder by animateColorAsState(target.cardBorder, spec, label = "skyCardBorder")
    val heroFill by animateColorAsState(target.heroFill, spec, label = "skyHero")
    val heroBorder by animateColorAsState(target.heroBorder, spec, label = "skyHeroBorder")
    val chipFill by animateColorAsState(target.chipFill, spec, label = "skyChip")
    val chipBorder by animateColorAsState(target.chipBorder, spec, label = "skyChipBorder")

    val palette = target.copy(
        gradient = listOf(start, middle, end),
        text = text,
        textMuted = muted,
        accent = accent,
        cardFill = cardFill,
        cardBorder = cardBorder,
        heroFill = heroFill,
        heroBorder = heroBorder,
        chipFill = chipFill,
        chipBorder = chipBorder
    )

    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color(0xFF1A1440),
            secondary = palette.accent,
            onSecondary = Color(0xFF1A1440),
            background = palette.gradient[0],
            onBackground = palette.text,
            surface = palette.sheet,
            onSurface = palette.sheetText,
            surfaceVariant = palette.sheet,
            onSurfaceVariant = palette.sheetMuted,
            outline = palette.cardBorder,
            error = Color(0xFFFFB4AB)
        )
    } else {
        lightColorScheme(
            primary = SkyPalette.BrandGreen,
            onPrimary = Color.White,
            secondary = palette.accent,
            onSecondary = Color.White,
            background = palette.gradient[0],
            onBackground = palette.text,
            surface = palette.sheet,
            onSurface = palette.sheetText,
            surfaceVariant = palette.sheet,
            onSurfaceVariant = palette.sheetMuted,
            outline = Color(0xFF6B7C88),
            error = Color(0xFF8C1D18)
        )
    }

    CompositionLocalProvider(LocalSkyPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MawaqitiTypography,
            content = content
        )
    }
}
