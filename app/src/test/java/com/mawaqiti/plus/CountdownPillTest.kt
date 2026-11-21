package com.mawaqiti.plus

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.ui.screens.HeroCard
import com.mawaqiti.plus.ui.screens.HeroState
import com.mawaqiti.plus.ui.theme.GoldenPalettes
import com.mawaqiti.plus.ui.theme.MawaqitiTheme
import com.mawaqiti.plus.ui.theme.SkyPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta23.1: the "متبقي" pill is ONE text, so its two runs cannot disagree.
 *
 * beta17 shipped one concatenated `Text`, beta18/beta23 split it into two
 * `Text`s and aligned them with `Modifier.alignByBaseline()`. The rendered
 * beta23 pill already had both runs on the same pixel row, so no alignment
 * attempt could change what the owner saw. The pill is now a single [Text]
 * again, but with the label and the digits as two styled runs of one
 * `AnnotatedString`, which makes the shared baseline a layout invariant rather
 * than an alignment contract: one text layout, one baseline, one line.
 *
 * These tests assert that invariant directly (exactly one text node holding
 * both runs, one line, both runs reporting line 0) and that the RTL order and
 * the tabular digit width that the alignment attempts were protecting are
 * still intact.
 */
@RunWith(RobolectricTestRunner::class)
class CountdownPillTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Ticked by the tabular-width test; everything else uses the initial value. */
    private val secondsLeft = mutableStateOf(22 * 60L)

    private fun render(fontScale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
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
                                secondsLeft = secondsLeft.value,
                                progress = 0.42f
                            ),
                            progress = 0.42f,
                            isArabic = true,
                            clockFormat = ClockFormat.H24,
                            palette = GoldenPalettes.Night,
                            nowText = "10:55",
                            celestial = com.mawaqiti.plus.ui.theme.CelestialPhase.MOON
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun pillLayout(): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(PILL)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { get -> get(layouts) }
        assertTrue("the pill must expose one text layout", layouts.isNotEmpty())
        return layouts.first()
    }

    /** Width of the pill's text layout in px - the run that must not jitter. */
    private fun pillWidth(): Int = pillLayout().size.width

    private fun assertSingleTextPill(widthDp: Int, fontScale: Float) {
        render(fontScale)

        val label = context.getString(R.string.remaining_short)
        val expected = "$label 00:22:00"

        // One node carries the whole pill: the previous two-Text row (and its
        // per-run tags) must be gone, otherwise a baseline offset is possible.
        compose.onAllNodesWithTag("hero_countdown_caption").assertCountEquals(0)
        compose.onAllNodesWithTag("hero_countdown_value").assertCountEquals(0)
        compose.onNodeWithTag(PILL).assertTextEquals(expected)

        // One line means one baseline for both runs - by construction.
        val layout = pillLayout()
        assertEquals("$widthDp/$fontScale must stay on one line", 1, layout.lineCount)
        assertEquals("label run is on the single line", 0, layout.getLineForOffset(0))
        assertEquals(
            "digits run is on the single line",
            0,
            layout.getLineForOffset(layout.layoutInput.text.text.length - 1)
        )

        // RTL reading order: the label is the leading, right-most run and the
        // LTR digits follow it to the left.
        val labelBox = layout.getBoundingBox(0)
        val digitBox = layout.getBoundingBox(layout.layoutInput.text.text.length - 1)
        assertTrue(
            "label must stay right of the digits (label $labelBox digits $digitBox)",
            labelBox.left >= digitBox.right - 1f
        )

        // No wrap, and the pill still lives inside the hero card.
        val card = compose.onNodeWithTag("hero_card").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "pill text escapes the hero card",
            layout.size.width <= card.width
        )

        println(
            "beta23.1-pill width=${widthDp}dp fontScale=$fontScale " +
                "lineCount=${layout.lineCount} baseline=${layout.getLineBaseline(0)} " +
                "textSize=${layout.size} cardWidth=${card.width}"
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `single text pill at 360dp fontScale 1_0`() {
        assertSingleTextPill(360, 1.0f)
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `single text pill at 360dp fontScale 1_3`() {
        assertSingleTextPill(360, 1.3f)
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-xhdpi")
    fun `single text pill at 411dp fontScale 1_0`() {
        assertSingleTextPill(411, 1.0f)
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w480dp-h1000dp-xhdpi")
    fun `single text pill at 480dp fontScale 1_3`() {
        assertSingleTextPill(480, 1.3f)
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-xhdpi")
    fun `tabular digits keep the pill width while ticking`() {
        render(1.0f)
        val before = pillWidth()
        secondsLeft.value = 11 * 3600L + 11 * 60L + 11L
        compose.waitForIdle()
        compose.onNodeWithTag(PILL)
            .assertTextEquals(context.getString(R.string.remaining_short) + " 11:11:11")
        val after = pillWidth()
        println("beta23.1-tabular pillWidth 00:22:00=$before 11:11:11=$after")
        assertEquals("the pill must not jitter while the countdown ticks", before, after)
    }

    private companion object {
        const val PILL = "hero_countdown_pill"
    }
}
