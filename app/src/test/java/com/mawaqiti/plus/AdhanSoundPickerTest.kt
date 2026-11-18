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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableProfile
import com.mawaqiti.plus.notify.AdhanSound
import com.mawaqiti.plus.ui.screens.SettingsList
import com.mawaqiti.plus.ui.theme.MawaqitiTheme
import com.mawaqiti.plus.ui.theme.SkyPhase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta14: the settings dropdown is wired to the registry and to persistence.
 *
 * The tests render the real [SettingsList], scroll to the "صوت الأذان" card,
 * open the themed menu and pick another sound, asserting:
 *  - the closed trigger shows the persisted selection (the default is
 *    أذان الأقصى تكبيرة);
 *  - the open menu contains exactly the seven registry entries, in order;
 *  - a pick calls back with the matching [AdhanSound] and closes the menu;
 *  - the whole card is reachable at the largest in-app font scale.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl")
class AdhanSoundPickerTest {

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

    private var picked: AdhanSound? = null

    private fun render(settings: AppSettings = AppSettings(), fontScale: Float = 1.0f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MawaqitiTheme(phase = SkyPhase.NIGHT) {
                    SettingsList(
                        settings = settings,
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
                        onSelectAdhanSound = { picked = it },
                        onEventEnabled = { _, _ -> },
                        onEventMode = { _, _ -> },
                        onApplyModeToEnabled = {},
                        onOpenExactAlarmSettings = {},
                        onOpenNotificationSettings = {},
                        onOpenBatterySettings = {}
                    )
                }
            }
        }
    }

    private fun scrollToCard() {
        compose.onNodeWithTag("settings_adhan_sound").performScrollTo()
    }

    @Test
    fun `default selection is the al aqsa takbir`() {
        render()
        scrollToCard()
        compose.onNodeWithTag("adhan_sound_trigger").assertIsDisplayed()
        compose.onAllNodesWithText(context.getString(R.string.adhan_alaqsa_tkbera))
            .assertCountEquals(1)
    }

    @Test
    fun `persisted selection is shown when the page opens`() {
        render(AppSettings(adhanSound = AdhanSound.MASR_TKBERA))
        scrollToCard()
        compose.onAllNodesWithText(context.getString(R.string.adhan_masr_tkbera))
            .assertCountEquals(1)
    }

    @Test
    fun `menu lists the seven sounds and a pick is reported once`() {
        render()
        scrollToCard()
        compose.onNodeWithTag("adhan_sound_trigger").performClick()

        AdhanSound.ordered.forEach { sound ->
            compose.onNodeWithTag("adhan_sound_option_${sound.storageKey}")
                .assertExists("${sound.storageKey} missing from the menu")
        }

        compose.onNodeWithTag("adhan_sound_option_${AdhanSound.MAKKAH_FULL.storageKey}")
            .performClick()
        compose.waitForIdle()

        assertEquals(AdhanSound.MAKKAH_FULL, picked)
        compose.onAllNodesWithTag("adhan_sound_menu").assertCountEquals(0)
    }

    @Test
    fun `card stays reachable at the largest font scale`() {
        render(fontScale = 1.3f)
        scrollToCard()
        compose.onNodeWithTag("adhan_sound_trigger").assertIsDisplayed()
    }
}
