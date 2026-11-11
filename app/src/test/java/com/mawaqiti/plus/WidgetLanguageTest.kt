package com.mawaqiti.plus

import android.content.Context
import android.view.View
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.notify.NotificationText
import com.mawaqiti.plus.notify.WidgetText
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.notify.WidgetUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta5 W3 guard: the widget language is a separate choice that defaults to the
 * app language, can be switched on its own, and selects a fully localized
 * layout. It must never leak into the app UI language.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl")
class WidgetLanguageTest {

    private val context: Context
        get() = androidx.test.core.app.ApplicationProvider.getApplicationContext()

    private fun localized(language: AppLanguage): Context =
        NotificationText.localizedContext(context, language)

    @Test
    fun `unset widget language follows the app language`() {
        assertEquals(
            AppLanguage.ARABIC,
            AppSettings(language = AppLanguage.ARABIC).effectiveWidgetLanguage
        )
        assertEquals(
            AppLanguage.ENGLISH,
            AppSettings(language = AppLanguage.ENGLISH).effectiveWidgetLanguage
        )
        assertNull(AppSettings().widgetLanguage)
    }

    @Test
    fun `widget override wins over the app language in both directions`() {
        // English app, Arabic widget.
        val englishApp = AppSettings(language = AppLanguage.ENGLISH)
        assertEquals(
            AppLanguage.ARABIC,
            englishApp.copy(widgetLanguage = AppLanguage.ARABIC).effectiveWidgetLanguage
        )
        // Arabic app, English widget.
        val arabicApp = AppSettings(language = AppLanguage.ARABIC)
        assertEquals(
            AppLanguage.ENGLISH,
            arabicApp.copy(widgetLanguage = AppLanguage.ENGLISH).effectiveWidgetLanguage
        )
        // The UI language itself is untouched by the override.
        assertEquals(
            AppLanguage.ARABIC,
            arabicApp.copy(widgetLanguage = AppLanguage.ENGLISH).language
        )
    }

    @Test
    fun `each widget language has its own mirrored layout`() {
        val arabic = WidgetUpdater.layoutFor(AppLanguage.ARABIC)
        val english = WidgetUpdater.layoutFor(AppLanguage.ENGLISH)
        assertEquals(R.layout.widget_mawaqiti_ar, arabic)
        assertEquals(R.layout.widget_mawaqiti_en, english)
        assertNotEquals(arabic, english)
    }

    @Test
    fun `widget strings resolve from the chosen language not the system locale`() {
        val arabic = localized(AppLanguage.ARABIC)
        val english = localized(AppLanguage.ENGLISH)

        assertEquals("الفجر", arabic.getString(R.string.prayer_fajr))
        assertEquals("Fajr", english.getString(R.string.prayer_fajr))
        assertEquals("العشاء", arabic.getString(R.string.prayer_isha))
        assertEquals("Isha", english.getString(R.string.prayer_isha))
        assertEquals("القادمة", arabic.getString(R.string.widget_next))
        assertEquals("Next", english.getString(R.string.widget_next))

        // The launcher label does not follow the widget language.
        assertEquals("نداء - Nidaa", arabic.getString(R.string.app_name))
        assertEquals("نداء - Nidaa", english.getString(R.string.app_name))
        // The wordmark is always Arabic.
        assertEquals("نداء", arabic.getString(R.string.brand_name))
        assertEquals("نداء", english.getString(R.string.brand_name))
    }

    @Test
    fun `widget language also drives layout direction of its own context`() {
        assertEquals(
            View.LAYOUT_DIRECTION_RTL,
            localized(AppLanguage.ARABIC).resources.configuration.layoutDirection
        )
        assertEquals(
            View.LAYOUT_DIRECTION_LTR,
            localized(AppLanguage.ENGLISH).resources.configuration.layoutDirection
        )
    }

    @Test
    fun `times stay latin digits and honour 12h and 24h in both languages`() {
        val day = TableDay(
            fajr = 300, sunrise = 360, dhuhr = 720, asr = 930,
            maghrib = 1140, isha = 1200, lastThird = 60,
            fajrTomorrow = 301, ishaYesterday = 1201, row = 0
        )

        assertEquals("05:00", WidgetText.clock(day.minutesFor(TableEvent.FAJR), ClockFormat.H24, true))
        assertEquals("05:00", WidgetText.clock(day.minutesFor(TableEvent.FAJR), ClockFormat.H24, false))

        val h12Arabic = WidgetText.clock(day.minutesFor(TableEvent.FAJR), ClockFormat.H12, true)
        val h12English = WidgetText.clock(day.minutesFor(TableEvent.FAJR), ClockFormat.H12, false)
        assertTrue(h12Arabic.startsWith("5:00"))
        assertTrue(h12English.startsWith("5:00"))
        assertNotEquals("the 12h suffix must follow the widget language", h12Arabic, h12English)

        // The next-prayer label flips language but never the clock preference.
        assertEquals(TableEvent.SUNRISE, WidgetText.nextPrayer(day, 301).first)
        assertEquals(TableEvent.FAJR, WidgetText.nextPrayer(day, 1201).first)
    }
}
