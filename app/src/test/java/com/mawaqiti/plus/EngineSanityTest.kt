package com.mawaqiti.plus

import com.mawaqiti.plus.engine.AsrMadhab
import com.mawaqiti.plus.engine.CalculationMethod
import com.mawaqiti.plus.engine.CalculationSettings
import com.mawaqiti.plus.engine.HighLatitudeRule
import com.mawaqiti.plus.engine.PrayerEngine
import com.mawaqiti.plus.engine.Qibla
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract tests for the parts the cross-check does not cover. */
class EngineSanityTest {

    private val mwl = CalculationSettings(
        method = CalculationMethod.MWL,
        asrMadhab = AsrMadhab.SHAFI,
        highLatitudeRule = HighLatitudeRule.NIGHT_MIDDLE
    )

    @Test
    fun everyTimeIsRoundedUpToAWholeMinute() {
        val day = PrayerEngine.calculate(
            date = LocalDate.of(2026, 6, 21),
            latitude = 21.4225,
            longitude = 39.8262,
            zone = ZoneOffset.UTC,
            settings = mwl
        )
        listOf(day.fajr, day.sunrise, day.dhuhr, day.asr, day.maghrib, day.isha).forEach { moment ->
            assertEquals(0, moment.dateTime.second)
            assertEquals(0, moment.dateTime.nano)
        }
    }

    @Test
    fun ummAlQuraIshaIsMaghribPlus90Minutes() {
        val day = PrayerEngine.calculate(
            date = LocalDate.of(2026, 3, 1),
            latitude = 21.4225,
            longitude = 39.8262,
            zone = ZoneOffset.UTC,
            settings = mwl.copy(method = CalculationMethod.UMM_AL_QURA)
        )
        val gap = java.time.Duration.between(day.maghrib.dateTime, day.isha.dateTime).toMinutes()
        assertEquals(90L, gap)
    }

    @Test
    fun imsakIsTenMinutesBeforeFajr() {
        val day = PrayerEngine.calculate(
            date = LocalDate.of(2026, 3, 1),
            latitude = 24.7136,
            longitude = 46.6753,
            zone = ZoneOffset.UTC,
            settings = mwl
        )
        val gap = java.time.Duration.between(day.imsak.dateTime, day.fajr.dateTime).toMinutes()
        assertEquals(10L, gap)
    }

    @Test
    fun lastThirdMatchesTheOriginalConvention() {
        val day = PrayerEngine.calculate(
            date = LocalDate.of(2026, 11, 6),
            latitude = 21.4225,
            longitude = 39.8262,
            zone = ZoneOffset.ofHours(3),
            settings = mwl
        )
        val night = java.time.Duration.between(day.isha.dateTime, day.tomorrowFajr.dateTime).toMinutes()
        val expected = day.tomorrowFajr.dateTime.minusMinutes(night / 3)
        assertEquals(expected, day.lastThird.dateTime)
        assertTrue("last third must fall before Fajr", day.lastThird.dateTime.isBefore(day.tomorrowFajr.dateTime))
        assertTrue("last third must fall after Isha", day.lastThird.dateTime.isAfter(day.isha.dateTime))
    }

    @Test
    fun hanafiAsrIsLaterThanShafii() {
        val shafii = PrayerEngine.calculate(
            LocalDate.of(2026, 5, 1), 30.0444, 31.2357, ZoneOffset.UTC, mwl
        )
        val hanafi = PrayerEngine.calculate(
            LocalDate.of(2026, 5, 1), 30.0444, 31.2357, ZoneOffset.UTC,
            mwl.copy(asrMadhab = AsrMadhab.HANAFI)
        )
        assertTrue(hanafi.asr.instantMillis > shafii.asr.instantMillis)
    }

    @Test
    fun qiblaBearingsMatchIndependentGreatCircleValues() {
        data class Case(val name: String, val lat: Double, val lon: Double, val expected: Double)
        val cases = listOf(
            Case("Cairo", 30.0444, 31.2357, 136.14),
            Case("London", 51.5074, -0.1278, 118.99),
            Case("New York", 40.7128, -74.0060, 58.48),
            Case("Jakarta", -6.2088, 106.8456, 295.15)
        )
        for (case in cases) {
            val actual = Qibla.bearing(case.lat, case.lon)
            assertTrue(
                "${case.name}: expected ${case.expected}, got $actual",
                abs(actual - case.expected) < 0.5
            )
        }
        val madinah = Qibla.distanceKm(24.4672, 39.6111)
        assertTrue("Madinah-Kaaba distance was $madinah", abs(madinah - 339.27) < 5.0)
    }

    @Test
    fun deviceTimeZoneAppliesDaylightSavingAutomatically() {
        // London is UTC+1 on 2026-07-15 and UTC+0 on 2026-01-15.
        val summer = PrayerEngine.calculate(
            LocalDate.of(2026, 7, 15), 51.5074, -0.1278,
            ZoneId.of("Europe/London"), mwl
        )
        val winter = PrayerEngine.calculate(
            LocalDate.of(2026, 1, 15), 51.5074, -0.1278,
            ZoneId.of("Europe/London"), mwl
        )
        assertEquals(1, summer.dhuhr.dateTime.offset.totalSeconds / 3600)
        assertEquals(0, winter.dhuhr.dateTime.offset.totalSeconds / 3600)
    }
}
