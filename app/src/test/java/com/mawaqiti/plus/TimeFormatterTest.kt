package com.mawaqiti.plus

import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Display-only formatter tests. All engine values stay in minutes since
 * midnight; only the rendered string changes.
 */
class TimeFormatterTest {

    @Test
    fun twentyFourHourBoundaries() {
        val expected = mapOf(
            0 to "00:00",
            59 to "00:59",
            720 to "12:00",
            779 to "12:59",
            1439 to "23:59"
        )
        for ((minutes, text) in expected) {
            assertEquals(text, TimeFormatter.format(minutes, ClockFormat.H24, isArabic = true))
            assertEquals(text, TimeFormatter.format(minutes, ClockFormat.H24, isArabic = false))
        }
    }

    @Test
    fun twelveHourBoundariesArabic() {
        val expected = mapOf(
            0 to "12:00 ص",
            59 to "12:59 ص",
            720 to "12:00 م",
            779 to "12:59 م",
            1439 to "11:59 م"
        )
        for ((minutes, text) in expected) {
            assertEquals(text, TimeFormatter.format(minutes, ClockFormat.H12, isArabic = true))
        }
    }

    @Test
    fun twelveHourBoundariesEnglish() {
        val expected = mapOf(
            0 to "12:00 a.m.",
            59 to "12:59 a.m.",
            720 to "12:00 p.m.",
            779 to "12:59 p.m.",
            1439 to "11:59 p.m."
        )
        for ((minutes, text) in expected) {
            assertEquals(text, TimeFormatter.format(minutes, ClockFormat.H12, isArabic = false))
        }
    }

    @Test
    fun referenceExamplesFromSpec() {
        assertEquals("5:09 ص", TimeFormatter.format(5 * 60 + 9, ClockFormat.H12, true))
        assertEquals("6:21 م", TimeFormatter.format(18 * 60 + 21, ClockFormat.H12, true))
        assertEquals("5:09 a.m.", TimeFormatter.format(5 * 60 + 9, ClockFormat.H12, false))
        assertEquals("6:21 p.m.", TimeFormatter.format(18 * 60 + 21, ClockFormat.H12, false))
    }

    @Test
    fun digitsAreAlwaysLatin() {
        val arabicIndic = '\u0660'..'\u0669'
        val extended = '\u06F0'..'\u06F9'
        for (clock in ClockFormat.entries) {
            for (minute in listOf(0, 309, 720, 1101, 1439)) {
                for (arabic in listOf(true, false)) {
                    val text = TimeFormatter.format(minute, clock, arabic)
                    assertFalse("Arabic-Indic digits in $text", text.any { it in arabicIndic })
                    assertFalse("Extended Arabic-Indic digits in $text", text.any { it in extended })
                }
            }
        }
    }

    @Test
    fun minutesWrapAcrossMidnight() {
        assertEquals("00:00", TimeFormatter.format(1440, ClockFormat.H24, true))
        assertEquals("12:00 ص", TimeFormatter.format(-1440, ClockFormat.H12, true))
    }

    @Test
    fun countdownIsElapsedStyleAndClamped() {
        assertEquals("00:00:00", TimeFormatter.formatCountdown(0))
        assertEquals("01:01:01", TimeFormatter.formatCountdown(3661))
        assertEquals("02:00:00", TimeFormatter.formatCountdown(7200))
        assertEquals("00:00:00", TimeFormatter.formatCountdown(-5))
        assertTrue(TimeFormatter.formatCountdown(3661).contains(":"))
    }
}
