package com.mawaqiti.plus.notify

import com.mawaqiti.plus.R
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.HijriDateFormatter
import com.mawaqiti.plus.format.TimeFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

/**
 * Pure formatting rules for the widget. Keeping them out of the RemoteViews
 * updater makes the 12h/24h and Arabic/English matrix unit-testable without a
 * device or a launcher.
 */
object WidgetText {

    private val order = listOf(TableEvent.FAJR, TableEvent.SUNRISE, TableEvent.DHUHR,
        TableEvent.ASR, TableEvent.MAGHRIB, TableEvent.ISHA)

    fun clock(minutes: Int, clock: ClockFormat, isArabic: Boolean): String =
        TimeFormatter.format(minutes, clock, isArabic)

    fun gregorian(date: LocalDate, isArabic: Boolean): String {
        val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH
        val pattern = if (isArabic) "d MMM yyyy" else "d MMM yyyy"
        return DateTimeFormatter.ofPattern(pattern, locale)
            .withDecimalStyle(DecimalStyle.STANDARD)
            .format(date)
    }

    fun hijri(date: LocalDate, isArabic: Boolean): String =
        HijriDateFormatter.format(date, isArabic)

    /** The next of the six table times, or tomorrow's Fajr after Isha. */
    fun nextPrayer(day: TableDay, nowMinutes: Int): Pair<TableEvent, Int> {
        val nowSeconds = nowMinutes * 60L
        for (event in order) {
            val minutes = day.minutesFor(event)
            if (minutes * 60L > nowSeconds) return event to minutes
        }
        return TableEvent.FAJR to day.fajrTomorrow
    }

    fun labelRes(event: TableEvent): Int = when (event) {
        TableEvent.FAJR -> R.string.prayer_fajr
        TableEvent.SUNRISE -> R.string.prayer_sunrise
        TableEvent.DHUHR -> R.string.prayer_dhuhr
        TableEvent.ASR -> R.string.prayer_asr
        TableEvent.MAGHRIB -> R.string.prayer_maghrib
        TableEvent.ISHA -> R.string.prayer_isha
    }

    /**
     * beta 7.9: short "in Xh Ym" for the next-prayer widget, computed once at
     * update time (the widget never ticks). Wraps around midnight so the Fajr
     * tomorrow case is always positive.
     */
    fun remaining(minutesUntil: Int, isArabic: Boolean): String {
        val left = ((minutesUntil % 1440) + 1440) % 1440
        val hours = left / 60
        val minutes = left % 60
        val body = when {
            hours > 0 && minutes > 0 ->
                if (isArabic) "${hours}س ${minutes}د" else "${hours}h ${minutes}m"
            hours > 0 ->
                if (isArabic) "${hours}س" else "${hours}h"
            else ->
                if (isArabic) "${minutes}د" else "${minutes}m"
        }
        return (if (isArabic) "بعد " else "in ") + body
    }

    /** Minutes from [nowMinutes] until [nextMinutes], wrapping past midnight. */
    fun minutesUntil(nextMinutes: Int, nowMinutes: Int): Int {
        val delta = nextMinutes - nowMinutes
        return if (delta < 0) delta + 1440 else delta
    }

    fun allEvents(): List<TableEvent> = order
}
