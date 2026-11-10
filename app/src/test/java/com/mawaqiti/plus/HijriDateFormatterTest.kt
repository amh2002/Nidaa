package com.mawaqiti.plus

import com.mawaqiti.plus.format.HijriDateFormatter
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hijri rendering tests.
 *
 * Expectations are derived from the chrono library itself wherever a
 * Gregorian↔Hijri pair is needed, so no invented pair can make a broken
 * conversion look correct. The only hard-coded lists are the twelve month
 * names, which are part of the user-facing specification.
 */
class HijriDateFormatterTest {

    private val arabicNames = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    private val englishNames = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula",
        "Jumada al-Akhirah", "Rajab", "Sha'ban", "Ramadan", "Shawwal",
        "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    @Test
    fun fixedHijrahDateRendersInBothLocales() {
        val hijri = HijrahDate.of(1448, 6, 19)
        val gregorian = LocalDate.from(hijri)

        assertEquals("19 جمادى الآخرة 1448هـ", HijriDateFormatter.format(gregorian, true))
        assertEquals("19 Jumada al-Akhirah 1448 AH", HijriDateFormatter.format(gregorian, false))
    }

    @Test
    fun everyMonthFirstAndLastDayRoundTripsThroughGregorian() {
        for (month in 1..12) {
            val first = HijrahDate.of(1448, month, 1)
            val length = first.lengthOfMonth()
            assertTrue("month $month length $length", length == 29 || length == 30)
            val last = HijrahDate.of(1448, month, length)

            for (hijri in listOf(first, last)) {
                val gregorian = LocalDate.from(hijri)
                val parts = HijriDateFormatter.toHijri(gregorian)
                assertEquals(hijri.get(ChronoField.YEAR), parts.year)
                assertEquals(month, parts.month)
                assertEquals(hijri.get(ChronoField.DAY_OF_MONTH), parts.day)
                assertEquals(hijri, HijrahDate.from(gregorian))
            }
        }
    }

    @Test
    fun twelveMonthNamesInBothLanguages() {
        assertEquals(arabicNames, HijriDateFormatter.monthNames(isArabic = true))
        assertEquals(englishNames, HijriDateFormatter.monthNames(isArabic = false))
        for (month in 1..12) {
            assertEquals(arabicNames[month - 1], HijriDateFormatter.monthName(month, true))
            assertEquals(englishNames[month - 1], HijriDateFormatter.monthName(month, false))
        }
    }

    @Test
    fun everyMonthNameAppearsWithCorrectSuffix() {
        for (month in 1..12) {
            val gregorian = LocalDate.from(HijrahDate.of(1448, month, 1))
            val arabic = HijriDateFormatter.format(gregorian, true)
            val english = HijriDateFormatter.format(gregorian, false)

            assertTrue(arabic, arabic.startsWith("1 ${arabicNames[month - 1]} 1448هـ"))
            assertTrue(english, english.startsWith("1 ${englishNames[month - 1]} 1448 AH"))
        }
    }

    @Test
    fun digitsAreAlwaysLatin() {
        val arabicIndic = '\u0660'..'\u0669'
        val extended = '\u06F0'..'\u06F9'
        for (month in 1..12) {
            val first = HijrahDate.of(1448, month, 1)
            val days = listOf(1, first.lengthOfMonth())
            for (day in days) {
                val gregorian = LocalDate.from(HijrahDate.of(1448, month, day))
                for (isArabic in listOf(true, false)) {
                    val text = HijriDateFormatter.format(gregorian, isArabic)
                    assertFalse("Arabic-Indic digits in $text", text.any { it in arabicIndic })
                    assertFalse("Extended Arabic-Indic digits in $text", text.any { it in extended })
                }
            }
        }
    }

    @Test
    fun localMidnightAdvancesToTheNextHijriDay() {
        val zone = ZoneId.of("Asia/Hebron")
        val beforeMidnight = ZonedDateTime.of(2026, 11, 9, 23, 59, 59, 0, zone)
        val afterMidnight = ZonedDateTime.of(2026, 11, 10, 0, 0, 1, 0, zone)

        assertEquals(LocalDate.of(2026, 11, 9), beforeMidnight.toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 10), afterMidnight.toLocalDate())

        val before = HijriDateFormatter.toHijri(beforeMidnight.toLocalDate())
        val after = HijriDateFormatter.toHijri(afterMidnight.toLocalDate())
        val beforeText = HijriDateFormatter.format(beforeMidnight.toLocalDate(), true)
        val afterText = HijriDateFormatter.format(afterMidnight.toLocalDate(), true)

        assertNotEquals(beforeText, afterText)
        assertEquals(
            1L,
            chronoDay(afterMidnight.toLocalDate()) - chronoDay(beforeMidnight.toLocalDate())
        )
    }

    @Test
    fun monthArgumentIsValidated() {
        for (bad in listOf(0, 13, -1)) {
            try {
                HijriDateFormatter.monthName(bad, true)
                throw AssertionError("month $bad should be rejected")
            } catch (expected: IllegalArgumentException) {
                // Expected.
            }
        }
    }

    private fun chronoDay(date: LocalDate): Long =
        HijrahDate.from(date).toEpochDay()
}
