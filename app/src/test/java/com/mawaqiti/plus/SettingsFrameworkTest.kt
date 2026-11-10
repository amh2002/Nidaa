package com.mawaqiti.plus

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression guard for the beta4 Settings framework rule.
 *
 * Both locales at font scales 1.0 and 1.3 must show EXACTLY ONE footer text
 * node, and the single scrollable list must reach the footer without clipping.
 * If someone reintroduces a Box with stacked Texts or a duplicate footer, the
 * count assertion fails; if they put the footer outside the scroll list, the
 * scroll assertion fails.
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

    private fun footerText(): String =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.settings_footer)

    private fun assertSingleReachableFooter() {
        val footer = footerText()
        compose.onAllNodesWithText(footer).assertCountEquals(1)
        compose.onNodeWithTag("settings_footer")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `arabic font scale 1_0 single footer and reachable`() {
        render(isArabic = true, fontScale = 1.0f)
        assertSingleReachableFooter()
    }

    @Test
    fun `arabic font scale 1_3 single footer and reachable`() {
        render(isArabic = true, fontScale = 1.3f)
        assertSingleReachableFooter()
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `english font scale 1_0 single footer and reachable`() {
        render(isArabic = false, fontScale = 1.0f)
        assertSingleReachableFooter()
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `english font scale 1_3 single footer and reachable`() {
        render(isArabic = false, fontScale = 1.3f)
        assertSingleReachableFooter()
    }
}
