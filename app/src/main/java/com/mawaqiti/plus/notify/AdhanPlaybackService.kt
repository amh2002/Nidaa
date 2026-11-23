package com.mawaqiti.plus.notify

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
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
 *
 * beta14: the sound is the user's pick from the adhan registry. It arrives on
 * the start intent (resolved by [AdhanSoundProvider] at fire time) and is
 * opened through that same provider, so playback has exactly one source of
 * truth and the choice applies to the very next alarm on every Android version
 * without re-creating a channel.
 *
 * beta17: the loudness arrives the same way (resolved from the persisted
 * percentage) and is applied with `MediaPlayer.setVolume` on this player only.
 * The audio path is unchanged - MediaPlayer on `USAGE_ALARM` - so the volume
 * is a true amplitude scalar and the notification channels stay untouched.
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
                // beta30: the expanded text (city + time + the after-adhan
                // supplication). Absent means "same as the collapsed body".
                val bigBody = intent.getStringExtra(EXTRA_BIG_BODY).orEmpty().ifBlank { body }
                val stopLabel = intent.getStringExtra(EXTRA_STOP_LABEL)
                    ?: getString(R.string.notify_stop)
                val sound = AdhanSound.fromKey(intent.getStringExtra(EXTRA_ADHAN_KEY))
                val volume = intent.getIntExtra(EXTRA_ADHAN_VOLUME, AdhanVolume.DEFAULT)
                // Channels may have been removed (app data cleared / beta6
                // migration): make sure they exist before the foreground post.
                NotificationChannels.ensure(this)
                startForegroundCompat(id, buildNotification(id, title, body, bigBody, stopLabel))
                play(sound, volume)
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

    private fun buildNotification(
        id: Int,
        title: String,
        body: String,
        bigBody: String,
        stopLabel: String
    ): Notification {
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
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigBody))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, stopLabel, stop)
            .build()
    }

    /**
     * beta14: plays the selected registry entry through [AdhanSoundProvider].
     * The descriptor is the packaged file itself - no trimming, conversion or
     * copy - so the shipped bytes are exactly the owner's master file.
     *
     * beta17: [volumePercent] sets this player's linear gain; 100 % is 1.0f,
     * which is what every install used before the slider existed.
     */
    private fun play(sound: AdhanSound, volumePercent: Int) {
        stopPlayback()
        val gain = AdhanVolume.gain(volumePercent)
        Log.d(
            TAG,
            "playing ${sound.rawName} (res ${sound.rawRes}) at " +
                "${AdhanVolume.normalize(volumePercent)}% (gain $gain)"
        )
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        player = MediaPlayer().apply {
            setAudioAttributes(attributes)
            try {
                val descriptor = AdhanSoundProvider.openDescriptor(this@AdhanPlaybackService, sound)
                    ?: throw IllegalStateException("${sound.rawName} is missing")
                descriptor.use {
                    setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
                prepare()
                setVolume(gain, gain)
                start()
                setOnCompletionListener { stopEverything() }
            } catch (error: Exception) {
                // A decode failure must never leave a foreground service alive.
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
        private const val TAG = "AdhanPlaybackService"
        private const val ACTION_START = "com.nidaa.prayer.action.START_ADHAN"
        private const val ACTION_STOP = "com.nidaa.prayer.action.STOP_ADHAN"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BODY = "body"
        private const val EXTRA_BIG_BODY = "big_body"
        private const val EXTRA_STOP_LABEL = "stop_label"
        private const val EXTRA_ADHAN_KEY = "adhan_key"
        private const val EXTRA_ADHAN_VOLUME = "adhan_volume"
        private const val DEFAULT_ID = 7000
        private const val REQUEST_OPEN = 6100
        private const val REQUEST_STOP = 6101

        fun start(
            context: Context,
            notificationId: Int,
            title: String,
            body: String,
            /** beta30: the expanded body; defaults to [body]. */
            bigBody: String = body,
            stopLabel: String,
            sound: AdhanSound,
            /** beta17: persisted loudness percentage, passed through verbatim. */
            volumePercent: Int = AdhanVolume.DEFAULT
        ) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_BIG_BODY, bigBody)
                putExtra(EXTRA_STOP_LABEL, stopLabel)
                putExtra(EXTRA_ADHAN_KEY, sound.storageKey)
                putExtra(EXTRA_ADHAN_VOLUME, AdhanVolume.normalize(volumePercent))
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
