package com.mawaqiti.plus

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.ui.screens.SearchField
import com.mawaqiti.plus.ui.theme.GoldenPalettes
import com.mawaqiti.plus.ui.theme.MawaqitiTheme
import com.mawaqiti.plus.ui.theme.SkyPhase
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * beta17: vertical centring of the city-search hint.
 *
 * Before this beta the hint sat in a taller `Box` that only aligned its start
 * edge, so the ink line rode ~4 px above the field centre at 411 dp. The field
 * now aligns the placeholder's content, which must hold at every width and
 * font scale. The assertions use the semantics rectangles of the field, the
 * hint and the magnifier, so they do not depend on real Arabic glyph metrics.
 */
@RunWith(RobolectricTestRunner::class)
class CitySearchHintTest {

    @get:Rule
    val compose = createComposeRule()

    /** Density captured from the composition, so tolerances are expressed in dp. */
    private var density: Density = Density(1f, 1f)

    @Composable
    private fun Field(fontScale: Float) {
        val current = LocalDensity.current
        density = Density(current.density, fontScale)
        val placeholder = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.city_search_hint)
        CompositionLocalProvider(LocalDensity provides density) {
            MawaqitiTheme(phase = SkyPhase.NIGHT) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    SearchField(
                        query = "",
                        onQueryChange = {},
                        placeholder = placeholder,
                        clearDescription = "",
                        palette = GoldenPalettes.Night
                    )
                }
            }
        }
    }

    private fun assertCentred(fontScale: Float) {
        compose.setContent { Field(fontScale) }
        compose.waitForIdle()

        val field = compose.onNodeWithTag("city_search_field").fetchSemanticsNode().boundsInRoot
        val hint = compose.onNodeWithTag("city_search_hint", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val icon = compose.onNodeWithTag("city_search_icon", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        val topGap = hint.top - field.top
        val bottomGap = field.bottom - hint.bottom
        // The hint may sit up to ~4 dp from dead centre: the documented 1.5 dp
        // optical lift plus the font's line box (Robolectric has no real Arabic
        // metrics). Anything like the old top alignment differs by far more.
        val tolerance = with(density) { 4.dp.toPx() }
        assertTrue(
            "hint is off-centre at fontScale $fontScale: above ${topGap}px, below ${bottomGap}px",
            abs(topGap - bottomGap) <= tolerance
        )

        // The magnifier is centred on the field's axis.
        val iconCentreDelta = abs(icon.center.y - field.center.y)
        assertTrue(
            "magnifier is off-centre at fontScale $fontScale (${iconCentreDelta}px)",
            iconCentreDelta <= 1.5f
        )
        assertTrue(
            "hint and magnifier are not on one optical line at fontScale $fontScale",
            abs(hint.center.y - icon.center.y) <= tolerance
        )

        // The hint never escapes the field.
        assertTrue(hint.top >= field.top && hint.bottom <= field.bottom)
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h640dp-xxhdpi")
    fun `hint is centred at fontScale 1_0`() = assertCentred(1.0f)

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h640dp-xxhdpi")
    fun `hint is centred at fontScale 1_3`() = assertCentred(1.3f)

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xxhdpi")
    fun `hint is centred at 360dp`() = assertCentred(1.3f)

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w480dp-h640dp-xxhdpi")
    fun `hint is centred at 480dp`() = assertCentred(1.0f)
}
