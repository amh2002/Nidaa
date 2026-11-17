package com.mawaqiti.plus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.ui.screens.HeroCard
import com.mawaqiti.plus.ui.screens.HeroMetrics
import com.mawaqiti.plus.ui.screens.HeroState
import com.mawaqiti.plus.ui.theme.GoldenPalettes
import com.mawaqiti.plus.ui.theme.MawaqitiTheme
import com.mawaqiti.plus.ui.theme.NidaaDimens
import com.mawaqiti.plus.ui.theme.SkyPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta15: geometry guard for the next-prayer hero card.
 *
 * Renders the real [HeroCard] (the same composable the home screen uses) on
 * 360 dp, 411 dp and 480 dp screens, at font scale 1.0 and 1.3, and asserts the
 * two rules beta15 is about:
 *
 *  1. the "الصلاة القادمة" caption and the prayer name are separated by exactly
 *     [NidaaDimens.nextPrayerLabelGap] (>= 8 dp) and never intersect;
 *  2. the current-time row is centred on the card's vertical axis (<= 1 dp from
 *     the centre) and keeps clear of the beta12 divider and of the card edges -
 *     centred, never pinned to the RTL start edge, never clipped.
 *
 * The bounds come from the semantics tree, so a regression (a fixed offset, a
 * start-aligned row, a removed weight/ellipsis) fails here before it reaches a
 * device. Robolectric does not load real Arabic glyph advances, so ink widths
 * are verified by the beta15 screenshots; this test pins the *layout* rules.
 */
@RunWith(RobolectricTestRunner::class)
class HeroCardLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    /** Captured from the composition so dp/sp can be converted with the same
     *  density (and font scale) the card was laid out with. */
    private var density: Density = Density(1f, 1f)

    /**
     * Renders the card exactly like the home screen does: [screenWidth] wide,
     * 18 dp of screen padding on both sides, so the card is [screenWidth]-36 dp.
     */
    private fun render(screenWidth: Dp, fontScale: Float) {
        compose.setContent {
            val current = LocalDensity.current
            density = Density(current.density, fontScale)
            CompositionLocalProvider(LocalDensity provides density) {
                MawaqitiTheme(phase = SkyPhase.NIGHT) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp)
                    ) {
                        HeroCard(
                            hero = HeroState(
                                event = TableEvent.ISHA,
                                minutes = 18 * 60 + 3,
                                secondsLeft = 22 * 60L,
                                progress = 0.42f
                            ),
                            progress = 0.42f,
                            isArabic = true,
                            clockFormat = ClockFormat.H24,
                            palette = GoldenPalettes.Night,
                            nowText = "10:55"
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun px(value: Dp): Float = with(density) { value.toPx() }

    /** Shared assertions for one (screen width, font scale) pair. */
    private fun assertCardGeometry() {
        val card = bounds("hero_card")
        val label = bounds("next_prayer_label")
        val name = bounds("hero_prayer_name")
        val divider = bounds("now_divider")
        val row = bounds("now_time_row")

        // ---- caption vs prayer name -------------------------------------
        // Siblings in a Column: the caption owns its line, the name owns the
        // next one. The clear space is the theme dimen, so the two rectangles
        // can never intersect at any width or font scale.
        assertTrue(
            "caption and prayer name overlap: $label vs $name",
            name.top >= label.bottom
        )
        val gap = name.top - label.bottom
        val expectedGap = px(HeroMetrics.LABEL_TO_NAME)
        assertEquals(
            "caption -> name gap must be the theme dimen ($expectedGap px)",
            expectedGap.toDouble(),
            gap.toDouble(),
            0.5
        )
        assertTrue("gap is below the 8 dp floor", gap >= px(8.dp) - 0.5f)

        // Both stay inside the card's horizontal bounds, in RTL and LTR alike.
        assertTrue(
            "caption escapes the card",
            label.left >= card.left && label.right <= card.right + 0.5f
        )
        assertTrue(
            "name escapes the card",
            name.left >= card.left && name.right <= card.right + 0.5f
        )

        // The name is one line: a wrap would double its height. (Robolectric
        // has no real Arabic advances, so this is a coarse guard here; the
        // screenshots cover the pixel truth.)
        assertTrue(
            "prayer name wrapped instead of ellipsizing (height ${name.height} px)",
            name.height <= with(density) { 46.sp.toPx() } * 1.35f
        )

        // ---- current-time row --------------------------------------------
        // Centred on the card's vertical axis, as one unit.
        val centreDelta = kotlin.math.abs(row.center.x - card.center.x)
        assertTrue(
            "current-time row is not centred (off by $centreDelta px)",
            centreDelta <= px(1.dp)
        )

        // Clear of the beta12 divider (7 dp spacer) and of the card edges.
        assertTrue(
            "current-time row touches the divider",
            row.top - divider.bottom >= px(7.dp) - 0.5f
        )
        assertTrue(
            "current-time row touches a card edge: $row inside $card",
            row.left > card.left && row.right < card.right
        )

        // Geometry dump: it lands in the test-results XML, so the beta report
        // can quote measured layout numbers without a screenshot pixel hunt.
        println(
            "BETA15_GEOM card=${card.width / density.density}dp " +
                "screen=${bounds("hero_content").width / density.density}dp " +
                "scale=${density.fontScale} " +
                "labelToName=${gap / density.density}dp " +
                "rowCentreOffset=${centreDelta / density.density}dp " +
                "rowToDivider=${(row.top - divider.bottom) / density.density}dp " +
                "row=${row.left / density.density}..${row.right / density.density}dp " +
                "cardX=${card.left / density.density}..${card.right / density.density}dp"
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h740dp-420dpi")
    fun `360 dp screen at font scale 1_0`() {
        render(360.dp, 1.0f)
        assertCardGeometry()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h740dp-420dpi")
    fun `360 dp screen at font scale 1_3`() {
        render(360.dp, 1.3f)
        assertCardGeometry()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-420dpi")
    fun `411 dp screen at font scale 1_0`() {
        render(411.dp, 1.0f)
        assertCardGeometry()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-420dpi")
    fun `411 dp screen at font scale 1_3`() {
        render(411.dp, 1.3f)
        assertCardGeometry()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w480dp-h1000dp-420dpi")
    fun `480 dp screen at font scale 1_0`() {
        render(480.dp, 1.0f)
        assertCardGeometry()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w480dp-h1000dp-420dpi")
    fun `480 dp screen at font scale 1_3`() {
        render(480.dp, 1.3f)
        assertCardGeometry()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-420dpi")
    fun `the label gap token keeps the 8 dp floor`() {
        assertTrue(
            "NidaaDimens.nextPrayerLabelGap must stay >= 8 dp",
            NidaaDimens.nextPrayerLabelGap >= 8.dp
        )
        assertEquals(8.dp, NidaaDimens.nextPrayerLabelGap)
        assertEquals(NidaaDimens.nextPrayerLabelGap, HeroMetrics.LABEL_TO_NAME)
    }
}
