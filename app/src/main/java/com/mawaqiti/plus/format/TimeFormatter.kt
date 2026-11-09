package com.mawaqiti.plus.format

import java.util.Locale

/** Display-only clock preference. Engine values always stay 24h minutes. */
enum class ClockFormat(val storageKey: String) {
    H24("24"),
    H12("12");

    companion object {
        fun fromKey(key: String?): ClockFormat =
            entries.firstOrNull { it.storageKey == key } ?: H24
    }
}

/**
 * Presentation-layer formatter.
 *
 * It never changes a stored or calculated value: it only turns minutes-since-
 * midnight into the string shown on screen. Digits are always Latin, in both
 * languages, matching the beta1 behaviour.
 */
object TimeFormatter {

    fun format(minutesOfDay: Int, clock: ClockFormat, isArabic: Boolean): String {
        val minutes = ((minutesOfDay % 1440) + 1440) % 1440
        val hour = minutes / 60
        val minute = minutes % 60
        return when (clock) {
            ClockFormat.H24 -> String.format(Locale.US, "%02d:%02d", hour, minute)
            ClockFormat.H12 -> {
                val hour12 = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                val suffix = when {
                    isArabic && hour < 12 -> "ص"
                    isArabic -> "م"
                    hour < 12 -> "a.m."
                    else -> "p.m."
                }
                String.format(Locale.US, "%d:%02d %s", hour12, minute, suffix)
            }
        }
    }

    /** Elapsed-style countdown; always 24h, as required by the hero pill. */
    fun formatCountdown(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
