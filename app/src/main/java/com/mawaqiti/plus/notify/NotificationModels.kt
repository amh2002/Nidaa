package com.mawaqiti.plus.notify

import com.mawaqiti.plus.engine.table.TableEvent

/**
 * Every event the user can be notified about.
 *
 * [requestCodeBase] is fixed per event so the alarm identity never depends on
 * the occurrence date. Distinct occurrences share the code but use different
 * intent data URIs, which is what AlarmManager uses to keep them apart.
 */
enum class NotifyEvent(val storageKey: String, val requestCodeBase: Int) {
    FAJR("fajr", 4100),
    SUNRISE("sunrise", 4101),
    DHUHR("dhuhr", 4102),
    ASR("asr", 4103),
    MAGHRIB("maghrib", 4104),
    ISHA("isha", 4105),
    LAST_THIRD("last_third", 4106);

    /** The matching table event, or null for the derived last third. */
    val tableEvent: TableEvent?
        get() = when (this) {
            FAJR -> TableEvent.FAJR
            SUNRISE -> TableEvent.SUNRISE
            DHUHR -> TableEvent.DHUHR
            ASR -> TableEvent.ASR
            MAGHRIB -> TableEvent.MAGHRIB
            ISHA -> TableEvent.ISHA
            LAST_THIRD -> null
        }

    companion object {
        fun fromKey(key: String?): NotifyEvent? =
            entries.firstOrNull { it.storageKey == key }
    }
}

/** How a notification is delivered, independent of whether it is enabled. */
enum class NotifyMode(val storageKey: String) {
    ADHAN("adhan"),
    SILENT("silent"),
    VIBRATE("vibrate");

    companion object {
        fun fromKey(key: String?): NotifyMode =
            entries.firstOrNull { it.storageKey == key } ?: SILENT
    }
}

data class EventNotification(
    val enabled: Boolean,
    val mode: NotifyMode
)

/**
 * All notification state. Defaults are the beta4 specification:
 * Fajr/Dhuhr/Asr/Maghrib/Isha = ON + Adhan, Sunrise/Last third = OFF + Silent.
 */
data class NotificationSettings(
    val masterEnabled: Boolean = true,
    val events: Map<NotifyEvent, EventNotification> = DEFAULT_EVENTS,
    /** Persisted content URI of a user-picked Adhan file; null = bundled chime. */
    val adhanUri: String? = null
) {
    fun forEvent(event: NotifyEvent): EventNotification =
        events[event] ?: DEFAULT_EVENTS.getValue(event)

    fun isEnabled(event: NotifyEvent): Boolean =
        masterEnabled && forEvent(event).enabled

    companion object {
        val DEFAULT_EVENTS: Map<NotifyEvent, EventNotification> = mapOf(
            NotifyEvent.FAJR to EventNotification(true, NotifyMode.ADHAN),
            NotifyEvent.SUNRISE to EventNotification(false, NotifyMode.SILENT),
            NotifyEvent.DHUHR to EventNotification(true, NotifyMode.ADHAN),
            NotifyEvent.ASR to EventNotification(true, NotifyMode.ADHAN),
            NotifyEvent.MAGHRIB to EventNotification(true, NotifyMode.ADHAN),
            NotifyEvent.ISHA to EventNotification(true, NotifyMode.ADHAN),
            NotifyEvent.LAST_THIRD to EventNotification(false, NotifyMode.SILENT)
        )
    }
}

/** One planned alarm: which event, when, and the fixed request code. */
data class PlannedAlarm(
    val event: NotifyEvent,
    val fireAt: java.time.ZonedDateTime,
    val requestCode: Int
)
