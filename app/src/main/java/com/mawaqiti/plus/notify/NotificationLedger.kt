package com.mawaqiti.plus.notify

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * beta35 (BUG-1): durable ledger of the notification occurrences already handled.
 *
 * `SchedulePlanner` arms an occurrence whenever `fireAt` is still ahead of the
 * clock. That is correct for the forward direction only: when the system
 * corrects the clock backwards - an automatic NITZ/NTP correction, a manual
 * change, a timezone or DST switch - an alarm that already fired becomes
 * "future" again, is re-armed by the reschedule path and is delivered a second
 * time. The planner therefore needs a memory of *what this occurrence already
 * did*, not just of where `now` sits.
 *
 * Identity is (event, occurrence epoch millis, kind). Epoch millis is part of
 * the key, so the same prayer on two days is two entries while a re-plan of the
 * same prayer is one entry. [claim] is the atomic check-and-set the delivery
 * paths use; [isDelivered] is the read the plan path uses to drop an occurrence
 * before it is armed again.
 *
 * Storage is SharedPreferences - the scheduler's existing pattern, so no new
 * dependency. Every entry is kept for [RETENTION_MS] after its occurrence epoch
 * and older entries are pruned on each claim: the ledger stays well under the
 * 48-hour plan horizon while never forgetting an occurrence that could still be
 * re-armed.
 */
object NotificationLedger {

    /** Which delivery an entry belongs to; the occurrence epoch alone is not unique across kinds. */
    enum class Kind(val storageKey: String) {
        FIRE("fire"),
        PRE_ALERT("pre")
    }

    private const val PREFS = "mawaqiti_ledger"
    private const val KEY_DELIVERED = "delivered"
    private const val RETENTION_MS = 7L * 24 * 60 * 60 * 1000
    private const val TAG = "NidaaLedger"

    /** Guards the read-modify-write of the whole set, so claim/isDelivered cannot interleave. */
    private val lock = Any()

    fun key(event: NotifyEvent, occurrenceEpoch: Long, kind: Kind): String =
        "${kind.storageKey}|${event.storageKey}|$occurrenceEpoch"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** True when this occurrence was already handled; the plan path never re-arms it. */
    fun isDelivered(context: Context, key: String): Boolean = synchronized(lock) {
        prefs(context).getStringSet(KEY_DELIVERED, emptySet()).orEmpty().contains(key)
    }

    /**
     * Atomic check-and-set for a delivery.
     *
     * Returns true exactly once per occurrence; false means the occurrence was
     * already delivered and the caller must drop it silently - no notification
     * and no adhan.
     */
    fun claim(context: Context, key: String): Boolean = synchronized(lock) {
        val p = prefs(context)
        val delivered = p.getStringSet(KEY_DELIVERED, emptySet()).orEmpty()
        if (key in delivered) {
            Log.i(TAG, "drop already-delivered $key")
            false
        } else {
            val cutoff = System.currentTimeMillis() - RETENTION_MS
            val kept = delivered.filterTo(HashSet()) { entry ->
                val epoch = entry.substringAfterLast('|').toLongOrNull()
                epoch != null && epoch >= cutoff
            }
            kept += key
            // commit, not apply: the claim must be on disk before the notification
            // is posted, so a process death right after it cannot resurrect it.
            p.edit().putStringSet(KEY_DELIVERED, kept).commit()
            Log.i(TAG, "claim $key entries=${kept.size}")
            true
        }
    }
}
