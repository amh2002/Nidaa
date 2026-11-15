package com.mawaqiti.plus

import androidx.compose.ui.graphics.Color
import com.mawaqiti.plus.ui.theme.ColorMath
import com.mawaqiti.plus.ui.theme.GoldenPalettes
import com.mawaqiti.plus.ui.theme.GoldenPhase
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.mawaqiti.plus.ui.theme.cloudAlphaFor
import com.mawaqiti.plus.ui.theme.moonAlphaFor
import com.mawaqiti.plus.ui.theme.skylineAlphaFor
import com.mawaqiti.plus.ui.theme.starAlphaFor
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.1 AA proof for the five Golden Night palettes.
 *
 * The model is deliberately pessimistic:
 *  - gradient sampled across the whole height,
 *  - horizon tint and bottom glow composited at full strength everywhere,
 *  - GlassCard's 10% white top sheen added to every card,
 *  - muted text composited with its own alpha,
 *  - the countdown pill as black 20% over the hero card.
 *
 * If a palette edit drops any text/background pair below 4.5:1, this fails.
 */
class GoldenPaletteTest {

    private companion object {
        /** Worst-case white sheen GlassCard paints over the top of a card. */
        val SHEEN = Color.White.copy(alpha = 0.10f)
        val PILL_SCRIM = Color.Black.copy(alpha = 0.20f)
        val FRACTIONS = listOf(0f, 0.05f, 0.16f, 0.30f, 0.42f, 0.6f, 0.8f, 1f)
    }

    private fun ratio(first: Color, second: Color): Double = ColorMath.contrastRatio(first, second)

    private fun assertMin(actual: Double, minimum: Double, what: String) {
        assertTrue(
            String.format(Locale.US, "%s: %.2f:1 < %.1f:1", what, actual, minimum),
            actual >= minimum
        )
    }

    /** Sky with its decorative overlays applied at full strength. */
    private fun sky(palette: SkyPalette, fraction: Float): Color {
        var color = palette.backgroundAt(fraction)
        palette.horizonTint?.let { color = ColorMath.composite(it, color) }
        palette.glow?.let { color = ColorMath.composite(it, color) }
        return color
    }

    private fun card(palette: SkyPalette, fraction: Float, hero: Boolean): Color {
        val fill = if (hero) palette.heroFill else palette.cardFill
        return ColorMath.composite(SHEEN, ColorMath.composite(fill, sky(palette, fraction)))
    }

    @Test
    fun textPassesAaEverywhere() {
        for (phase in GoldenPhase.entries) {
            val palette = GoldenPalettes.of(phase)
            for (fraction in FRACTIONS) {
                assertMin(
                    ratio(palette.text, sky(palette, fraction)), 4.5,
                    "${phase.name} text on sky @ $fraction"
                )
                assertMin(
                    ratio(palette.text, card(palette, fraction, hero = false)), 4.5,
                    "${phase.name} text on card @ $fraction"
                )
                assertMin(
                    ratio(palette.text, card(palette, fraction, hero = true)), 4.5,
                    "${phase.name} text on hero @ $fraction"
                )
            }
        }
    }

    @Test
    fun mutedTextPassesAaIncludingItsOwnAlpha() {
        for (phase in GoldenPhase.entries) {
            val palette = GoldenPalettes.of(phase)
            for (fraction in FRACTIONS) {
                val skyColor = sky(palette, fraction)
                assertMin(
                    ratio(ColorMath.composite(palette.textMuted, skyColor), skyColor), 4.5,
                    "${phase.name} muted on sky @ $fraction"
                )
                val cardColor = card(palette, fraction, hero = false)
                assertMin(
                    ratio(ColorMath.composite(palette.textMuted, cardColor), cardColor), 4.5,
                    "${phase.name} muted on card @ $fraction"
                )
                val heroColor = card(palette, fraction, hero = true)
                assertMin(
                    ratio(ColorMath.composite(palette.textMuted, heroColor), heroColor), 4.5,
                    "${phase.name} muted on hero @ $fraction"
                )
            }
        }
    }

    @Test
    fun accentPassesAaAsTextAndAsIcon() {
        for (phase in GoldenPhase.entries) {
            val palette = GoldenPalettes.of(phase)
            for (fraction in FRACTIONS) {
                assertMin(
                    ratio(palette.accent, sky(palette, fraction)), 4.5,
                    "${phase.name} accent on sky @ $fraction"
                )
                val cardColor = card(palette, fraction, hero = false)
                assertMin(
                    ratio(palette.accent, cardColor), 4.5,
                    "${phase.name} accent on card @ $fraction"
                )
                val heroColor = card(palette, fraction, hero = true)
                assertMin(
                    ratio(palette.accent, heroColor), 4.5,
                    "${phase.name} accent on hero @ $fraction"
                )
                // Countdown pill: accent text on a black-20% inset of the hero.
                val pill = ColorMath.composite(PILL_SCRIM, heroColor)
                assertMin(
                    ratio(palette.accent, pill), 4.5,
                    "${phase.name} countdown on pill @ $fraction"
                )
            }
        }
    }

    @Test
    fun chipAndSheetSurfacesPassAa() {
        for (phase in GoldenPhase.entries) {
            val palette = GoldenPalettes.of(phase)
            val chip = ColorMath.composite(
                SHEEN,
                ColorMath.composite(palette.chipFill, sky(palette, 0.06f))
            )
            assertMin(ratio(palette.text, chip), 4.5, "${phase.name} chip label")

            assertMin(
                ratio(palette.sheetText, palette.sheet), 4.5,
                "${phase.name} sheet text"
            )
            val muted = ColorMath.composite(palette.sheetMuted, palette.sheet)
            assertMin(ratio(muted, palette.sheet), 4.5, "${phase.name} sheet muted")
        }
    }

    @Test
    fun nightIsAnchoredToTheReferenceImage() {
        // Sampled from the owner's golden_night reference: near-black navy with
        // a single warm gold accent.
        assertEquals(
            listOf(Color(0xFF04060E), Color(0xFF0A1020), Color(0xFF141C36)),
            GoldenPalettes.Night.gradient
        )
        assertEquals(Color(0xFFF2CF8B), GoldenPalettes.Night.accent)
        assertEquals(Color(0xFFFBEFD6), GoldenPalettes.Night.text)
        assertTrue(GoldenPalettes.Night.dark)
    }

    @Test
    fun allFivePalettesAreDistinctAndDark() {
        val gradients = GoldenPhase.entries.map { GoldenPalettes.of(it).gradient }
        assertEquals(5, gradients.toSet().size)
        for (phase in GoldenPhase.entries) {
            assertTrue("${phase.name} must be dark", GoldenPalettes.of(phase).dark)
            assertEquals(3, GoldenPalettes.of(phase).gradient.size)
            assertEquals(1f, GoldenPalettes.of(phase).text.alpha)
            assertEquals(1f, GoldenPalettes.of(phase).accent.alpha)
        }
        assertNotEquals(GoldenPalettes.Night.accent, GoldenPalettes.Dawn.accent)
        assertNotEquals(GoldenPalettes.Afternoon.gradient, GoldenPalettes.Sunset.gradient)
    }

    @Test
    fun decorationsFadeOutAwayFromNight() {
        assertEquals(1f, starAlphaFor(GoldenPhase.NIGHT))
        assertTrue(starAlphaFor(GoldenPhase.DAWN) > 0f)
        assertTrue(starAlphaFor(GoldenPhase.DAWN) < starAlphaFor(GoldenPhase.NIGHT))
        assertEquals(1f, moonAlphaFor(GoldenPhase.NIGHT))
        assertTrue(moonAlphaFor(GoldenPhase.DAWN) > 0f)
        assertTrue(moonAlphaFor(GoldenPhase.DAWN) < moonAlphaFor(GoldenPhase.NIGHT))
        // The silhouette stays visible in every phase, it is what grounds the
        // layout; clouds never disappear either.
        for (phase in GoldenPhase.entries) {
            assertTrue(skylineAlphaFor(phase) > 0.5f)
            assertTrue(cloudAlphaFor(phase) > 0f)
        }
    }
}
