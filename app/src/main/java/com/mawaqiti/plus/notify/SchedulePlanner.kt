package com.mawaqiti.plus.notify

import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.engine.table.TableEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Six table times plus the derived last third, in minutes since midnight. */
data class DayTimes(
    val fajr: Int,
    val sunrise: Int,
    val dhuhr: Int,
    val asr: Int,
    val maghrib: Int,
    val isha: Int,
    val lastThird: Int
) {
    fun minutesFor(event: NotifyEvent): Int = when (event) {
        NotifyEvent.FAJR -> fajr
        NotifyEvent.SUNRISE -> sunrise
        NotifyEvent.DHUHR -> dhuhr
        NotifyEvent.ASR -> asr
        NotifyEvent.MAGHRIB -> maghrib
        NotifyEvent.ISHA -> isha
        NotifyEvent.LAST_THIRD -> lastThird
    }
}

/** Supplies the table times for a date; injected so tests need no Android. */
fun interface DayTimesSource {
    fun forDate(date: LocalDate): DayTimes
}

/**
 * Production source: the existing [TableEngine], with the manual summer-time
 * toggle applied exactly as the UI applies it (Palestine only).
 */
class TableDayTimesSource(
    private val city: City,
    private val summerTime: Boolean
) : DayTimesSource {
    override fun forDate(date: LocalDate): DayTimes {
        val day = TableEngine.day(
            profile = city.profile,
            minDiff = city.minDiff,
            date = date,
            summerTime = summerTime && city.country == Country.PS
        )
        return DayTimes(
            fajr = day.fajr,
            sunrise = day.sunrise,
            dhuhr = day.dhuhr,
            asr = day.asr,
            maghrib = day.maghrib,
            isha = day.isha,
            lastThird = day.lastThird
        )
    }
}

/**
 * Pure 48-hour planner.
 *
 * The last third belongs to the night that starts on [date], so its
 * occurrence is placed on `date + 1` at [DayTimes.lastThird]. Dates one day
 * before and after "today" are included so an event shortly before midnight
 * and the corresponding early-morning event are both found.
 */
object SchedulePlanner {

    const val HORIZON_HOURS = 48L

    fun plan(
        now: ZonedDateTime,
        source: DayTimesSource,
        horizonHours: Long = HORIZON_HOURS
    ): List<PlannedAlarm> {
        val end = now.plusHours(horizonHours)
        val today = now.toLocalDate()
        val result = ArrayList<PlannedAlarm>(16)

        for (offset in -1L..2L) {
            val date = today.plusDays(offset)
            val times = source.forDate(date)
            for (event in NotifyEvent.entries) {
                val dayOffset = if (event == NotifyEvent.LAST_THIRD) 1L else 0L
                val minutes = times.minutesFor(event)
                val fireAt = ZonedDateTime.of(
                    date.plusDays(dayOffset),
                    LocalTime.of((minutes / 60) % 24, minutes % 60),
                    now.zone
                )
                if (fireAt.isAfter(now) && !fireAt.isAfter(end)) {
                    result += PlannedAlarm(event, fireAt, event.requestCodeBase)
                }
            }
        }

        return result.sortedBy { it.fireAt.toInstant() }
    }

    /** The first occurrence of [event] strictly after [now], if within 48 h. */
    fun nextOccurrence(
        now: ZonedDateTime,
        source: DayTimesSource,
        event: NotifyEvent
    ): ZonedDateTime? = plan(now, source).firstOrNull { it.event == event }?.fireAt
}
