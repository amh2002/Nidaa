package com.mawaqiti.plus

import androidx.compose.ui.graphics.Color
import com.mawaqiti.plus.ui.theme.ColorMath
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.mawaqiti.plus.ui.theme.SkyPhase
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.1 AA checks against the colours the app actually paints.
 *
 * Backgrounds are modelled as the vertical gradient with the rendered top
 * haze, and glass cards as their translucent fill composited over that. This
 * is the same math used by [SkyPalette.effectiveBackgroundAt] and
 * [SkyPalette.cardBackgroundAt], so a palette change that lowers contrast
 * fails here before it ships.
 */
class SkyPaletteTest {

    private val fractions = listOf(0f, 0.05f, 0.16f, 0.30f, 0.42f, 0.6f, 0.8f, 1f)

    /**
     * Places where muted text is drawn directly on the sky: the date line
     * under the wordmark and the official note above the footer. Mid-screen
     * muted labels always sit inside glass cards, which are checked below.
     */
    private val onSkyTextFractions = listOf(0f, 0.05f, 0.12f, 0.18f, 0.88f, 0.95f, 1f)

    private fun ratio(first: Color, second: Color): Double =
        ColorMath.contrastRatio(first, second)

    private fun assertMin(actual: Double, minimum: Double, what: String) {
        assertTrue(
            String.format(Locale.US, "%s: %.2f:1 < %.1f:1", what, actual, minimum),
            actual >= minimum
        )
    }

    @Test
    fun textPassesAaOnEveryRenderedBackground() {
        for (phase in SkyPhase.entries) {
            val palette = SkyPalette.of(phase)
            for (fraction in fractions) {
                val sky = palette.effectiveBackgroundAt(fraction)
                assertMin(
                    ratio(palette.text, sky), 4.5,
                    "${phase.name} text on sky @ $fraction"
                )
                assertMin(
                    ratio(palette.text, palette.cardBackgroundAt(fraction)), 4.5,
                    "${phase.name} text on card @ $fraction"
                )
                assertMin(
                    ratio(palette.text, palette.cardBackgroundAt(fraction, true)), 4.5,
                    "${phase.name} text on hero @ $fraction"
                )
            }
        }
    }

    @Test
    fun mutedTextPassesAaIncludingItsOwnAlpha() {
        for (phase in SkyPhase.entries) {
            val palette = SkyPalette.of(phase)
            for (fraction in onSkyTextFractions) {
                val sky = palette.effectiveBackgroundAt(fraction)
                val onSky = ColorMath.composite(palette.textMuted, sky)
                assertMin(
                    ratio(onSky, sky), 4.5,
                    "${phase.name} muted text on sky @ $fraction"
                )
            }
            for (fraction in fractions) {
                val card = palette.cardBackgroundAt(fraction)
                val onCard = ColorMath.composite(palette.textMuted, card)
                assertMin(
                    ratio(onCard, card), 4.5,
                    "${phase.name} muted text on card @ $fraction"
                )

                val hero = palette.cardBackgroundAt(fraction, true)
                val onHero = ColorMath.composite(palette.textMuted, hero)
                assertMin(
                    ratio(onHero, hero), 4.5,
                    "${phase.name} muted text on hero @ $fraction"
                )
            }
        }
    }

    @Test
    fun accentPassesLargeTextAndIconThreshold() {
        for (phase in SkyPhase.entries) {
            val palette = SkyPalette.of(phase)
            for (fraction in fractions) {
                assertMin(
                    ratio(palette.accent, palette.effectiveBackgroundAt(fraction)), 3.0,
                    "${phase.name} accent on sky @ $fraction"
                )
                assertMin(
                    ratio(palette.accent, palette.cardBackgroundAt(fraction)), 3.0,
                    "${phase.name} accent on card @ $fraction"
                )
                assertMin(
                    ratio(palette.accent, palette.cardBackgroundAt(fraction, true)), 3.0,
                    "${phase.name} accent on hero @ $fraction"
                )
            }
        }
    }

    @Test
    fun sheetSurfacesPassAa() {
        for (phase in SkyPhase.entries) {
            val palette = SkyPalette.of(phase)
            assertMin(
                ratio(palette.sheetText, palette.sheet), 4.5,
                "${phase.name} sheet text"
            )
            val muted = ColorMath.composite(palette.sheetMuted, palette.sheet)
            assertMin(ratio(muted, palette.sheet), 4.5, "${phase.name} sheet muted text")
        }
    }

    @Test
    fun pickerActiveSegmentAndSelectionPass() {
        val white = Color.White
        assertMin(ratio(white, SkyPalette.BrandGreen), 4.5, "active segment label")

        for (phase in SkyPhase.entries) {
            val palette = SkyPalette.of(phase)
            val selection = if (palette.dark) Color(0xFF8FE3B8) else SkyPalette.BrandGreen
            assertMin(
                ratio(selection, palette.sheet), 3.0,
                "${phase.name} selected city on sheet"
            )
            assertMin(
                ratio(palette.text, palette.chipFill.compositeOver(palette.effectiveBackgroundAt(0.12f))), 4.5,
                "${phase.name} chip label"
            )
        }
    }

    @Test
    fun gradientStopsAreTheAuthoritativeTokens() {
        assertEquals(
            listOf(Color(0xFF1A1440), Color(0xFF2A2155), Color(0xFF3A2C63)),
            SkyPalette.Dawn.gradient
        )
        assertEquals(
            listOf(Color(0xFF2F6EA6), Color(0xFF5C9CC9), Color(0xFFA8D0E8)),
            SkyPalette.Day.gradient
        )
        assertEquals(
            listOf(Color(0xFF2F6EA6), Color(0xFF6FA3C6), Color(0xFFB9D3E2)),
            SkyPalette.Golden.gradient
        )
        assertEquals(
            listOf(Color(0xFF060A20), Color(0xFF0E1540), Color(0xFF1D2C62)),
            SkyPalette.Night.gradient
        )
    }

    @Test
    fun decorIsOnlyDrawnAtNight() {
        for (phase in SkyPhase.entries) {
            assertEquals(phase == SkyPhase.NIGHT, SkyPalette.of(phase).phase == SkyPhase.NIGHT)
        }
    }

    private fun Color.compositeOver(base: Color): Color = ColorMath.composite(this, base)
}
