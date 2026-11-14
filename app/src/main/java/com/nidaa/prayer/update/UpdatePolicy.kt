package com.nidaa.prayer.update

/**
 * Pure cadence rule: an automatic check runs at most once per 24 hours, only
 * while the feature is enabled. Manual checks bypass this rule.
 */
object UpdatePolicy {

    const val CHECK_INTERVAL_MS: Long = 24L * 60L * 60L * 1000L

    fun shouldAutoCheck(enabled: Boolean, lastCheckMs: Long, nowMs: Long): Boolean {
        if (!enabled) return false
        if (lastCheckMs <= 0L) return true
        // A clock change must not lock the updater out for days.
        if (nowMs < lastCheckMs) return true
        return nowMs - lastCheckMs >= CHECK_INTERVAL_MS
    }
}
