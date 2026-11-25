package com.mawaqiti.plus.notify

import com.mawaqiti.plus.data.PreAlertMinutes
import java.time.ZonedDateTime

/**
 * One pre-alert occurrence: the prayer, the instant the reminder must fire, and
 * the epoch of the prayer occurrence it belongs to (the ledger identity, so the
 * reminder cannot be delivered twice even if the lead time changes).
 */
data class PlannedPreAlert(
    val event: NotifyEvent,
    val fireAt: ZonedDateTime,
    val occurrenceEpoch: Long
)

/**
 * beta22: pure pre-alert planner.
 *
 * It derives the reminder instants from the same 48-hour prayer plan the adhan
 * alarms already use, so it can never disagree with the table engine: a
 * pre-alert is exactly `adhan - minutesBefore`.
 *
 * Only the five prayers get a reminder (sunrise and the derived last third are
 * excluded), and only the NEXT occurrence of each prayer is planned - the plan
 * is rebuilt at every alarm and at midnight, so exactly five pre-alerts exist
 * at any moment instead of ten across the 48-hour window.
 *
 * Occurrences whose reminder instant is already in the past are skipped, and
 * [PreAlertMinutes.DISABLED] plans nothing at all.
 *
 * beta35: an occurrence the ledger already delivered is skipped too, so a clock
 * rollback cannot re-arm a reminder that was already shown. The next undelivered
 * occurrence for that prayer is planned instead of leaving the prayer unarmed.
 */
object PreAlertPlanner {

    fun plan(
        alarms: List<PlannedAlarm>,
        now: ZonedDateTime,
        minutesBefore: Int,
        /** beta35: true for an occurrence the ledger already delivered. */
        isDelivered: (PlannedPreAlert) -> Boolean = { false }
    ): List<PlannedPreAlert> {
        if (minutesBefore <= 0) return emptyList()
        val lead = minutesBefore.toLong()

        return NotifyEvent.PRE_ALERT_EVENTS.mapNotNull { event ->
            alarms.asSequence()
                .filter { it.event == event }
                .map {
                    PlannedPreAlert(
                        event = event,
                        fireAt = it.fireAt.minusMinutes(lead),
                        occurrenceEpoch = it.fireAt.toInstant().toEpochMilli()
                    )
                }
                .firstOrNull { it.fireAt.isAfter(now) && !isDelivered(it) }
        }.sortedBy { it.fireAt.toInstant() }
    }
}
