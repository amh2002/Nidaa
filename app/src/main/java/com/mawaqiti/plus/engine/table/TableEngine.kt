package com.mawaqiti.plus.engine.table

import java.time.LocalDate
import java.time.Year
import java.util.Locale

/** The three tables shipped in the original APK (w7.c). */
enum class TableProfile {
    DEFAULT,
    MAKKAH,
    MADINAH
}

/** Event order used by every table array. */
enum class TableEvent {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA
}

/** One day of table output, in minutes since local midnight. */
data class TableDay(
    val fajr: Int,
    val sunrise: Int,
    val dhuhr: Int,
    val asr: Int,
    val maghrib: Int,
    val isha: Int,
    /** Start of the last third of the night (original app convention). */
    val lastThird: Int,
    val fajrTomorrow: Int,
    val ishaYesterday: Int,
    val row: Int
) {
    fun minutesFor(event: TableEvent): Int = when (event) {
        TableEvent.FAJR -> fajr
        TableEvent.SUNRISE -> sunrise
        TableEvent.DHUHR -> dhuhr
        TableEvent.ASR -> asr
        TableEvent.MAGHRIB -> maghrib
        TableEvent.ISHA -> isha
    }
}

/**
 * Bit-exact port of the original app's calculation path
 * (`p061i1.j.o()` / `p061i1.j.w()`).
 *
 * There is no astronomy here on purpose: the original APK ships 36 integer
 * arrays and the only runtime arithmetic is the optional +60 minute summer
 * toggle (default profile only), the per-city [minDiff], minute carry/borrow,
 * and hour wrap.
 */
object TableEngine {

    const val MINUTES_PER_DAY = 24 * 60

    /** Table row for a date: 366 rows with 29 February at index 59. */
    fun rowFor(date: LocalDate): Int {
        var dayOfYear = date.dayOfYear
        val leap = Year.isLeap(date.year.toLong())
        // p061i1.j.o: common year and doy > 59 -> increment, then subtract 1.
        if (!leap && dayOfYear > 59) {
            dayOfYear += 1
        }
        return dayOfYear - 1
    }

    /** Look up one event exactly as the original app does. */
    fun eventMinutes(
        profile: TableProfile,
        event: TableEvent,
        date: LocalDate,
        minDiff: Int,
        summerTime: Boolean
    ): Int {
        val row = rowFor(date)
        val hours = hoursFor(profile)[event.ordinal]
        val minutes = minutesFor(profile)[event.ordinal]

        var hour = hours[row]
        var minute = minutes[row]

        // p061i1.j.o: if (summerTime && !makkah/madinah) hour++;
        if (summerTime && profile == TableProfile.DEFAULT) {
            hour += 1
        }

        // int minDiff = city.getMinDiff() + tableMinute;
        minute += minDiff
        if (minute >= 60) {
            hour += minute / 60
            minute %= 60
        } else if (minute < 0) {
            // Ceiling division, exactly as in the decompiled bytecode.
            val borrow = (-minute + 59) / 60
            hour -= borrow
            minute += borrow * 60
        }

        if (hour < 0) {
            hour += 24
        }
        hour %= 24

        return hour * 60 + minute
    }

    /** Full day for a city: six events plus the derived last third. */
    fun day(
        profile: TableProfile,
        minDiff: Int,
        date: LocalDate,
        summerTime: Boolean
    ): TableDay {
        fun minutes(event: TableEvent, d: LocalDate = date): Int =
            eventMinutes(profile, event, d, minDiff, summerTime)

        val fajr = minutes(TableEvent.FAJR)
        val sunrise = minutes(TableEvent.SUNRISE)
        val dhuhr = minutes(TableEvent.DHUHR)
        val asr = minutes(TableEvent.ASR)
        val maghrib = minutes(TableEvent.MAGHRIB)
        val isha = minutes(TableEvent.ISHA)

        val fajrTomorrow = minutes(TableEvent.FAJR, date.plusDays(1))
        val ishaYesterday = minutes(TableEvent.ISHA, date.minusDays(1))

        // p061i1.j.w: night = (24:00 - Isha) + Fajr tomorrow;
        // lastThird = Fajr tomorrow - floor(night / 3).
        val night = (MINUTES_PER_DAY - isha) + fajrTomorrow
        val third = night / 3
        val lastThird = ((fajrTomorrow - third) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY

        return TableDay(
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            lastThird = lastThird,
            fajrTomorrow = fajrTomorrow,
            ishaYesterday = ishaYesterday,
            row = rowFor(date)
        )
    }

    /** `%02d:%02d` with Latin digits, exactly like the original. */
    fun format(minutes: Int): String {
        val normalized = ((minutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return String.format(
            Locale.US,
            "%02d:%02d",
            normalized / 60,
            normalized % 60
        )
    }

    private fun hoursFor(profile: TableProfile): Array<IntArray> = when (profile) {
        TableProfile.DEFAULT -> PrayerTables.Default.HOURS
        TableProfile.MAKKAH -> PrayerTables.Makkah.HOURS
        TableProfile.MADINAH -> PrayerTables.Madinah.HOURS
    }

    private fun minutesFor(profile: TableProfile): Array<IntArray> = when (profile) {
        TableProfile.DEFAULT -> PrayerTables.Default.MINUTES
        TableProfile.MAKKAH -> PrayerTables.Makkah.MINUTES
        TableProfile.MADINAH -> PrayerTables.Madinah.MINUTES
    }
}
