package com.mawaqiti.plus.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mawaqiti.plus.MainActivity
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.TimeFormatter

/** Posts the right notification type for one event's own mode. */
object NotificationDispatcher {

    fun deliver(
        context: Context,
        event: NotifyEvent,
        mode: NotifyMode,
        city: City,
        minutes: Int,
        clockFormat: ClockFormat,
        language: AppLanguage,
        isTest: Boolean
    ) {
        val isArabic = language == AppLanguage.ARABIC
        val localized = NotificationText.localizedContext(context, language)
        val title = NotificationText.title(localized, event)
        val time = TimeFormatter.format(minutes, clockFormat, isArabic)
        val body = NotificationText.body(localized, city.displayName(isArabic), time)
        val notificationId = event.requestCodeBase + if (isTest) TEST_ID_OFFSET else 0

        if (mode == NotifyMode.ADHAN) {
            AdhanPlaybackService.start(
                context = context,
                notificationId = notificationId,
                title = title,
                body = body,
                stopLabel = localized.getString(R.string.notify_stop)
            )
        } else {
            postQuiet(context, notificationId, mode, title, body)
        }
    }

    private fun postQuiet(
        context: Context,
        notificationId: Int,
        mode: NotifyMode,
        title: String,
        body: String
    ) {
        if (!canPost(context)) return

        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(
            context,
            NotificationRouting.channelId(mode)
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(
                if (mode == NotifyMode.VIBRATE) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (mode == NotifyMode.SILENT) {
            builder.setSilent(true)
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    const val TEST_ID_OFFSET = 100
    private const val REQUEST_OPEN = 6000
}
