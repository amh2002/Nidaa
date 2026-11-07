package com.mawaqiti.plus.engine

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/** Supported calculation methods. */
enum class CalculationMethod {
    MWL,
    ISNA,
    EGYPT,
    UMM_AL_QURA,
    KARACHI,
    CUSTOM
}

/** Asr shadow length: Shafi'i = 1, Hanafi = 2. */
enum class AsrMadhab(val shadowFactor: Double) {
    SHAFI(1.0),
    HANAFI(2.0)
}

/** How Fajr/Isha are bounded when the sun never reaches the required angle. */
enum class HighLatitudeRule {
    NIGHT_MIDDLE,
    ANGLE_BASED
}

/**
 * Immutable calculation settings. Pure Kotlin so the engine can run in a JVM
 * unit test without Android classes.
 */
data class CalculationSettings(
    val method: CalculationMethod = CalculationMethod.MWL,
    val asrMadhab: AsrMadhab = AsrMadhab.SHAFI,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.NIGHT_MIDDLE,
    val customFajrAngle: Double = 18.0,
    val customIshaAngle: Double = 17.0,
    val imsakOffsetMinutes: Int = 10
) {
    val fajrAngle: Double
        get() = when (method) {
            CalculationMethod.MWL -> 18.0
            CalculationMethod.ISNA -> 15.0
            CalculationMethod.EGYPT -> 19.5
            CalculationMethod.UMM_AL_QURA -> 18.5
            CalculationMethod.KARACHI -> 18.0
            CalculationMethod.CUSTOM -> customFajrAngle
        }

    val ishaAngle: Double
        get() = when (method) {
            CalculationMethod.MWL -> 17.0
            CalculationMethod.ISNA -> 15.0
            CalculationMethod.EGYPT -> 17.5
            CalculationMethod.UMM_AL_QURA -> 0.0
            CalculationMethod.KARACHI -> 18.0
            CalculationMethod.CUSTOM -> customIshaAngle
        }

    /** Umm al-Qura uses Maghrib + 90 minutes instead of an Isha angle. */
    val ishaIntervalMinutes: Int
        get() = if (method == CalculationMethod.UMM_AL_QURA) 90 else 0
}

/** A rounded prayer time, kept as a real instant so DST is handled by java.time. */
data class PrayerMoment(val dateTime: ZonedDateTime) {
    val localTime: LocalTime get() = dateTime.toLocalTime()

    val minuteOfDay: Int
        get() = dateTime.hour * 60 + dateTime.minute

    /** 24-hour HH:mm, locale-independent. */
    fun display(): String =
        String.format(Locale.US, "%02d:%02d", dateTime.hour, dateTime.minute)

    val instantMillis: Long get() = dateTime.toInstant().toEpochMilli()
}

/** All values for one local day. */
data class PrayerDay(
    val date: LocalDate,
    val zone: ZoneId,
    val settings: CalculationSettings,
    val fajr: PrayerMoment,
    val sunrise: PrayerMoment,
    val dhuhr: PrayerMoment,
    val asr: PrayerMoment,
    val maghrib: PrayerMoment,
    val isha: PrayerMoment,
    val imsak: PrayerMoment,
    val midnight: PrayerMoment,
    val lastThird: PrayerMoment,
    val tomorrowFajr: PrayerMoment
) {
    /** The five obligatory prayers in display order (sunrise is informational). */
    val prayers: List<Pair<PrayerName, PrayerMoment>>
        get() = listOf(
            PrayerName.FAJR to fajr,
            PrayerName.DHUHR to dhuhr,
            PrayerName.ASR to asr,
            PrayerName.MAGHRIB to maghrib,
            PrayerName.ISHA to isha
        )
}

enum class PrayerName { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

class SolarCalculationException(message: String) : IllegalStateException(message)

/**
 * Solar prayer-time calculator.
 *
 * Pipeline:
 * 1. Julian day for the local date at 00:00 UT.
 * 2. Apparent solar longitude -> declination + equation of time.
 * 3. Transit (Dhuhr) in UT hours.
 * 4. Hour angles for Fajr/Isha twilight angles, sunrise/sunset at -0.833 deg,
 *    and Asr from the shadow factor.
 * 5. Optional high-latitude bounding (night middle or angle based).
 * 6. Convert UT hours to the device zone with java.time (automatic DST) and
 *    round upward to the next whole minute.
 *
 * All internal arithmetic is in UT hours; the device zone is applied only at
 * the end, so the engine stays timezone-independent.
 */
object PrayerEngine {

    private const val SUNRISE_ALTITUDE = -0.833
    private const val DEG = PI / 180.0

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        settings: CalculationSettings = CalculationSettings()
    ): PrayerDay {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }

        val todaySolar = solarDay(date, latitude, longitude, settings)
            ?: throw SolarCalculationException("Sunrise/sunset undefined at this latitude on $date")

        val tomorrowSolar = solarDay(date.plusDays(1), latitude, longitude, settings)
            ?: throw SolarCalculationException("Sunrise/sunset undefined at this latitude on ${date.plusDays(1)}")

        // Night length runs from a sunset to the *following* sunrise, so each
        // day is bounded with the next day's sunrise rather than its own.
        val dayAfterSolar = solarDay(date.plusDays(2), latitude, longitude, settings)
        val tomorrowsNextSunrise = dayAfterSolar?.sunrise ?: (tomorrowSolar.sunrise + 24.0)

        val today = applyHighLatitude(todaySolar, tomorrowSolar.sunrise, settings)
        val tomorrow = applyHighLatitude(tomorrowSolar, tomorrowsNextSunrise, settings)

        val fajr = moment(date, today.fajr, zone)
        val sunrise = moment(date, today.sunrise, zone)
        val dhuhr = moment(date, today.transit, zone)
        val asr = moment(date, today.asr, zone)
        val maghrib = moment(date, today.sunset, zone)
        val isha = moment(date, today.isha, zone)
        val tomorrowFajr = moment(date.plusDays(1), tomorrow.fajr, zone)
        val tomorrowSunrise = moment(date.plusDays(1), tomorrow.sunrise, zone)

        val imsak = PrayerMoment(fajr.dateTime.minusMinutes(settings.imsakOffsetMinutes.toLong()))

        // Same convention as the original app (Fajr tomorrow minus a third of
        // the Isha-to-Fajr night), but measured on real instants so that an Isha
        // after midnight is handled correctly.
        val nightMinutes = Duration.between(isha.dateTime, tomorrowFajr.dateTime)
            .toMinutes().coerceAtLeast(0)
        val lastThird = PrayerMoment(
            tomorrowFajr.dateTime.minusMinutes(nightMinutes / 3)
        )

        // Solar midnight: midpoint between sunset and the following sunrise.
        val nightFromSunset = Duration.between(maghrib.dateTime, tomorrowSunrise.dateTime)
            .toMinutes().coerceAtLeast(0)
        val midnight = PrayerMoment(maghrib.dateTime.plusMinutes(nightFromSunset / 2))

        return PrayerDay(
            date = date,
            zone = zone,
            settings = settings,
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            imsak = imsak,
            midnight = midnight,
            lastThird = lastThird,
            tomorrowFajr = tomorrowFajr
        )
    }

    // ---------------------------------------------------------------------
    // Raw solar day (UT hours)
    // ---------------------------------------------------------------------

    private data class SolarDay(
        val transit: Double,
        val sunrise: Double,
        val sunset: Double,
        /** Fajr from the twilight angle, or NaN if the sun never reaches it. */
        val fajrAngle: Double,
        val asr: Double,
        /** Isha from the twilight angle (or sunset + interval), NaN if unreachable. */
        val ishaAngle: Double
    )

    private data class RawDay(
        val transit: Double,
        val sunrise: Double,
        val sunset: Double,
        val fajr: Double,
        val asr: Double,
        val isha: Double
    )

    /** Unbounded solar events for one date, as UT hours from that date's 00:00 UT. */
    private fun solarDay(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        settings: CalculationSettings
    ): SolarDay? {
        val julian = julianDay(date)
        val sunAt: (Double) -> SunPosition = { hours -> sunPosition(julian + hours / 24.0) }

        // Transit: the equation of time is evaluated at the transit instant
        // itself, not at 00:00 UT. Two refinement passes converge to well
        // under a second.
        var transit = 12.0 - longitude / 15.0 - sunAt(12.0).equationOfTime
        repeat(3) {
            transit = 12.0 - longitude / 15.0 - sunAt(transit).equationOfTime
        }

        // Hour-angle solutions are refined with the declination at the event
        // time. Declination moves ~0.4 deg/day, which is worth up to a minute
        // at mid latitudes if left at its 00:00 UT value.
        fun eventTime(altitude: Double, afterTransit: Boolean): Double {
            var angle = hourAngle(altitude, latitude, sunAt(transit).declination)
            if (angle.isNaN()) return Double.NaN
            repeat(3) {
                val event = if (afterTransit) transit + angle else transit - angle
                angle = hourAngle(altitude, latitude, sunAt(event).declination)
                if (angle.isNaN()) return Double.NaN
            }
            return if (afterTransit) transit + angle else transit - angle
        }

        val sunrise = eventTime(SUNRISE_ALTITUDE, false)
        val sunset = eventTime(SUNRISE_ALTITUDE, true)
        if (sunrise.isNaN() || sunset.isNaN()) return null

        // The Asr shadow ratio is referenced to the sun's declination at the
        // start of the UT day (the convention used by the independent
        // reference); the hour angle itself is then solved at the event time.
        val asrAngle = atan(
            1.0 / (settings.asrMadhab.shadowFactor +
                tan(abs(latitude - sunAt(0.0).declination) * DEG))
        ) / DEG
        val asr = eventTime(asrAngle, true)
        if (asr.isNaN()) return null

        val fajrAngle = eventTime(-settings.fajrAngle, false)
        val ishaAngle = if (settings.ishaIntervalMinutes > 0) {
            sunset + settings.ishaIntervalMinutes / 60.0
        } else {
            eventTime(-settings.ishaAngle, true)
        }

        return SolarDay(transit, sunrise, sunset, fajrAngle, asr, ishaAngle)
    }

    /**
     * Applies the high-latitude rule. [nextSunrise] is the sunrise of the
     * following day, so [SolarDay.sunset] to [nextSunrise] is the real night.
     */
    private fun applyHighLatitude(
        day: SolarDay,
        nextSunrise: Double,
        settings: CalculationSettings
    ): RawDay {
        val night = fixHour(nextSunrise - day.sunset)

        val fajrPortion = when (settings.highLatitudeRule) {
            HighLatitudeRule.NIGHT_MIDDLE -> night / 2.0
            HighLatitudeRule.ANGLE_BASED -> settings.fajrAngle / 60.0 * night
        }
        var fajr = day.fajrAngle
        if (fajr.isNaN() || fajr < day.sunrise - fajrPortion) {
            fajr = day.sunrise - fajrPortion
        }

        var isha = day.ishaAngle
        if (settings.ishaIntervalMinutes == 0) {
            val ishaPortion = when (settings.highLatitudeRule) {
                HighLatitudeRule.NIGHT_MIDDLE -> night / 2.0
                HighLatitudeRule.ANGLE_BASED -> settings.ishaAngle / 60.0 * night
            }
            if (isha.isNaN() || isha > day.sunset + ishaPortion) {
                isha = day.sunset + ishaPortion
            }
        }

        return RawDay(day.transit, day.sunrise, day.sunset, fajr, day.asr, isha)
    }

    // ---------------------------------------------------------------------
    // Astronomy
    // ---------------------------------------------------------------------

    /** Julian day at 00:00 UT for the supplied Gregorian date. */
    fun julianDay(date: LocalDate): Double {
        var year = date.year
        var month = date.monthValue
        val day = date.dayOfMonth
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = kotlin.math.floor(year / 100.0)
        val b = 2.0 - a + kotlin.math.floor(a / 4.0)
        return kotlin.math.floor(365.25 * (year + 4716)) +
            kotlin.math.floor(30.6001 * (month + 1)) +
            day + b - 1524.5
    }

    private data class SunPosition(
        val declination: Double,
        val equationOfTime: Double
    )

    private fun sunPosition(julianDay: Double): SunPosition {
        val d = julianDay - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(g * DEG) + 0.020 * sin(2 * g * DEG))
        val e = 23.439 - 0.00000036 * d

        val rightAscension = fixHour(
            atan2(cos(e * DEG) * sin(l * DEG), cos(l * DEG)) / DEG / 15.0
        )
        val declination = asin(sin(e * DEG) * sin(l * DEG)) / DEG
        val equationOfTime = q / 15.0 - rightAscension
        return SunPosition(declination, equationOfTime)
    }

    /** Hour angle in hours, or NaN when the altitude is never reached. */
    private fun hourAngle(altitude: Double, latitude: Double, declination: Double): Double {
        val cosH = (sin(altitude * DEG) - sin(latitude * DEG) * sin(declination * DEG)) /
            (cos(latitude * DEG) * cos(declination * DEG))
        if (cosH.isNaN() || cosH > 1.0 || cosH < -1.0) return Double.NaN
        return acos(cosH) / DEG / 15.0
    }

    private fun fixAngle(angle: Double): Double {
        var value = angle % 360.0
        if (value < 0.0) value += 360.0
        return value
    }

    private fun fixHour(hour: Double): Double {
        var value = hour % 24.0
        if (value < 0.0) value += 24.0
        return value
    }

    // ---------------------------------------------------------------------
    // Timezone conversion + ceiling
    // ---------------------------------------------------------------------

    private fun moment(date: LocalDate, utcHours: Double, zone: ZoneId): PrayerMoment {
        val startOfUtcDay = date.atStartOfDay(ZoneOffset.UTC)
        val millis = (utcHours * 3_600_000.0).roundToLong()
        val zoned = startOfUtcDay.toInstant().plusMillis(millis).atZone(zone)
        return PrayerMoment(ceilToNextMinute(zoned))
    }

    /**
     * Rounds upward to the next whole minute. An exact minute stays unchanged;
     * the tiny epsilon absorbs floating-point noise.
     */
    private fun ceilToNextMinute(time: ZonedDateTime): ZonedDateTime {
        val startOfLocalDay = time.toLocalDate().atStartOfDay(time.zone)
        val seconds = time.toLocalTime().toSecondOfDay() +
            time.nano / 1_000_000_000.0
        val minutes = ceil(seconds / 60.0 - 1e-9).toLong()
        return startOfLocalDay.plusMinutes(minutes)
    }
}
