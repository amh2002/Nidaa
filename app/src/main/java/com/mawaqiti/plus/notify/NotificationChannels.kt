package com.mawaqiti.plus.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.mawaqiti.plus.R

/**
 * Creates exactly the three delivery channels the spec defines:
 * "adhan" (silent channel, the app plays the audio), "silent" (no sound, no
 * vibration) and "vibrate" (vibration only).
 *
 * beta6 rules:
 *  - every channel is `VISIBILITY_PUBLIC`, so its notifications are visible on
 *    the lock screen;
 *  - the silent channel is `IMPORTANCE_DEFAULT` (not LOW/MIN) because Android
 *    hides low-importance notifications on the lock screen;
 *  - the ids carry a `_v2` suffix and the legacy ids are deleted once, because
 *    a channel's importance and lockscreen visibility are immutable after
 *    creation and the owner's device already has the beta4/beta5 channels.
 */
object NotificationChannels {

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Channels are immutable: delete the pre-beta6 ids so the new
        // visibility/importance actually applies on already-installed devices.
        for (legacyId in NotificationRouting.LEGACY_CHANNEL_IDS) {
            runCatching { manager.deleteNotificationChannel(legacyId) }
        }

        val adhan = channel(
            context = context,
            id = NotificationRouting.CHANNEL_ADHAN,
            nameRes = R.string.notify_channel_adhan,
            descriptionRes = R.string.notify_channel_adhan_desc,
            importance = NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(true)
        }

        val silent = channel(
            context = context,
            id = NotificationRouting.CHANNEL_SILENT,
            nameRes = R.string.notify_channel_silent,
            descriptionRes = R.string.notify_channel_silent_desc,
            importance = NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        val vibrate = channel(
            context = context,
            id = NotificationRouting.CHANNEL_VIBRATE,
            nameRes = R.string.notify_channel_vibrate,
            descriptionRes = R.string.notify_channel_vibrate_desc,
            importance = NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 250, 700)
            setShowBadge(true)
        }

        manager.createNotificationChannels(listOf(adhan, silent, vibrate))
    }

    private fun channel(
        context: Context,
        id: String,
        nameRes: Int,
        descriptionRes: Int,
        importance: Int
    ): NotificationChannel = NotificationChannel(
        id,
        context.getString(nameRes),
        importance
    ).apply {
        description = context.getString(descriptionRes)
        // beta6: visible on the lock screen on all three channels.
        setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
    }
}
