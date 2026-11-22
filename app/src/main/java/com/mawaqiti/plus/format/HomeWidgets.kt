package com.mawaqiti.plus.format

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoUnit

/**
 * beta25: rotation of «ذكر اليوم».
 *
 * The index is derived from the Hijri day count, not from a random or a stored
 * value, so the same phrase is shown for the whole Hijri day on every device
 * and changes exactly when the Hijri date does. Both inputs come from the
 * app's existing Hijri source ([HijriDateFormatter]): the Umm al-Qura
 * chronology the home screen already renders.
 */
object DailyDhikr {

    /** Index into a list of [size] phrases; a negative day can never be returned. */
    fun index(hijriEpochDay: Long, size: Int): Int {
        if (size <= 0) return 0
        return Math.floorMod(hijriEpochDay, size.toLong()).toInt()
    }

    /**
     * Days since the start of the Hijri epoch for [date]. Falls back to the
     * Gregorian epoch day on a device whose java.time conversion fails, which
     * keeps the rotation stable instead of crashing.
     */
    fun hijriEpochDay(date: LocalDate): Long = try {
        HijrahDate.from(date).toEpochDay()
    } catch (error: RuntimeException) {
        date.toEpochDay()
    }
}

/** The four occasions the countdown targets, by Hijri month and day. */
enum class IslamicEventTarget(val month: Int, val day: Int) {
    RAMADAN(month = 9, day = 1),
    EID_FITR(month = 10, day = 1),
    EID_ADHA(month = 12, day = 10),
    NEW_YEAR(month = 1, day = 1)
}

/** One resolved countdown: the nearest target and the whole days until it. */
data class EventCountdown(
    val target: IslamicEventTarget,
    val daysLeft: Long
) {
    /** True on the event day itself - the UI then says «اليوم» instead. */
    val isToday: Boolean get() = daysLeft == 0L
}

/**
 * Nearest upcoming occasion, computed entirely inside the Hijri calendar.
 *
 * Both this year's and next year's occurrence of every target are considered,
 * so the answer stays right across a Hijri year boundary, and the smallest
 * non-negative day delta wins. A target that falls today is returned with
 * [EventCountdown.daysLeft] = 0 rather than being skipped.
 */
object EventCountdownCalculator {

    fun next(today: HijriDate): EventCountdown? {
        val todayHijri = runCatching {
            HijrahDate.of(today.year, today.month, today.day)
        }.getOrNull() ?: return null

        var best: EventCountdown? = null
        for (target in IslamicEventTarget.entries) {
            for (yearOffset in 0..1) {
                val candidate = runCatching {
                    HijrahDate.of(today.year + yearOffset, target.month, target.day)
                }.getOrNull() ?: continue
                val delta = ChronoUnit.DAYS.between(todayHijri, candidate)
                if (delta < 0) continue
                if (best == null || delta < best.daysLeft) {
                    best = EventCountdown(target = target, daysLeft = delta)
                }
            }
        }
        return best
    }
}

/**
 * Decorative moon phase, approximated from the Hijri day of month exactly as
 * the beta25 specification bands it. It is a visual companion to the date, not
 * an astronomical calculation.
 */
enum class MoonPhaseKind {
    NEW_CRESCENT,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT,
    OLD_CRESCENT;

    companion object {
        fun forHijriDay(day: Int): MoonPhaseKind = when (day.coerceIn(1, 30)) {
            in 1..2 -> NEW_CRESCENT
            in 3..6 -> WAXING_CRESCENT
            in 7..9 -> FIRST_QUARTER
            in 10..13 -> WAXING_GIBBOUS
            in 14..15 -> FULL
            in 16..20 -> WANING_GIBBOUS
            in 21..23 -> LAST_QUARTER
            in 24..27 -> WANING_CRESCENT
            else -> OLD_CRESCENT
        }
    }
}

/** Lit fraction of the disc (0 = new, 1 = full) for a Hijri day of month. */
object MoonIllumination {

    /** True while the disc grows (first half of the Hijri month). */
    fun waxing(day: Int): Boolean = day.coerceIn(1, 30) <= 15

    /**
     * Simple synodic approximation: the month is treated as 29 days, so day 1
     * is the new moon and day 15 is full. Clamped to 0…1 for the canvas.
     */
    fun illuminatedFraction(day: Int): Float {
        val monthPosition = (day.coerceIn(1, 30) - 1) / 29f
        val fraction = (1f - kotlin.math.cos(2.0 * Math.PI * monthPosition)) / 2.0
        return fraction.toFloat().coerceIn(0f, 1f)
    }
}
