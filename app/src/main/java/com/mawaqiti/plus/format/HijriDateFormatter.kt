package com.mawaqiti.plus.format

import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone as IcuTimeZone
import android.icu.util.ULocale
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Locale

/** One Hijri calendar date, independent of how it was computed. */
data class HijriDate(
    val year: Int,
    val month: Int,
    val day: Int
)

/**
 * Presentation-only Hijri date.
 *
 * The primary conversion is `java.time.chrono.HijrahDate`, whose chronology is
 * the Umm al-Qura calendar. Core-library desugaring makes it available down to
 * the app's minSdk (24). On the unlikely device where the java.time conversion
 * fails, the Android ICU `islamic-umalqura` calendar is used as a fallback.
 *
 * No moon-sighting arithmetic is hand-rolled here: both paths delegate to the
 * platform chronologies. The conversion takes the same [LocalDate] the app
 * already treats as "today", so it follows the device's local timezone and
 * advances when the Gregorian date crosses midnight.
 */
object HijriDateFormatter {

    private val arabicMonths = listOf(
        "محرم",
        "صفر",
        "ربيع الأول",
        "ربيع الآخر",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوال",
        "ذو القعدة",
        "ذو الحجة"
    )

    private val englishMonths = listOf(
        "Muharram",
        "Safar",
        "Rabi' al-Awwal",
        "Rabi' al-Thani",
        "Jumada al-Ula",
        "Jumada al-Akhirah",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    /**
     * Renders a Gregorian [date] as a Hijri line, always with Latin digits:
     * Arabic `19 جمادى الآخرة 1448هـ`, English `19 Jumada al-Akhirah 1448 AH`.
     */
    fun format(date: LocalDate, isArabic: Boolean): String {
        val hijri = toHijri(date)
        val month = monthName(hijri.month, isArabic)
        return if (isArabic) {
            String.format(Locale.US, "%d %s %dهـ", hijri.day, month, hijri.year)
        } else {
            String.format(Locale.US, "%d %s %d AH", hijri.day, month, hijri.year)
        }
    }

    /** Gregorian → Hijri using java.time first, then Android ICU. */
    fun toHijri(date: LocalDate): HijriDate = try {
        val hijri = HijrahDate.from(date)
        HijriDate(
            year = hijri.get(ChronoField.YEAR),
            month = hijri.get(ChronoField.MONTH_OF_YEAR),
            day = hijri.get(ChronoField.DAY_OF_MONTH)
        )
    } catch (error: RuntimeException) {
        fromIcu(date)
    }

    fun monthName(month: Int, isArabic: Boolean): String {
        require(month in 1..12) { "Hijri month must be 1..12, was $month" }
        return if (isArabic) arabicMonths[month - 1] else englishMonths[month - 1]
    }

    /** Month names in calendar order; used by tests and documentation. */
    fun monthNames(isArabic: Boolean): List<String> =
        if (isArabic) arabicMonths.toList() else englishMonths.toList()

    private fun fromIcu(date: LocalDate): HijriDate {
        val calendar = IslamicCalendar(IcuTimeZone.getDefault(), ULocale.ROOT)
        calendar.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        calendar.timeInMillis = date
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return HijriDate(
            year = calendar.get(android.icu.util.Calendar.YEAR),
            month = calendar.get(android.icu.util.Calendar.MONTH) + 1,
            day = calendar.get(android.icu.util.Calendar.DAY_OF_MONTH)
        )
    }
}
