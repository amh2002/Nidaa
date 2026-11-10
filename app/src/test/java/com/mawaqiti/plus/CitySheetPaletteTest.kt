package com.mawaqiti.plus

import androidx.compose.ui.graphics.Color
import com.mawaqiti.plus.ui.theme.CitySheetPalette
import com.mawaqiti.plus.ui.theme.ColorMath
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves the always-dark city sheet meets its own contrast contract on both
 * ends of the gradient, independent of the Living Sky phase.
 */
class CitySheetPaletteTest {

    private fun ratio(first: Color, second: Color): Double =
        ColorMath.contrastRatio(first, second)

    private fun assertMin(actual: Double, minimum: Double, what: String) {
        assertTrue(
            String.format(Locale.US, "%s: %.2f:1 < %.1f:1", what, actual, minimum),
            actual >= minimum
        )
    }

    private fun overTop(alphaColor: Color): Color =
        ColorMath.composite(alphaColor, CitySheetPalette.GradientTop)

    private fun overBottom(alphaColor: Color): Color =
        ColorMath.composite(alphaColor, CitySheetPalette.GradientBottom)

    @Test
    fun primaryTextPassesAaAtBothGradientEnds() {
        assertMin(ratio(CitySheetPalette.Text, CitySheetPalette.GradientTop), 4.5, "text top")
        assertMin(ratio(CitySheetPalette.Text, CitySheetPalette.GradientBottom), 4.5, "text bottom")
    }

    @Test
    fun secondaryTextPassesAaAtBothGradientEnds() {
        assertMin(ratio(overTop(CitySheetPalette.Muted), CitySheetPalette.GradientTop), 4.5, "muted top")
        assertMin(ratio(overBottom(CitySheetPalette.Muted), CitySheetPalette.GradientBottom), 4.5, "muted bottom")
    }

    @Test
    fun selectedCityAndActiveSegmentPass() {
        assertMin(ratio(CitySheetPalette.Accent, CitySheetPalette.GradientTop), 4.5, "accent top")
        assertMin(ratio(CitySheetPalette.Accent, CitySheetPalette.GradientBottom), 4.5, "accent bottom")

        // Active segment: gold label on white 16% over each gradient stop.
        assertMin(
            ratio(CitySheetPalette.Accent, overTop(CitySheetPalette.ActiveFill)), 4.5,
            "active label top"
        )
        assertMin(
            ratio(CitySheetPalette.Accent, overBottom(CitySheetPalette.ActiveFill)), 4.5,
            "active label bottom"
        )
    }

    @Test
    fun defaultTagPassesAgainstBothGradientEnds() {
        assertMin(ratio(overTop(CitySheetPalette.DefaultTag), CitySheetPalette.GradientTop), 4.5, "tag top")
        assertMin(ratio(overBottom(CitySheetPalette.DefaultTag), CitySheetPalette.GradientBottom), 4.5, "tag bottom")
    }

    @Test
    fun placeholderHintStaysReadable() {
        // The spec fixes the hint at white 45%; it is a placeholder/icon, not
        // body copy, so the contract here is the 3:1 graphical threshold.
        assertMin(ratio(overTop(CitySheetPalette.Hint), CitySheetPalette.GradientTop), 3.0, "hint top")
        assertMin(ratio(overBottom(CitySheetPalette.Hint), CitySheetPalette.GradientBottom), 3.0, "hint bottom")
    }

    @Test
    fun dividerIsSubtleButVisible() {
        val composite = overBottom(CitySheetPalette.Divider)
        assertTrue(
            "divider should differ from its background",
            ColorMath.luminance(composite) > ColorMath.luminance(CitySheetPalette.GradientBottom)
        )
    }
}
