package com.mawaqiti.plus

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta17: the countdown pill keeps "متبقي" and the digits on one baseline.
 *
 * The two parts are rendered by ONE `Text`, so they cannot drift apart: equal
 * baselines are a property of the text run, not of a row alignment. The guard
 * here is structural - the pill must keep exactly one text child carrying both
 * the caption and the `HH:MM:SS` digits - so a future refactor into two Texts
 * (the shape that makes digits look raised) fails immediately.
 *
 * The beta17 pixel measurement (screenshots, 411 dp) found the pre-existing
 * baselines already equal at y = 814 px for both the Arabic word and the
 * digits; the perceived lift comes from lining figures reaching cap height
 * while the Arabic body sits on the x-height. Nothing moves, so nothing can
 * change size, colour, family or RTL order.
 */
@RunWith(RobolectricTestRunner::class)
class CountdownPillTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun render(screenWidthDp: Int, fontScale: Float) {
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
                            nowText = "10:55"
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun assertSingleText() {
        compose.onNodeWithTag("hero_countdown_pill").assertExists()

        // Exactly one child: the caption and the digits share one text run.
        compose.onNodeWithTag("hero_countdown_pill").onChildren().assertCountEquals(1)

        // ... whose content is the Arabic caption followed by the countdown.
        val expected = context.getString(R.string.remaining_short) + " " + "00:22:00"
        compose.onAllNodesWithText(expected).assertCountEquals(1)

        // RTL reading order is unchanged: caption first in the string.
        assertTrue(expected.startsWith(context.getString(R.string.remaining_short)))

        // The pill stays inside the hero card.
        val pill = compose.onNodeWithTag("hero_countdown_pill").fetchSemanticsNode().boundsInRoot
        val card = compose.onNodeWithTag("hero_card").fetchSemanticsNode().boundsInRoot
        assertTrue("pill escapes the hero card", pill.left >= card.left && pill.right <= card.right)
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `single text at 360dp fontScale 1_0`() {
        render(360, 1.0f)
        assertSingleText()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `single text at 360dp fontScale 1_3`() {
        render(360, 1.3f)
        assertSingleText()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w480dp-h1000dp-xhdpi")
    fun `single text at 480dp fontScale 1_3`() {
        render(480, 1.3f)
        assertSingleText()
    }
}
