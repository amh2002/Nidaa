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
 * midnight into the string shown on screen. Digits are Latin by default, in
 * both languages, matching the beta1 behaviour.
 *
 * beta30: [arabicIndic] is the «الأرقام الهندية» setting. It is opt-in and
 * defaults to false, so every existing caller - notifications, the settings
 * preview, the TV dashboard - keeps the beta29 rendering unless it asks for the
 * rewording. The conversion happens on the finished string through the single
 * [Numerals] table, never on an epoch or a stored minute.
 */
object TimeFormatter {

    fun format(
        minutesOfDay: Int,
        clock: ClockFormat,
        isArabic: Boolean,
        arabicIndic: Boolean = false
    ): String {
        val minutes = ((minutesOfDay % 1440) + 1440) % 1440
        val hour = minutes / 60
        val minute = minutes % 60
        val text = when (clock) {
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
        return if (arabicIndic) Numerals.toArabicIndic(text) else text
    }

    /** Elapsed-style countdown; always 24h, as required by the hero pill. */
    fun formatCountdown(totalSeconds: Long, arabicIndic: Boolean = false): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        val text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        return if (arabicIndic) Numerals.toArabicIndic(text) else text
    }
}
