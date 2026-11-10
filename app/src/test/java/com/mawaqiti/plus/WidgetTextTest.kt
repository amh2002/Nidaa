package com.mawaqiti.plus

import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.notify.WidgetText
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Widget formatting matrix: both locales, both clock formats, next-prayer logic. */
class WidgetTextTest {

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
    fun `24 hour widget times use latin digits`() {
        assertEquals("05:00", WidgetText.clock(300, ClockFormat.H24, isArabic = true))
        assertEquals("05:00", WidgetText.clock(300, ClockFormat.H24, isArabic = false))
        assertEquals("20:00", WidgetText.clock(1200, ClockFormat.H24, isArabic = false))
    }

    @Test
    fun `12 hour widget times render locale suffixes`() {
        assertEquals("5:00 a.m.", WidgetText.clock(300, ClockFormat.H12, isArabic = false))
        assertEquals("8:00 p.m.", WidgetText.clock(1200, ClockFormat.H12, isArabic = false))

        val arabic = WidgetText.clock(300, ClockFormat.H12, isArabic = true)
        assertTrue(arabic.startsWith("5:00"))
        assertTrue(arabic != WidgetText.clock(300, ClockFormat.H12, isArabic = false))
    }

    @Test
    fun `next prayer skips the times already passed`() {
        assertEquals(TableEvent.FAJR to 300, WidgetText.nextPrayer(day, nowMinutes = 0))
        assertEquals(TableEvent.SUNRISE to 360, WidgetText.nextPrayer(day, nowMinutes = 301))
        assertEquals(TableEvent.DHUHR to 720, WidgetText.nextPrayer(day, nowMinutes = 700))
        assertEquals(TableEvent.ISHA to 1200, WidgetText.nextPrayer(day, nowMinutes = 1150))
    }

    @Test
    fun `after isha the next prayer is tomorrow fajr`() {
        assertEquals(TableEvent.FAJR to 301, WidgetText.nextPrayer(day, nowMinutes = 1201))
        assertEquals(TableEvent.FAJR to 301, WidgetText.nextPrayer(day, nowMinutes = 1439))
    }

    @Test
    fun `gregorian and hijri lines exist in both locales`() {
        val date = LocalDate.of(2026, 11, 10)
        val ar = WidgetText.gregorian(date, isArabic = true)
        val en = WidgetText.gregorian(date, isArabic = false)

        assertTrue(ar.contains("10"))
        assertTrue(en.contains("10"))
        assertTrue(en.contains("Nov"))
        assertTrue(WidgetText.hijri(date, isArabic = false).contains("AH"))
        assertTrue(WidgetText.hijri(date, isArabic = true).isNotBlank())
    }

    @Test
    fun `the widget exposes all six table events`() {
        assertEquals(6, WidgetText.allEvents().size)
        assertEquals(TableEvent.FAJR, WidgetText.allEvents().first())
        assertEquals(TableEvent.ISHA, WidgetText.allEvents().last())
    }
}
