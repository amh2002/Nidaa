package com.nidaa.prayer.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.mawaqiti.plus.notify.NotificationText

/**
 * Exactly one local notification per release: «تحديث جديد متاح لنداء».
 * The «جاهز للتثبيت» notification is posted when a verified download finishes
 * (background completion never installs by itself).
 *
 * Both notifications deep-link to the update UI through an explicit intent and
 * IMMUTABLE PendingIntent; the banner uses the same target.
 */
object UpdateNotifier {

    const val CHANNEL_ID = "updates_v1"
    const val NOTIFICATION_ID = 7301
    const val READY_NOTIFICATION_ID = 7303
    private const val REQUEST_OPEN = 7302
    private const val REQUEST_READY = 7304

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.update_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.update_channel_desc)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun notify(context: Context, release: ReleaseInfo, language: AppLanguage) {
        val localized = notificationsAllowed(context, language) ?: return
        val body = localized.getString(R.string.update_notification_body, release.tag)
        publish(
            context = context,
            requestCode = REQUEST_OPEN,
            notificationId = NOTIFICATION_ID,
            title = localized.getString(R.string.update_notification_title),
            body = body
        )
    }

    fun notifyReady(context: Context, release: ReleaseInfo, language: AppLanguage) {
        val localized = notificationsAllowed(context, language) ?: return
        val body = localized.getString(R.string.update_ready_notification_body, release.tag)
        publish(
            context = context,
            requestCode = REQUEST_READY,
            notificationId = READY_NOTIFICATION_ID,
            title = localized.getString(R.string.update_ready_notification_title),
            body = body
        )
    }

    fun cancelReady(context: Context) {
        NotificationManagerCompat.from(context).cancel(READY_NOTIFICATION_ID)
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(READY_NOTIFICATION_ID)
    }

    /** Explicit deep link into the screen that hosts «التحديثات». */
    internal fun openAppIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_UPDATES, true)
        }

    /** IMMUTABLE + UPDATE_CURRENT, mandatory for API 23+. */
    internal fun contentPendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun publish(
        context: Context,
        requestCode: Int,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val openApp = PendingIntent.getActivity(
            context,
            requestCode,
            openAppIntent(context),
            contentPendingFlags()
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun notificationsAllowed(
        context: Context,
        language: AppLanguage
    ): android.content.Context? {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return null
        return NotificationText.localizedContext(context, language)
    }
}
