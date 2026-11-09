package com.mawaqiti.plus.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** The four Living Sky states, derived from today's table times. */
enum class SkyPhase {
    DAWN,
    DAY,
    GOLDEN,
    NIGHT;

    companion object {
        /**
         * DAWN: 90 minutes before Fajr until sunrise.
         * DAY: sunrise until Asr.
         * GOLDEN: Asr until Maghrib.
         * NIGHT: Maghrib until the next Dawn.
         *
         * All arguments are minutes since local midnight; wrapping across
         * midnight is handled explicitly so the app behaves the same shortly
         * after 00:00 as it does late at night.
         */
        fun forMinutes(
            minuteOfDay: Int,
            fajr: Int,
            sunrise: Int,
            asr: Int,
            maghrib: Int
        ): SkyPhase {
            val dawnStart = fajr - DAWN_LEAD_MINUTES
            return when {
                inWindow(minuteOfDay, dawnStart, sunrise) -> DAWN
                inWindow(minuteOfDay, sunrise, asr) -> DAY
                inWindow(minuteOfDay, asr, maghrib) -> GOLDEN
                else -> NIGHT
            }
        }

        const val DAWN_LEAD_MINUTES = 90

        private fun inWindow(value: Int, start: Int, end: Int): Boolean {
            val s = wrap(start)
            val e = wrap(end)
            val v = wrap(value)
            return if (s <= e) v >= s && v < e else v >= s || v < e
        }

        private fun wrap(minutes: Int): Int = ((minutes % 1440) + 1440) % 1440
    }
}

/**
 * Presentation tokens for one phase. Every colour used by a screen comes from
 * here; [contrastRatio] lets the unit tests prove WCAG AA for text colours
 * against the gradient and against the glass cards composited over it.
 */
data class SkyPalette(
    val phase: SkyPhase,
    val gradient: List<Color>,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val cardFill: Color,
    val cardBorder: Color,
    val heroFill: Color,
    val heroBorder: Color,
    val chipFill: Color,
    val chipBorder: Color,
    val sheet: Color,
    val sheetText: Color,
    val sheetMuted: Color,
    val warmOverlay: Color?,
    val horizonTint: Color?,
    val glow: Color?,
    /**
     * Pale haze painted from the top of the screen, fading out by
     * [SCRIM_FALLOFF] of the height. Only the two light phases need it: the
     * Day gradient starts at #2F6EA6, where #0D2438 body text would otherwise
     * sit at ~2.9:1. Compositing this wash lifts the header area to WCAG AA
     * without changing any of the authoritative gradient stops.
     */
    val topScrim: Color?,
    val dark: Boolean
) {
    fun backgroundAt(fraction: Float): Color {
        val clamped = min(1f, max(0f, fraction))
        if (gradient.size == 1) return gradient[0]
        val scaled = clamped * (gradient.size - 1)
        val index = min(gradient.size - 2, scaled.toInt())
        val local = scaled - index
        return ColorMath.lerp(gradient[index], gradient[index + 1], local)
    }

    /** The top haze's alpha at a vertical [fraction] of the screen. */
    fun scrimAlphaAt(fraction: Float): Float {
        val scrim = topScrim ?: return 0f
        val falloff = (1f - min(1f, max(0f, fraction)) / SCRIM_FALLOFF).coerceIn(0f, 1f)
        return scrim.alpha * falloff
    }

    /**
     * The colour actually painted behind content at a vertical [fraction]:
     * gradient, warm/horizon effects approximated by the gradient stops, and
     * the top haze. Used by the contrast tests so they measure what the user
     * sees rather than the raw tokens.
     */
    fun effectiveBackgroundAt(fraction: Float): Color {
        val base = backgroundAt(fraction)
        val scrim = topScrim ?: return base
        return ColorMath.composite(scrim.copy(alpha = scrimAlphaAt(fraction)), base)
    }

    /** Glass card fill composited over the rendered sky at [fraction]. */
    fun cardBackgroundAt(fraction: Float, emphasized: Boolean = false): Color =
        ColorMath.composite(
            if (emphasized) heroFill else cardFill,
            effectiveBackgroundAt(fraction)
        )

    companion object {

        /** Height (as a fraction) over which the top haze fades out. */
        const val SCRIM_FALLOFF = 0.42f

        fun of(phase: SkyPhase): SkyPalette = when (phase) {
            SkyPhase.DAWN -> Dawn
            SkyPhase.DAY -> Day
            SkyPhase.GOLDEN -> Golden
            SkyPhase.NIGHT -> Night
        }

        /** Brand green, kept for the picker's active segment like the reference. */
        val BrandGreen = Color(0xFF0F5132)
        val BrandGreenLight = Color(0xFF2E7D5B)

        val Dawn = SkyPalette(
            phase = SkyPhase.DAWN,
            gradient = listOf(Color(0xFF1A1440), Color(0xFF2A2155), Color(0xFF3A2C63)),
            text = Color(0xFFF2EDE2),
            textMuted = Color(0xFFF2EDE2).copy(alpha = 0.78f),
            accent = Color(0xFFFFD98A),
            cardFill = Color.White.copy(alpha = 0.10f),
            cardBorder = Color.White.copy(alpha = 0.20f),
            heroFill = Color.White.copy(alpha = 0.14f),
            heroBorder = Color.White.copy(alpha = 0.28f),
            chipFill = Color.White.copy(alpha = 0.12f),
            chipBorder = Color.White.copy(alpha = 0.28f),
            sheet = Color(0xFF171331),
            sheetText = Color(0xFFF2EDE2),
            sheetMuted = Color(0xFFF2EDE2).copy(alpha = 0.70f),
            warmOverlay = null,
            horizonTint = null,
            glow = Color(0xFFC77B4A).copy(alpha = 0.20f),
            topScrim = null,
            dark = true
        )

        val Day = SkyPalette(
            phase = SkyPhase.DAY,
            gradient = listOf(Color(0xFF2F6EA6), Color(0xFF5C9CC9), Color(0xFFA8D0E8)),
            text = Color(0xFF0D2438),
            textMuted = Color(0xFF0D2438).copy(alpha = 0.92f),
            accent = Color(0xFF0D2438),
            cardFill = Color.White.copy(alpha = 0.16f),
            cardBorder = Color.White.copy(alpha = 0.35f),
            heroFill = Color.White.copy(alpha = 0.22f),
            heroBorder = Color.White.copy(alpha = 0.45f),
            chipFill = Color.White.copy(alpha = 0.20f),
            chipBorder = Color.White.copy(alpha = 0.40f),
            sheet = Color(0xFFF7F9FB),
            sheetText = Color(0xFF10222F),
            sheetMuted = Color(0xFF4A5C68),
            warmOverlay = null,
            horizonTint = null,
            glow = null,
            topScrim = Color.White.copy(alpha = 0.38f),
            dark = false
        )

        val Golden = SkyPalette(
            phase = SkyPhase.GOLDEN,
            gradient = listOf(Color(0xFF2F6EA6), Color(0xFF6FA3C6), Color(0xFFB9D3E2)),
            text = Color(0xFF0D2438),
            textMuted = Color(0xFF0D2438).copy(alpha = 0.90f),
            accent = Color(0xFF0D2438),
            cardFill = Color.White.copy(alpha = 0.18f),
            cardBorder = Color.White.copy(alpha = 0.38f),
            heroFill = Color.White.copy(alpha = 0.24f),
            heroBorder = Color.White.copy(alpha = 0.48f),
            chipFill = Color.White.copy(alpha = 0.22f),
            chipBorder = Color.White.copy(alpha = 0.42f),
            sheet = Color(0xFFFBF8F3),
            sheetText = Color(0xFF10222F),
            sheetMuted = Color(0xFF4A5C68),
            warmOverlay = Color(0xFFF2B263).copy(alpha = 0.18f),
            horizonTint = Color(0xFFE8A45E).copy(alpha = 0.24f),
            glow = null,
            topScrim = Color.White.copy(alpha = 0.38f),
            dark = false
        )

        val Night = SkyPalette(
            phase = SkyPhase.NIGHT,
            gradient = listOf(Color(0xFF060A20), Color(0xFF0E1540), Color(0xFF1D2C62)),
            text = Color(0xFFEEF2FF),
            textMuted = Color(0xFFEEF2FF).copy(alpha = 0.75f),
            accent = Color(0xFFFFD98A),
            cardFill = Color.White.copy(alpha = 0.075f),
            cardBorder = Color.White.copy(alpha = 0.16f),
            heroFill = Color.White.copy(alpha = 0.10f),
            heroBorder = Color.White.copy(alpha = 0.22f),
            chipFill = Color.White.copy(alpha = 0.09f),
            chipBorder = Color.White.copy(alpha = 0.20f),
            sheet = Color(0xFF101733),
            sheetText = Color(0xFFEEF2FF),
            sheetMuted = Color(0xFFEEF2FF).copy(alpha = 0.65f),
            warmOverlay = null,
            horizonTint = null,
            glow = Color(0xFFC77B4A).copy(alpha = 0.10f),
            topScrim = null,
            dark = true
        )
    }
}

/** Colour math shared by the theme and its unit tests. */
object ColorMath {

    /** Composites [over] (with its alpha) onto an opaque [base]. */
    fun composite(over: Color, base: Color): Color = lerp(base, over, over.alpha)

    fun lerp(from: Color, to: Color, fraction: Float): Color {
        val t = min(1f, max(0f, fraction))
        return Color(
            red = from.red + (to.red - from.red) * t,
            green = from.green + (to.green - from.green) * t,
            blue = from.blue + (to.blue - from.blue) * t,
            alpha = 1f
        )
    }

    /** WCAG 2.1 relative luminance. */
    fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    /** WCAG contrast ratio, 1.0 .. 21.0. */
    fun contrastRatio(first: Color, second: Color): Double {
        val a = luminance(first)
        val b = luminance(second)
        val lighter = max(a, b)
        val darker = min(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Text colour after its own alpha is composited onto [background]. */
    fun effectiveText(text: Color, background: Color): Color = composite(text, background)
}
