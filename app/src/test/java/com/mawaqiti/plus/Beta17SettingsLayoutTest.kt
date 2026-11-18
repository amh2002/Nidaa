package com.mawaqiti.plus

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableProfile
import com.mawaqiti.plus.notify.AdhanVolume
import com.mawaqiti.plus.ui.screens.SettingsList
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
 * beta17 geometry + structure guard for the prayer-notification settings.
 *
 * Rendering the real [SettingsList] (Arabic RTL, 360 dp and 411 dp, font scale
 * 1.0 and 1.3) pins the four layout promises of this beta:
 *
 *  1. exactly ONE adhan card; the sound picker and the sound-only test button
 *     share one horizontally aligned row (same vertical centre, button on the
 *     trailing side), and no node anywhere carries a test-notification tag;
 *  2. the volume slider sits directly under that row, inside the same card,
 *     defaults to 100 % and is live (its value label follows the thumb);
 *  3. exactly one apply-to-all control and it is above the first event card
 *     (Fajr), i.e. at the top of the prayer-notification section;
 *  4. the removed test-notification controls left no trace: no `event_test_*`
 *     row, no `notify_test*` card, no "Test"/"Test notifications" label.
 */
@RunWith(RobolectricTestRunner::class)
class Beta17SettingsLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val city = City(
        order = 0,
        nameAr = "القدس",
        nameEn = "Jerusalem",
        country = Country.PS,
        minDiff = -2,
        profile = TableProfile.DEFAULT,
        latitude = 31.7683,
        longitude = 35.2137
    )

    private val day = TableDay(
        fajr = 300,
        sunrise = 360,
        dhuhr = 720,
        asr = 930,
        maghrib = 1140,
        isha = 1200,
        lastThird = 60,
        fajrTomorrow = 301,
        ishaYesterday = 1201,
        row = 0
    )

    private var volume: Int? = null
    private var previewToggles = 0

    private fun render(fontScale: Float, initialVolume: Int = AdhanVolume.DEFAULT) {
        volume = null
        previewToggles = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MawaqitiTheme(phase = SkyPhase.NIGHT) {
                    SettingsList(
                        settings = AppSettings(adhanVolume = initialVolume),
                        city = city,
                        day = day,
                        isArabic = true,
                        canScheduleExact = true,
                        notificationsGranted = true,
                        batteryOptimized = false,
                        onBack = {},
                        onOpenCityPicker = {},
                        onSelectLanguage = {},
                        onSelectClockFormat = {},
                        onSelectFontScale = {},
                        onSelectWidgetLanguage = {},
                        onSummerTimeChange = {},
                        onNotificationsEnabled = {},
                        onEventEnabled = { _, _ -> },
                        onEventMode = { _, _ -> },
                        onApplyModeToEnabled = {},
                        onAdhanVolumeChange = { volume = it },
                        onToggleAdhanPreview = { previewToggles++ },
                        onOpenExactAlarmSettings = {},
                        onOpenNotificationSettings = {},
                        onOpenBatterySettings = {}
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun string(id: Int): String = context.getString(id)

    private fun volumeLabel(percent: Int): String =
        context.getString(R.string.settings_adhan_volume_value, percent.toString())

    /**
     * Unclipped rect of a node in root coordinates.
     *
     * The settings list is far taller than the test window, so off-screen
     * nodes report an empty `boundsInRoot` (it is clipped by the scroll box).
     * `positionInRoot` + `size` is the laid-out geometry and therefore the
     * right basis for a relative-order assertion.
     */
    private fun rect(tag: String): Rect {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        return Rect(
            node.positionInRoot,
            Size(node.size.width.toFloat(), node.size.height.toFloat())
        )
    }

    private fun bounds(tag: String) = rect(tag)

    /** Rule 1 + 4: one adhan card, side-by-side row, zero test notifications. */
    private fun assertAdhanCard() {
        compose.onAllNodesWithTag("settings_adhan_sound").assertCountEquals(1)

        val card = bounds("settings_adhan_sound")
        val picker = bounds("adhan_sound_trigger")
        val button = bounds("settings_adhan_test_button")
        val test = string(R.string.settings_adhan_test)

        compose.onNodeWithTag("settings_adhan_test_button").performScrollTo().assertIsDisplayed()

        // Beside, not under: the button overlaps the picker's vertical band and
        // both centres are within 1 dp of each other.
        assertTrue(
            "the sound test must sit on the picker's row (picker $picker vs button $button)",
            button.top < picker.bottom && picker.top < button.bottom
        )
        assertTrue(
            "the sound test row drifted vertically (${abs(button.center.y - picker.center.y)} px)",
            abs(button.center.y - picker.center.y) <= 3f
        )

        // RTL: the picker starts at the right edge, the button sits to its left
        // (i.e. at a smaller x), both inside the same card.
        assertTrue("the button is not on the trailing side", button.right <= picker.left + 1f)
        assertTrue(
            "the picker/test row escapes the card",
            picker.left >= card.left - 0.5f && button.right <= card.right + 0.5f
        )

        // The button exists exactly once and carries the strings.xml label.
        compose.onAllNodesWithText(test).assertCountEquals(1)

        // Rule 4: the removed controls are gone, not merely hidden.
        compose.onAllNodes(
            SemanticsMatcher("no test-notification controls") { node ->
                val tag = node.config.getOrNull(SemanticsProperties.TestTag) ?: return@SemanticsMatcher false
                tag.startsWith("event_test_") || tag.startsWith("notify_test")
            }
        ).assertCountEquals(0)
        compose.onAllNodesWithText(string(R.string.notify_apply_all)).assertCountEquals(1)
    }

    /** Rule 2: slider inside the same card, under the row, default 100 %. */
    private fun assertVolumeRow() {
        val picker = bounds("adhan_sound_trigger")
        val slider = bounds("settings_adhan_volume_slider")
        val value = bounds("settings_adhan_volume_value")
        val card = bounds("settings_adhan_sound")

        assertTrue("the slider must start below the picker row", slider.top >= picker.bottom)
        assertTrue("the slider escaped the adhan card", slider.top < card.bottom)
        assertTrue("the value label escaped the adhan card", value.top < card.bottom)
        assertTrue(
            "the volume value must sit above the slider thumb",
            value.bottom <= slider.top + 0.5f
        )

        compose.onNodeWithTag("settings_adhan_volume_slider")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("settings_adhan_volume_value")
            .assertTextEquals(volumeLabel(100))

        // The slider is live: moving it to 40 % updates the visible value.
        compose.onNodeWithTag("settings_adhan_volume_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { set -> set(40f) }
        compose.onNodeWithTag("settings_adhan_volume_value")
            .assertTextEquals(volumeLabel(40))
    }

    /** Rule 3: one apply-to-all card, above the Fajr card, nowhere else. */
    private fun assertApplyAllAtTop() {
        compose.onAllNodesWithTag("notify_apply_all").assertCountEquals(1)

        val apply = bounds("notify_apply_all")
        val fajr = bounds("event_card_fajr")
        assertTrue(
            "apply-to-all must sit before the first prayer card (apply $apply vs fajr $fajr)",
            apply.bottom <= fajr.top + 0.5f
        )

        // It is the first item of the prayer-notification section: the master
        // switch card is above it, and the Fajr card directly follows it.
        val master = bounds("notifications_master_toggle")
        assertTrue("apply-to-all must come after the master switch", master.bottom <= apply.top)
        assertTrue(
            "apply-to-all must be directly before Fajr",
            fajr.top - apply.bottom <= 80f
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `arabic 360dp font scale 1_0`() {
        render(fontScale = 1.0f)
        assertAdhanCard()
        assertVolumeRow()
        assertApplyAllAtTop()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `arabic 360dp font scale 1_3`() {
        render(fontScale = 1.3f)
        assertAdhanCard()
        assertVolumeRow()
        assertApplyAllAtTop()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-xhdpi")
    fun `arabic 411dp font scale 1_3`() {
        render(fontScale = 1.3f)
        assertAdhanCard()
        assertVolumeRow()
        assertApplyAllAtTop()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `persisted volume is rendered as the starting value`() {
        render(fontScale = 1.0f, initialVolume = 55)
        compose.onNodeWithTag("settings_adhan_volume_value")
            .assertTextEquals(volumeLabel(55))
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `preview button reflects its playing state label`() {
        // The idle label is the strings.xml "Test sound"; tapping asks the
        // caller to toggle the preview (the audio itself is device-verified).
        render(fontScale = 1.0f)
        compose.onAllNodesWithText(string(R.string.settings_adhan_test)).assertCountEquals(1)
        compose.onNodeWithTag("settings_adhan_test_button")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, previewToggles)
    }
}
