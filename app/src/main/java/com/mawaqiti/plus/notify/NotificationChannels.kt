package com.mawaqiti.plus.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.mawaqiti.plus.R

/**
 * Creates exactly the three delivery channels the spec defines:
 * "adhan" (silent channel, the app plays the audio), "silent" (no sound, no
 * vibration) and "vibrate" (vibration only).
 */
object NotificationChannels {

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val adhan = NotificationChannel(
            NotificationRouting.CHANNEL_ADHAN,
            context.getString(R.string.notify_channel_adhan),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notify_channel_adhan_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(true)
        }

        val silent = NotificationChannel(
            NotificationRouting.CHANNEL_SILENT,
            context.getString(R.string.notify_channel_silent),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notify_channel_silent_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        val vibrate = NotificationChannel(
            NotificationRouting.CHANNEL_VIBRATE,
            context.getString(R.string.notify_channel_vibrate),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notify_channel_vibrate_desc)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 250, 700)
            setShowBadge(true)
        }

        manager.createNotificationChannels(listOf(adhan, silent, vibrate))
    }
}
