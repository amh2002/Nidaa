package com.mawaqiti.plus.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.mawaqiti.plus.engine.table.TableDay

/**
 * Beta 9 "Golden Night" phase model.
 *
 * This is a presentation-only layer that sits on top of the protected engine.
 * It never re-derives a prayer time: it only decides which of the five looks is
 * painted for a given minute of the day, using the times the engine already
 * produced.
 *
 *   NIGHT     Isha            -> Fajr            (wraps midnight)
 *   DAWN      Fajr            -> Sunrise
 *   MORNING   Sunrise         -> Asr
 *   AFTERNOON Asr             -> Maghrib
 *   SUNSET    Maghrib         -> Isha
 *
 * Windows are half-open [start, end): the minute a prayer falls exactly on
 * already belongs to the new phase. The four day phases are explicit; NIGHT is
 * the complement, which is what makes the midnight wrap work without any
 * special case.
 */
enum class GoldenPhase(val skyPhase: SkyPhase, val key: String) {
    NIGHT(SkyPhase.NIGHT, "night"),
    DAWN(SkyPhase.DAWN, "dawn"),
    MORNING(SkyPhase.DAY, "morning"),
    AFTERNOON(SkyPhase.GOLDEN, "afternoon"),
    SUNSET(SkyPhase.GOLDEN, "sunset");

    companion object {
        /** Debug/intent parser: "night", "dawn", ... case-insensitive. */
        fun fromKey(key: String?): GoldenPhase? =
            entries.firstOrNull { it.name.equals(key, true) || it.key.equals(key, true) }
    }
}

/** The five engine times the phase model depends on, in minutes of the day. */
data class PhaseBounds(
    val fajr: Int,
    val sunrise: Int,
    val asr: Int,
    val maghrib: Int,
    val isha: Int
) {
    companion object {
        fun fromDay(day: TableDay): PhaseBounds = PhaseBounds(
            fajr = day.fajr,
            sunrise = day.sunrise,
            asr = day.asr,
            maghrib = day.maghrib,
            isha = day.isha
        )
    }
}

/**
 * Pure phase resolver. Same contract as [SkyPhase.forMinutes]: [minuteOfDay] is
 * minutes since local midnight and may be any integer, including 1440+.
 */
fun resolvePhase(minuteOfDay: Int, bounds: PhaseBounds): GoldenPhase {
    val minute = wrapMinute(minuteOfDay)
    return when {
        inPhaseWindow(minute, bounds.fajr, bounds.sunrise) -> GoldenPhase.DAWN
        inPhaseWindow(minute, bounds.sunrise, bounds.asr) -> GoldenPhase.MORNING
        inPhaseWindow(minute, bounds.asr, bounds.maghrib) -> GoldenPhase.AFTERNOON
        inPhaseWindow(minute, bounds.maghrib, bounds.isha) -> GoldenPhase.SUNSET
        else -> GoldenPhase.NIGHT
    }
}

internal fun wrapMinute(minutes: Int): Int = ((minutes % 1440) + 1440) % 1440

/** Half-open window that also works when it crosses midnight. */
internal fun inPhaseWindow(value: Int, start: Int, end: Int): Boolean {
    val s = wrapMinute(start)
    val e = wrapMinute(end)
    val v = wrapMinute(value)
    if (s == e) return false
    return if (s < e) v in s until e else v >= s || v < e
}

/** The phase currently painted; read by the decorations, not by the engine. */
val LocalGoldenPhase = staticCompositionLocalOf { GoldenPhase.NIGHT }

/**
 * The five Golden Night palettes.
 *
 * NIGHT is sampled from the owner's reference image: near-black navy gradient
 * (#04060E -> #141C36), warm cream text (#FBEFD6) and the reference gold
 * (#F2CF8B) as the single accent. DAWN turns the same layout purple with a
 * pink-gold glow; MORNING is a lighter navy; AFTERNOON is amber; SUNSET is a
 * deep red-amber.
 *
 * Every palette is dark, so the status/navigation bars stay light everywhere.
 * The `phase` field reuses the four-value [SkyPhase] enum purely so existing
 * components (GlassCard, dashedOutline, the Material scheme) keep working; the
 * golden phase itself travels in [LocalGoldenPhase].
 *
 * Contrast is proven by GoldenPaletteTest for every text/background pair,
 * including the 10% white sheen GlassCard paints on top of its top 220 px.
 */
object GoldenPalettes {

    // Reference anchors, kept as named tokens so the test can assert them.
    val ReferenceGold = Color(0xFFF2CF8B)
    val ReferenceInk = Color(0xFF04060E)

    val Night = palette(
        skyPhase = SkyPhase.NIGHT,
        gradient = listOf(Color(0xFF04060E), Color(0xFF0A1020), Color(0xFF141C36)),
        text = Color(0xFFFBEFD6),
        textMutedAlpha = 0.85f,
        accent = ReferenceGold,
        cardFillAlpha = 0.05f,
        heroFillAlpha = 0.07f,
        chipFillAlpha = 0.07f,
        sheet = Color(0xFF121A33),
        sheetMutedAlpha = 0.74f,
        warmOverlay = null,
        horizonTint = null,
        glow = ReferenceGold.copy(alpha = 0.06f)
    )

    val Dawn = palette(
        skyPhase = SkyPhase.DAWN,
        gradient = listOf(Color(0xFF1B1238), Color(0xFF2A1D4E), Color(0xFF372353)),
        text = Color(0xFFFFF4EC),
        textMutedAlpha = 0.92f,
        accent = Color(0xFFFFE3D0),
        cardFillAlpha = 0.05f,
        heroFillAlpha = 0.07f,
        chipFillAlpha = 0.07f,
        sheet = Color(0xFF1C1340),
        sheetMutedAlpha = 0.80f,
        warmOverlay = null,
        horizonTint = Color(0xFFFF9E7A).copy(alpha = 0.08f),
        glow = Color(0xFFFF9E7A).copy(alpha = 0.10f)
    )

    val Morning = palette(
        skyPhase = SkyPhase.DAY,
        gradient = listOf(Color(0xFF101A31), Color(0xFF182844), Color(0xFF203454)),
        text = Color(0xFFF4F8FF),
        textMutedAlpha = 0.92f,
        accent = Color(0xFFFFF0CE),
        cardFillAlpha = 0.05f,
        heroFillAlpha = 0.07f,
        chipFillAlpha = 0.07f,
        sheet = Color(0xFF131E36),
        sheetMutedAlpha = 0.80f,
        warmOverlay = null,
        horizonTint = null,
        glow = ReferenceGold.copy(alpha = 0.05f)
    )

    val Afternoon = palette(
        skyPhase = SkyPhase.GOLDEN,
        gradient = listOf(Color(0xFF24150A), Color(0xFF331D0B), Color(0xFF42260E)),
        text = Color(0xFFFFF7EA),
        textMutedAlpha = 0.92f,
        accent = Color(0xFFFFE0AE),
        cardFillAlpha = 0.05f,
        heroFillAlpha = 0.07f,
        chipFillAlpha = 0.07f,
        sheet = Color(0xFF241509),
        sheetMutedAlpha = 0.82f,
        warmOverlay = null,
        horizonTint = Color(0xFFE8A45E).copy(alpha = 0.08f),
        glow = Color(0xFFFFB25E).copy(alpha = 0.10f)
    )

    val Sunset = palette(
        skyPhase = SkyPhase.GOLDEN,
        gradient = listOf(Color(0xFF33100F), Color(0xFF461517), Color(0xFF541A12)),
        text = Color(0xFFFFF1E6),
        textMutedAlpha = 0.92f,
        accent = Color(0xFFFFD9AE),
        cardFillAlpha = 0.05f,
        heroFillAlpha = 0.07f,
        chipFillAlpha = 0.07f,
        sheet = Color(0xFF2A0F10),
        sheetMutedAlpha = 0.82f,
        warmOverlay = null,
        horizonTint = Color(0xFFFF8A4A).copy(alpha = 0.08f),
        glow = Color(0xFFFF8A4A).copy(alpha = 0.12f)
    )

    fun of(phase: GoldenPhase): SkyPalette = when (phase) {
        GoldenPhase.NIGHT -> Night
        GoldenPhase.DAWN -> Dawn
        GoldenPhase.MORNING -> Morning
        GoldenPhase.AFTERNOON -> Afternoon
        GoldenPhase.SUNSET -> Sunset
    }

    private fun palette(
        skyPhase: SkyPhase,
        gradient: List<Color>,
        text: Color,
        textMutedAlpha: Float,
        accent: Color,
        cardFillAlpha: Float,
        heroFillAlpha: Float,
        chipFillAlpha: Float,
        sheet: Color,
        sheetMutedAlpha: Float,
        warmOverlay: Color?,
        horizonTint: Color?,
        glow: Color?
    ) = SkyPalette(
        phase = skyPhase,
        gradient = gradient,
        text = text,
        textMuted = text.copy(alpha = textMutedAlpha),
        accent = accent,
        cardFill = Color.White.copy(alpha = cardFillAlpha),
        cardBorder = accent.copy(alpha = 0.30f),
        heroFill = Color.White.copy(alpha = heroFillAlpha),
        heroBorder = accent.copy(alpha = 0.45f),
        chipFill = Color.White.copy(alpha = chipFillAlpha),
        chipBorder = accent.copy(alpha = 0.35f),
        sheet = sheet,
        sheetText = text,
        sheetMuted = text.copy(alpha = sheetMutedAlpha),
        warmOverlay = warmOverlay,
        horizonTint = horizonTint,
        glow = glow,
        topScrim = null,
        dark = true
    )
}
