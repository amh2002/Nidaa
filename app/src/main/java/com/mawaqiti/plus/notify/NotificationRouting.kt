package com.mawaqiti.plus.notify

/**
 * Pure routing rules. The channel is chosen from the event's OWN mode, never
 * from a global mode, which is what lets Fajr be Adhan, Asr silent and Isha
 * vibrate at the same time.
 *
 * beta6: the channel ids are versioned (`_v2`). Android notification channels
 * are immutable after creation: importance and lockscreen visibility cannot be
 * changed by re-creating a channel with the same id. The owner's device already
 * has the beta4/beta5 `adhan` / `silent` / `vibrate` channels, so the app moves
 * to new ids and deletes the old ones once — see [LEGACY_CHANNEL_IDS].
 */
object NotificationRouting {

    const val CHANNEL_ADHAN = "adhan_v2"
    const val CHANNEL_SILENT = "silent_v2"
    const val CHANNEL_VIBRATE = "vibrate_v2"

    /**
     * Ids created by beta4 and beta5. They are deleted at startup so the user
     * does not end up with two "Adhan" entries (one of them unable to show on
     * the lock screen) in the system notification settings.
     */
    val LEGACY_CHANNEL_IDS = listOf("adhan", "silent", "vibrate")

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
