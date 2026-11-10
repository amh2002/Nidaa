package com.mawaqiti.plus.notify

/**
 * Pure routing rules. The channel is chosen from the event's OWN mode, never
 * from a global mode, which is what lets Fajr be Adhan, Asr silent and Isha
 * vibrate at the same time.
 */
object NotificationRouting {

    const val CHANNEL_ADHAN = "adhan"
    const val CHANNEL_SILENT = "silent"
    const val CHANNEL_VIBRATE = "vibrate"

    fun channelId(mode: NotifyMode): String = when (mode) {
        NotifyMode.ADHAN -> CHANNEL_ADHAN
        NotifyMode.SILENT -> CHANNEL_SILENT
        NotifyMode.VIBRATE -> CHANNEL_VIBRATE
    }

    /** Channel per event, for documentation, diagnostics and tests. */
    fun deliveryPlan(settings: NotificationSettings): Map<NotifyEvent, String> =
        NotifyEvent.entries.associateWith { channelId(settings.forEvent(it).mode) }

    /** Whether a real (non-test) occurrence should produce anything at all. */
    fun shouldDeliver(settings: NotificationSettings, event: NotifyEvent): Boolean =
        settings.masterEnabled && settings.forEvent(event).enabled
}
