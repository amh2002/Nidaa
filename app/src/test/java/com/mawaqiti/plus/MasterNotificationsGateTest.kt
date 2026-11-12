package com.mawaqiti.plus

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableProfile
import com.mawaqiti.plus.notify.NotificationSettings
import com.mawaqiti.plus.notify.NotifyEvent
import com.mawaqiti.plus.notify.NotifyMode
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
 * Beta 7, item 3: the master notifications switch locks every per-prayer
 * control while it is OFF.
 *
 * Checks disabled semantics, inert clicks, exact caption text in both
 * languages at font scales 1.0 and 1.3, a single caption node, and that
 * OFF -> ON restores the exact per-prayer modes (OFF never resets them).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl")
class MasterNotificationsGateTest {

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

    private class Holder(initial: AppSettings) {
        var value by mutableStateOf(initial)
    }

    private var modeChanges = 0

    private fun render(holder: Holder, isArabic: Boolean, fontScale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MawaqitiTheme(phase = SkyPhase.DAY) {
                    SettingsList(
                        settings = holder.value,
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
                        onNotificationsEnabled = { enabled ->
                            holder.value = holder.value.copy(
                                notifications = holder.value.notifications.copy(
                                    masterEnabled = enabled
                                )
                            )
                        },
                        onEventEnabled = { event, enabled ->
                            holder.value = holder.value.copy(
                                notifications = holder.value.notifications.withEvent(
                                    event,
                                    holder.value.notifications.forEvent(event).copy(
                                        enabled = enabled
                                    )
                                )
                            )
                        },
                        onEventMode = { event, mode ->
                            modeChanges++
                            holder.value = holder.value.copy(
                                notifications = holder.value.notifications.withEvent(
                                    event,
                                    holder.value.notifications.forEvent(event).copy(
                                        mode = mode
                                    )
                                )
                            )
                        },
                        onApplyModeToEnabled = {},
                        onTestEvent = { _, _ -> },
                        onTestMode = {},
                        onOpenExactAlarmSettings = {},
                        onOpenNotificationSettings = {},
                        onOpenBatterySettings = {}
                    )
                }
            }
        }
    }

    private fun NotificationSettings.withEvent(
        event: NotifyEvent,
        value: com.mawaqiti.plus.notify.EventNotification
    ): NotificationSettings = copy(events = events + (event to value))

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private val arabicCaption = "التنبيهات مطفأة — فعّلها لتعديل أوضاع الصلوات"
    private val englishCaption =
        "Notifications are off — enable them to change prayer alert modes."

    @Test
    fun `arabic 1_0 master off disables every prayer control and shows one caption`() {
        assertEquals(arabicCaption, string(R.string.notifications_off_caption))
        val holder = Holder(
            AppSettings(
                notifications = NotificationSettings(masterEnabled = false)
            )
        )
        render(holder, isArabic = true, fontScale = 1.0f)

        compose.onAllNodesWithText(arabicCaption, useUnmergedTree = true)
            .assertCountEquals(1)
        compose.onAllNodesWithTag("notifications_off_caption")
            .assertCountEquals(1)

        compose.onNodeWithTag("notifications_master_switch")
            .performScrollTo()
            .assertIsEnabled()

        compose.onNodeWithTag("event_card_fajr")
            .performScrollTo()
            .assertIsNotEnabled()
        compose.onNodeWithTag("event_toggle_fajr").assertIsNotEnabled()
        compose.onNodeWithTag("event_modes_fajr").assertIsNotEnabled()
        compose.onNodeWithTag("event_mode_fajr_vibrate").assertIsNotEnabled()

        // Physical taps on disabled controls must not call back.
        compose.onNodeWithTag("event_mode_fajr_vibrate")
            .performTouchInput { click() }
        compose.onNodeWithTag("event_toggle_fajr")
            .performTouchInput { click() }
        compose.waitForIdle()

        assertEquals(0, modeChanges)
        assertTrue(holder.value.notifications.forEvent(NotifyEvent.FAJR).enabled)
        assertEquals(
            NotifyMode.ADHAN,
            holder.value.notifications.forEvent(NotifyEvent.FAJR).mode
        )
    }

    @Test
    fun `arabic 1_3 master off then on restores the exact per prayer mode`() {
        val holder = Holder(AppSettings())
        render(holder, isArabic = true, fontScale = 1.3f)

        compose.onNodeWithTag("event_mode_fajr_vibrate")
            .performScrollTo()
            .performClick()
        compose.waitForIdle()
        assertEquals(
            NotifyMode.VIBRATE,
            holder.value.notifications.forEvent(NotifyEvent.FAJR).mode
        )
        assertEquals(1, modeChanges)

        compose.onNodeWithTag("notifications_master_switch")
            .performScrollTo()
            .performClick()
        compose.waitForIdle()

        assertFalse(holder.value.notifications.masterEnabled)
        compose.onAllNodesWithTag("notifications_off_caption").assertCountEquals(1)
        compose.onNodeWithTag("event_card_fajr").assertIsNotEnabled()
        compose.onNodeWithTag("event_mode_fajr_vibrate").assertIsNotEnabled()
        // Turning the master OFF must not reset the stored per-prayer mode.
        assertEquals(
            NotifyMode.VIBRATE,
            holder.value.notifications.forEvent(NotifyEvent.FAJR).mode
        )

        compose.onNodeWithTag("notifications_master_switch").performClick()
        compose.waitForIdle()

        assertTrue(holder.value.notifications.masterEnabled)
        compose.onAllNodesWithTag("notifications_off_caption").assertCountEquals(0)
        compose.onNodeWithTag("event_card_fajr").assertIsEnabled()
        compose.onNodeWithTag("event_mode_fajr_vibrate").assertIsEnabled()
        assertEquals(
            NotifyMode.VIBRATE,
            holder.value.notifications.forEvent(NotifyEvent.FAJR).mode
        )
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `english 1_0 master off shows exactly the English caption`() {
        assertEquals(englishCaption, string(R.string.notifications_off_caption))
        val holder = Holder(
            AppSettings(
                language = com.mawaqiti.plus.data.AppLanguage.ENGLISH,
                notifications = NotificationSettings(masterEnabled = false)
            )
        )
        render(holder, isArabic = false, fontScale = 1.0f)

        compose.onAllNodesWithText(englishCaption, useUnmergedTree = true)
            .assertCountEquals(1)
        compose.onAllNodesWithTag("notifications_off_caption")
            .assertCountEquals(1)
        compose.onNodeWithTag("event_card_fajr")
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `english 1_3 master on is editable and shows no caption`() {
        val holder = Holder(
            AppSettings(language = com.mawaqiti.plus.data.AppLanguage.ENGLISH)
        )
        render(holder, isArabic = false, fontScale = 1.3f)

        compose.onAllNodesWithTag("notifications_off_caption").assertCountEquals(0)
        compose.onNodeWithTag("event_card_fajr")
            .performScrollTo()
            .assertIsEnabled()

        compose.onNodeWithTag("event_mode_fajr_vibrate").performClick()
        compose.waitForIdle()
        assertEquals(
            NotifyMode.VIBRATE,
            holder.value.notifications.forEvent(NotifyEvent.FAJR).mode
        )
    }
}
