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

    fun allEvents(): List<TableEvent> = order
}
