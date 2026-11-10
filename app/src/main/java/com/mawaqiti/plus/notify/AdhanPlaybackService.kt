package com.mawaqiti.plus.notify

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mawaqiti.plus.MainActivity
import com.mawaqiti.plus.R

/**
 * Plays the Adhan as a foreground service.
 *
 * The notification channel used for the foreground notification has sound
 * disabled; the audio itself is played by [MediaPlayer] with
 * [AudioAttributes.USAGE_ALARM] so it follows the alarm volume, exactly like a
 * system Adhan alarm. The "stop" action and the notification tap both return
 * to a clean state.
 */
class AdhanPlaybackService : Service() {

    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopEverything()
            ACTION_START -> {
                val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, DEFAULT_ID)
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
                startForegroundCompat(id, buildNotification(id, title, body))
                play(intent.getStringExtra(EXTRA_URI))
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun startForegroundCompat(id: Int, notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, id, notification, type)
    }

    private fun buildNotification(id: Int, title: String, body: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, AdhanPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotificationRouting.CHANNEL_ADHAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, getString(R.string.notify_stop), stop)
            .build()
    }

    private fun play(uriString: String?) {
        stopPlayback()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        player = MediaPlayer().apply {
            setAudioAttributes(attributes)
            try {
                if (!uriString.isNullOrBlank()) {
                    setDataSource(this@AdhanPlaybackService, Uri.parse(uriString))
                } else {
                    val descriptor = resources.openRawResourceFd(R.raw.placeholder_adhan)
                        ?: throw IllegalStateException("missing placeholder chime")
                    descriptor.use {
                        setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    }
                }
                prepare()
                start()
                setOnCompletionListener { stopEverything() }
            } catch (error: Exception) {
                stopEverything()
            }
        }
    }

    private fun stopEverything() {
        stopPlayback()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopPlayback() {
        player?.runCatching {
            if (isPlaying) stop()
            release()
        }
        player = null
    }

    companion object {
        private const val ACTION_START = "com.mawaqiti.plus.action.START_ADHAN"
        private const val ACTION_STOP = "com.mawaqiti.plus.action.STOP_ADHAN"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BODY = "body"
        private const val EXTRA_URI = "adhan_uri"
        private const val DEFAULT_ID = 7000
        private const val REQUEST_OPEN = 6100
        private const val REQUEST_STOP = 6101

        fun start(
            context: Context,
            notificationId: Int,
            title: String,
            body: String,
            adhanUri: String?
        ) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_URI, adhanUri)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
