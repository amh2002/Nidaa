package com.mawaqiti.plus.data

import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.notify.NotificationSettings

enum class AppLanguage(val tag: String) {
    ARABIC("ar"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ARABIC
    }
}

/**
 * Everything the beta persists. Times and the qibla bearing are derived from
 * [cityOrder] alone, so there is no latitude/longitude state to keep in sync.
 */
data class AppSettings(
    val cityOrder: Int = City.DEFAULT_ORDER,
    /** Manual +1 hour toggle, shown only for Palestine. Default OFF. */
    val summerTime: Boolean = false,
    val language: AppLanguage = AppLanguage.ARABIC,
    /** False only before the very first launch picks Arabic explicitly. */
    val languageInitialized: Boolean = false,
    /** Display-only clock preference; default is the original app's 24h. */
    val clockFormat: ClockFormat = ClockFormat.H24,
    /** Per-event notification configuration, stored independently per event. */
    val notifications: NotificationSettings = NotificationSettings()
)
