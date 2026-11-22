package com.mawaqiti.plus

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableProfile
import com.mawaqiti.plus.ui.screens.SettingsList
import com.mawaqiti.plus.ui.theme.MawaqitiTheme
import com.mawaqiti.plus.ui.theme.SkyPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta18 geometry guard for the Settings list.
 *
 * Pins the three layout promises of this beta on the real [SettingsList]:
 *
 *  1. the "التنبيهات" master card is the FIRST card of the notification group,
 *     directly above the "صوت الأذان" card, and exists exactly once;
 *  2. the group order below it stays master -> sound -> apply-to-all -> Fajr;
 *  3. the battery status card wraps its content, never clips its description,
 *     and never collides with the About card below it.
 */
@RunWith(RobolectricTestRunner::class)
class Beta18SettingsLayoutTest {

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

    private fun render(fontScale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MawaqitiTheme(phase = SkyPhase.NIGHT) {
                    SettingsList(
                        settings = AppSettings(),
                        city = city,
                        day = day,
                        isArabic = true,
                        canScheduleExact = true,
                        notificationsGranted = true,
                        batteryOptimized = true,
                        initiallyExpanded = true,
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
                        onAdhanVolumeChange = {},
                        onToggleAdhanPreview = {},
                        onOpenExactAlarmSettings = {},
                        onOpenNotificationSettings = {},
                        onOpenBatterySettings = {}
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    /**
     * Unclipped rect in root coordinates: the list is taller than the test
     * window, so `boundsInRoot` is clipped but `positionInRoot` + `size` is the
     * real laid-out geometry.
     */
    private fun rect(tag: String): Rect {
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        return Rect(
            node.positionInRoot,
            Size(node.size.width.toFloat(), node.size.height.toFloat())
        )
    }

    private fun dpToPx(dp: Float): Float = dp * compose.density.density

    private fun layout(tag: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { get ->
                get(layouts)
            }
        assertTrue("no text layout for $tag", layouts.isNotEmpty())
        return layouts.first()
    }

    private fun assertNewCardOrder() {
        compose.onAllNodesWithTag("settings_notifications_master_card").assertCountEquals(1)
        compose.onAllNodesWithTag("notifications_master_toggle").assertCountEquals(1)

        val master = rect("settings_notifications_master_card")
        val sound = rect("settings_adhan_sound")
        val applyAll = rect("notify_apply_all")
        val fajr = rect("event_card_fajr")

        assertTrue("master card must be above the sound card", master.bottom <= sound.top)
        assertTrue("sound card must be above apply-to-all", sound.bottom <= applyAll.top)
        assertTrue("apply-to-all must be above Fajr", applyAll.bottom <= fajr.top)

        // One shared 12 dp card gap between each pair.
        assertEquals(
            "master -> sound gap",
            dpToPx(12f),
            sound.top - master.bottom,
            1f
        )
        assertEquals(
            "sound -> apply gap",
            dpToPx(12f),
            applyAll.top - sound.bottom,
            1f
        )
    }

    private fun assertBatteryCard() {
        compose.onAllNodesWithTag("status_battery_card").assertCountEquals(1)

        val battery = rect("status_battery_card")
        val batteryDesc = rect("status_battery_card_description")
        val about = rect("settings_about")

        // Content-driven height: the description sits inside the card's padded
        // content box and is never truncated.
        val descLayout = layout("status_battery_card_description")
        assertFalse("battery description must not overflow its height", descLayout.didOverflowHeight)
        assertTrue(
            "battery description escapes the card bottom ($batteryDesc vs $battery)",
            batteryDesc.bottom <= battery.bottom - dpToPx(16f) + 0.5f
        )
        assertTrue(
            "battery description escapes the card top",
            batteryDesc.top >= battery.top + dpToPx(16f) - 0.5f
        )

        // beta25: the battery row is the last item of the «التنبيهات» section and
        // the About card now sits under its own section header, so the contract
        // is "at least one card gap, never a collision".
        assertTrue("About must sit below the battery card", battery.bottom <= about.top)
        println(
            "beta18-battery card=${battery.height}px desc=${batteryDesc.height}px " +
                "descBottomMargin=${battery.bottom - batteryDesc.bottom}px " +
                "gap=${about.top - battery.bottom}px cardGap=${dpToPx(12f)}px " +
                "descLines=${layout("status_battery_card_description").lineCount}"
        )
        assertTrue(
            "battery -> about gap must be at least one card gap",
            about.top - battery.bottom >= dpToPx(12f)
        )
    }

    private fun assertAll() {
        assertNewCardOrder()
        assertBatteryCard()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `360dp fontScale 1_0`() {
        render(1.0f)
        assertAll()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
    fun `360dp fontScale 1_3`() {
        render(1.3f)
        assertAll()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-xhdpi")
    fun `411dp fontScale 1_0`() {
        render(1.0f)
        assertAll()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-xhdpi")
    fun `411dp fontScale 1_3`() {
        render(1.3f)
        assertAll()
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w480dp-h1000dp-xhdpi")
    fun `480dp fontScale 1_3`() {
        render(1.3f)
        assertAll()
    }
}
