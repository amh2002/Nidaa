package com.mawaqiti.plus

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import kotlin.math.abs

/**
 * beta18: "متبقي" and the countdown digits share one visual baseline.
 *
 * beta17 shipped this pill as ONE concatenated `Text` and claimed the baseline
 * was therefore correct. That claim could not fail: a single text run has no
 * two baselines to compare, so the guard only asserted "one child" and the
 * reported digit lift stayed on screen. The pill is now two [androidx.compose.material3.Text]s
 * in a [androidx.compose.foundation.layout.Row], BOTH carrying
 * `Modifier.alignByBaseline()`, so the baseline equality is a real contract and
 * this test measures it: each text's first-line baseline (layout baseline +
 * its position in the root) must be identical at font scale 1.0 and 1.3.
 */
@RunWith(RobolectricTestRunner::class)
class CountdownPillTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

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
                                secondsLeft = 22 * 60L,
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

    /** Root-space y of the first-line baseline of the text at [tag]. */
    private fun baselineOf(tag: String): Float {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { get ->
            get(layouts)
        }
        assertTrue("no text layout for $tag", layouts.isNotEmpty())
        val y = compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y
        return y + layouts.first().getLineBaseline(0)
    }

    private fun assertSharedBaseline(widthDp: Int, fontScale: Float) {
        render(fontScale)

        compose.onNodeWithTag("hero_countdown_pill").assertExists()

        val caption = context.getString(R.string.remaining_short)
        val digits = "00:22:00"
        compose.onAllNodesWithText(caption).assertCountEquals(1)
        compose.onAllNodesWithText(digits).assertCountEquals(1)

        // RTL reading order: the caption is the leading (right-most) child.
        val captionBox = compose.onNodeWithTag("hero_countdown_caption")
            .fetchSemanticsNode().boundsInRoot
        val valueBox = compose.onNodeWithTag("hero_countdown_value")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "caption must stay first in RTL order (caption $captionBox vs digits $valueBox)",
            captionBox.left >= valueBox.right - 1f
        )
        assertTrue(
            "countdown must not overlap the caption",
            valueBox.right <= captionBox.left + 1f
        )

        // The contract under test: identical first-line baselines.
        val captionBaseline = baselineOf("hero_countdown_caption")
        val valueBaseline = baselineOf("hero_countdown_value")
        println(
            "beta18-baseline width=${widthDp}dp fontScale=$fontScale " +
                "caption=$captionBaseline value=$valueBaseline " +
                "offset=${abs(captionBaseline - valueBaseline)}"
        )
        assertEquals(
            "baseline offset at ${widthDp}dp fontScale $fontScale",
            0f,
            abs(captionBaseline - valueBaseline),
            0.01f
        )

        // The pill still lives inside the hero card.
        val pill = compose.onNodeWithTag("hero_countdown_pill").fetchSemanticsNode().boundsInRoot
        val card = compose.onNodeWithTag("hero_card").fetchSemanticsNode().boundsInRoot
        assertTrue("pill escapes the hero card", pill.left >= card.left && pill.right <= card.right)
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `shared baseline at 360dp fontScale 1_0`() {
        assertSharedBaseline(360, 1.0f)
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `shared baseline at 360dp fontScale 1_3`() {
        assertSharedBaseline(360, 1.3f)
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-xhdpi")
    fun `shared baseline at 411dp fontScale 1_0`() {
        assertSharedBaseline(411, 1.0f)
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w480dp-h1000dp-xhdpi")
    fun `shared baseline at 480dp fontScale 1_3`() {
        assertSharedBaseline(480, 1.3f)
    }
}
