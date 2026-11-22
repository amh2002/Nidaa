package com.mawaqiti.plus

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.data.LayoutMode
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableProfile
import com.mawaqiti.plus.ui.screens.SettingsList
import com.mawaqiti.plus.ui.theme.MawaqitiTheme
import com.mawaqiti.plus.ui.theme.SkyPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta21: the display-layout setting is a persisted enum with a mobile default,
 * and the Settings picker reports both choices without clipping at 360 dp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
class Beta21LayoutModeTest {

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

    @Test
    fun `layout mode defaults to mobile and survives the storage round trip`() {
        assertEquals(LayoutMode.MOBILE, AppSettings().layoutMode)
        assertEquals(LayoutMode.MOBILE, LayoutMode.fromKey(null))
        assertEquals(LayoutMode.MOBILE, LayoutMode.fromKey(""))
        assertEquals(LayoutMode.MOBILE, LayoutMode.fromKey("unknown"))
        assertEquals(LayoutMode.MOBILE, LayoutMode.fromKey("mobile"))
        assertEquals(LayoutMode.TV, LayoutMode.fromKey("tv"))
        assertEquals(LayoutMode.TV, LayoutMode.fromKey("TV"))

        // The persisted strings are the contract with DataStore.
        assertEquals("mobile", LayoutMode.MOBILE.storageKey)
        assertEquals("tv", LayoutMode.TV.storageKey)
    }

    @Test
    fun `layout picker shows both options and reports the chosen mode`() {
        var mode by mutableStateOf(LayoutMode.MOBILE)
        val seen = mutableListOf<LayoutMode>()

        compose.setContent {
            MawaqitiTheme(phase = SkyPhase.NIGHT) {
                SettingsList(
                    settings = AppSettings(layoutMode = mode),
                    city = city,
                    day = day,
                    isArabic = true,
                    canScheduleExact = true,
                    notificationsGranted = true,
                    batteryOptimized = false,
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
                    onOpenExactAlarmSettings = {},
                    onOpenNotificationSettings = {},
                    onOpenBatterySettings = {},
                    onSelectLayout = {
                        seen += it
                        mode = it
                    }
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("settings_layout_card").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("layout_option_mobile").assertIsDisplayed()
        compose.onNodeWithTag("layout_option_tv").assertIsDisplayed()

        compose.onNodeWithTag("layout_option_tv").performClick()
        compose.waitForIdle()
        assertEquals(LayoutMode.TV, mode)

        compose.onNodeWithTag("layout_option_mobile").performClick()
        compose.waitForIdle()
        assertEquals(LayoutMode.MOBILE, mode)
        assertEquals(listOf(LayoutMode.TV, LayoutMode.MOBILE), seen)
    }

    @Test
    fun `layout strings are present and distinct`() {
        listOf(
            R.string.settings_layout_title,
            R.string.settings_layout_desc,
            R.string.layout_mobile,
            R.string.layout_tv,
            R.string.tv_today
        ).forEach { id ->
            assertTrue(context.getString(id).isNotBlank())
        }
        assertNotEquals(
            context.getString(R.string.layout_mobile),
            context.getString(R.string.layout_tv)
        )
    }

    @Test
    fun `tv metrics stay ordered so the compact panel never exceeds the full one`() {
        val full = com.mawaqiti.plus.ui.theme.TvDimens
        val compact = com.mawaqiti.plus.ui.theme.TvCompactDimens

        assertTrue(compact.screenPaddingH <= full.screenPaddingH)
        assertTrue(compact.heroTimeSize.value <= full.heroTimeSize.value)
        assertTrue(compact.prayerTimeSize.value <= full.prayerTimeSize.value)
        assertTrue(compact.icon <= full.icon)
    }
}
