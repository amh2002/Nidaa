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
import com.mawaqiti.plus.data.PreAlertMinutes
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableProfile
import com.mawaqiti.plus.notify.DayTimes
import com.mawaqiti.plus.notify.NotifyEvent
import com.mawaqiti.plus.notify.PreAlertPlanner
import com.mawaqiti.plus.notify.SchedulePlanner
import com.mawaqiti.plus.ui.screens.SettingsList
import com.mawaqiti.plus.ui.theme.MawaqitiTheme
import com.mawaqiti.plus.ui.theme.SkyPhase
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta22: the pre-prayer alert is a persisted 0/5/10/15/30 choice whose
 * reminders are derived from the same prayer plan as the adhan alarms, and the
 * Settings card reports the current value without clipping at 360 dp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w360dp-h640dp-xhdpi")
class Beta22PreAlertTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val zone: ZoneId = ZoneId.of("Asia/Hebron")

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

    private fun times() = DayTimes(
        fajr = 300,
        sunrise = 360,
        dhuhr = 720,
        asr = 930,
        maghrib = 1140,
        isha = 1200,
        lastThird = 60
    )

    private fun now(hour: Int, minute: Int = 0): ZonedDateTime =
        ZonedDateTime.of(2026, 6, 1, hour, minute, 0, 0, zone)

    @Test
    fun `pre-alert defaults to disabled and only accepts the five options`() {
        assertEquals(PreAlertMinutes.DISABLED, AppSettings().preAlertMinutes)
        assertEquals(listOf(0, 5, 10, 15, 30), PreAlertMinutes.OPTIONS)
        for (minutes in PreAlertMinutes.OPTIONS) {
            assertEquals(minutes, PreAlertMinutes.normalize(minutes))
        }
        // Anything outside the picker falls back to «معطّل».
        assertEquals(PreAlertMinutes.DISABLED, PreAlertMinutes.normalize(-5))
        assertEquals(PreAlertMinutes.DISABLED, PreAlertMinutes.normalize(7))
        assertEquals(PreAlertMinutes.DISABLED, PreAlertMinutes.normalize(60))
    }

    @Test
    fun `plans exactly one reminder per prayer at adhan minus N minutes`() {
        val alarms = SchedulePlanner.plan(now(10), { _ -> times() })
        val alerts = PreAlertPlanner.plan(alarms, now(10), minutesBefore = 10)

        assertEquals(5, alerts.size)
        assertEquals(
            setOf(
                NotifyEvent.FAJR,
                NotifyEvent.DHUHR,
                NotifyEvent.ASR,
                NotifyEvent.MAGHRIB,
                NotifyEvent.ISHA
            ),
            alerts.map { it.event }.toSet()
        )
        // Sunrise is never announced ahead of time.
        assertTrue(alerts.none { it.event == NotifyEvent.SUNRISE })

        for (alert in alerts) {
            val adhan = alarms.first { it.event == alert.event }.fireAt
            assertEquals(adhan.minusMinutes(10), alert.fireAt)
            assertTrue(alert.fireAt.isAfter(now(10)))
        }
        assertEquals(alerts.sortedBy { it.fireAt.toInstant() }, alerts)
    }

    @Test
    fun `disabled plans nothing and reminders already in the past are skipped`() {
        // A three-hour horizon keeps only today's Dhuhr (12:00) in the plan.
        val alarms = SchedulePlanner.plan(now(11, 45), { _ -> times() }, horizonHours = 3)
        assertEquals(listOf(NotifyEvent.DHUHR), alarms.map { it.event })

        assertTrue(PreAlertPlanner.plan(alarms, now(11, 45), 0).isEmpty())

        // Dhuhr is at 12:00. Ten minutes before it is still ahead…
        val ten = PreAlertPlanner.plan(alarms, now(11, 45), 10)
        assertEquals(listOf(NotifyEvent.DHUHR), ten.map { it.event })
        assertEquals(now(11, 50), ten.single().fireAt)

        // …but thirty minutes before it has already passed, so nothing is planned.
        assertTrue(PreAlertPlanner.plan(alarms, now(11, 45), 30).isEmpty())

        // With the full 48-hour plan the same 30-minute lead simply moves to the
        // next occurrence instead of firing late.
        val full = SchedulePlanner.plan(now(11, 45), { _ -> times() })
        val tomorrow = PreAlertPlanner.plan(full, now(11, 45), 30)
            .first { it.event == NotifyEvent.DHUHR }
        assertEquals(now(11, 45).toLocalDate().plusDays(1), tomorrow.fireAt.toLocalDate())
        assertEquals(11, tomorrow.fireAt.hour)
        assertEquals(30, tomorrow.fireAt.minute)
    }

    @Test
    fun `pre-alert picker shows every option and reports the chosen value`() {
        var minutes by mutableStateOf(PreAlertMinutes.DISABLED)
        val seen = mutableListOf<Int>()

        compose.setContent {
            MawaqitiTheme(phase = SkyPhase.NIGHT) {
                SettingsList(
                    settings = AppSettings(preAlertMinutes = minutes),
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
                    onOpenExactAlarmSettings = {},
                    onOpenNotificationSettings = {},
                    onOpenBatterySettings = {},
                    onSelectPreAlert = {
                        seen += it
                        minutes = it
                    }
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("settings_pre_alert_card").performScrollTo().assertIsDisplayed()
        for (option in PreAlertMinutes.OPTIONS) {
            compose.onNodeWithTag("pre_alert_option_$option").assertIsDisplayed()
        }

        compose.onNodeWithTag("pre_alert_option_15").performClick()
        compose.waitForIdle()
        assertEquals(15, minutes)

        compose.onNodeWithTag("pre_alert_option_0").performClick()
        compose.waitForIdle()
        assertEquals(PreAlertMinutes.DISABLED, minutes)
        assertEquals(listOf(15, 0), seen)
    }

    @Test
    fun `pre-alert strings are present and distinct`() {
        listOf(
            R.string.settings_pre_alert_title,
            R.string.settings_pre_alert_desc,
            R.string.pre_alert_off,
            R.string.pre_alert_5,
            R.string.pre_alert_10,
            R.string.pre_alert_15,
            R.string.pre_alert_30,
            R.string.pre_alert_title,
            R.string.notify_channel_pre_alert
        ).forEach { id ->
            assertTrue(context.getString(id).isNotBlank())
        }
        assertNotEquals(
            context.getString(R.string.pre_alert_5),
            context.getString(R.string.pre_alert_10)
        )
        // The body carries both the lead time and the prayer name. The Arabic
        // locale renders the number with Arabic-Indic digits, so the reference
        // string is formatted with the same locale.
        val fajr = context.getString(R.string.prayer_fajr)
        assertEquals(
            String.format(
                java.util.Locale.forLanguageTag("ar"),
                "تبقى %d دقائق على أذان %s",
                10,
                fajr
            ),
            context.getString(R.string.pre_alert_body, 10, fajr)
        )
    }
}
