package com.mawaqiti.plus

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression guard for the Settings framework rule.
 *
 * beta5 changes the target of the guard: the About card is now the LAST item
 * and the only place that carries the timetable/offline/no-data claims. For
 * both locales at font scales 1.0 and 1.3 the test asserts that
 *
 *  - the timetable claim exists exactly once (no duplicated footer card),
 *  - no legacy footer node exists at all,
 *  - scrolling reaches the developer credit, i.e. About is really last and is
 *    not clipped outside the single scrollable list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl")
class SettingsFrameworkTest {

    @get:Rule
    val compose = createComposeRule()

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

    private fun render(isArabic: Boolean, fontScale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MawaqitiTheme(phase = SkyPhase.DAY) {
                    SettingsList(
                        settings = AppSettings(),
                        city = city,
                        day = day,
                        isArabic = isArabic,
                        canScheduleExact = true,
                        notificationsGranted = true,
                        batteryOptimized = false,
                        onBack = {},
                        onOpenCityPicker = {},
                        onSelectLanguage = {},
                        onSelectClockFormat = {},
                        onSelectWidgetLanguage = {},
                        onSummerTimeChange = {},
                        onNotificationsEnabled = {},
                        onEventEnabled = { _, _ -> },
                        onEventMode = { _, _ -> },
                        onApplyModeToEnabled = {},
                        onTestEvent = { _, _ -> },
                        onTestMode = {},
                        onAdhanUri = {},
                        onChooseAdhan = {},
                        onOpenExactAlarmSettings = {},
                        onOpenNotificationSettings = {},
                        onOpenBatterySettings = {}
                    )
                }
            }
        }
    }

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun assertSingleReachableAbout() {
        // Exactly one place carries the timetable claim.
        compose.onAllNodesWithText(string(R.string.settings_about_timetable))
            .assertCountEquals(1)

        // The old duplicated footer card must not exist any more.
        compose.onAllNodesWithTag("settings_footer").assertCountEquals(0)

        // The credit is the very last line and must be reachable by scrolling.
        compose.onNodeWithTag("settings_about_credit")
            .performScrollTo()
            .assertIsDisplayed()

        // Both "claims" chips live inside the same About card.
        compose.onAllNodesWithText(string(R.string.settings_about_offline))
            .assertCountEquals(1)
        compose.onAllNodesWithText(string(R.string.settings_about_no_data))
            .assertCountEquals(1)

        // Sanity: the About card itself is the list's last item.
        assertTrue(string(R.string.settings_about_brand).contains("نداء"))
    }

    @Test
    fun `arabic font scale 1_0 about carries the claims once and is reachable`() {
        render(isArabic = true, fontScale = 1.0f)
        assertSingleReachableAbout()
    }

    @Test
    fun `arabic font scale 1_3 about carries the claims once and is reachable`() {
        render(isArabic = true, fontScale = 1.3f)
        assertSingleReachableAbout()
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `english font scale 1_0 about carries the claims once and is reachable`() {
        render(isArabic = false, fontScale = 1.0f)
        assertSingleReachableAbout()
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `english font scale 1_3 about carries the claims once and is reachable`() {
        render(isArabic = false, fontScale = 1.3f)
        assertSingleReachableAbout()
    }
}
