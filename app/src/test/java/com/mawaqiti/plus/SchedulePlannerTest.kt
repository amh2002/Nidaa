package com.mawaqiti.plus

import com.mawaqiti.plus.notify.DayTimes
import com.mawaqiti.plus.notify.NotifyEvent
import com.mawaqiti.plus.notify.SchedulePlanner
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixed-clock tests for the 48-hour planner. No Android, no device: the source
 * is a lambda so summer time, midnight wrap and skip logic are all explicit.
 */
class SchedulePlannerTest {

    private val zone: ZoneId = ZoneId.of("Asia/Hebron")

    private fun times(offset: Int = 0) = DayTimes(
        fajr = 300 + offset,
        sunrise = 360 + offset,
        dhuhr = 720 + offset,
        asr = 930 + offset,
        maghrib = 1140 + offset,
        isha = 1200 + offset,
        lastThird = 60 + offset
    )

    private val source: (LocalDate) -> DayTimes = { times() }

    private fun now(hour: Int, minute: Int = 0): ZonedDateTime =
        ZonedDateTime.of(2026, 6, 1, hour, minute, 0, 0, zone)

    @Test
    fun `plans roughly fourteen alarms in the next 48 hours`() {
        val plan = SchedulePlanner.plan(now(10), { date -> source(date) })

        assertEquals(14, plan.size)
        assertEquals(NotifyEvent.DHUHR, plan.first().event)
        assertEquals(12, plan.first().fireAt.hour)
        assertTrue(plan.all { it.fireAt.isAfter(now(10)) })
        assertTrue(plan.all { it.fireAt.isBefore(now(10).plusHours(48).plusNanos(1)) })
    }

    @Test
    fun `summer time shifts every fire instant by one hour`() {
        val summerPlan = SchedulePlanner.plan(
            now = now(10),
            source = { date -> times(offset = 60) }
        )

        val winterFirst = SchedulePlanner.plan(now(10), { date -> times() }).first()
        val summerFirst = summerPlan.first()

        assertEquals(winterFirst.fireAt.plusHours(1), summerFirst.fireAt)
        assertEquals(13, summerFirst.fireAt.hour)
    }

    @Test
    fun `midnight wrap advances to tomorrow and keeps the order`() {
        val late = now(23, 30)
        val plan = SchedulePlanner.plan(late, { date -> times() })

        val first = plan.first()
        assertEquals(NotifyEvent.LAST_THIRD, first.event)
        assertEquals(LocalDate.of(2026, 6, 2), first.fireAt.toLocalDate())
        assertEquals(1, first.fireAt.hour)
        assertTrue(plan.zipWithNext().all { (a, b) -> !b.fireAt.isBefore(a.fireAt) })
    }

    @Test
    fun `events that already passed today are skipped not duplicated`() {
        val plan = SchedulePlanner.plan(now(10), { date -> times() })

        val fajrAlarms = plan.filter { it.event == NotifyEvent.FAJR }
        assertEquals(2, fajrAlarms.size)
        assertEquals(LocalDate.of(2026, 6, 2), fajrAlarms.first().fireAt.toLocalDate())
        assertFalse(plan.any { it.fireAt.isBefore(now(10)) })
    }

    @Test
    fun `every occurrence keeps its fixed per-event request code`() {
        val plan = SchedulePlanner.plan(now(10), { date -> times() })

        assertTrue(plan.all { it.requestCode == it.event.requestCodeBase })
        assertEquals(14, plan.map { "${it.event.storageKey}|${it.fireAt.toInstant()}" }.distinct().size)
    }

    @Test
    fun `last third is scheduled on the following date at its night start`() {
        val plan = SchedulePlanner.plan(now(10), { date -> times() })
        val lastThird = plan.first { it.event == NotifyEvent.LAST_THIRD }
        assertEquals(LocalDate.of(2026, 6, 2), lastThird.fireAt.toLocalDate())
        assertEquals(1, lastThird.fireAt.hour)
        assertEquals(0, lastThird.fireAt.minute)
    }
}
