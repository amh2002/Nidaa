package com.mawaqiti.plus.notify

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

/**
 * beta17: sound-only adhan preview for the Settings screen.
 *
 * It is deliberately NOT a notification: no NotificationChannel, no
 * NotificationManager, no vibration. It opens the packaged track through
 * [AdhanSoundProvider] - the same registry entry and the same
 * `USAGE_ALARM`/`CONTENT_TYPE_MUSIC` attributes the prayer playback uses - and
 * applies the persisted volume scalar with `MediaPlayer.setVolume`, so the
 * button audibly proves exactly what the next prayer will sound like.
 *
 * A single instance is kept: tapping again (or leaving Settings) stops the
 * current preview instead of stacking players.
 */
object AdhanPreview {

    private const val TAG = "AdhanPreview"

    private var player: MediaPlayer? = null

    /**
     * True while a preview track is actually playing.
     *
     * `MediaPlayer.isPlaying` throws after release, so it is guarded; the
     * receiver is named explicitly because an unqualified `isPlaying` here
     * would resolve to this very property and recurse.
     */
    val isPlaying: Boolean
        get() = player?.let { mp -> runCatching { mp.isPlaying }.getOrDefault(false) } == true

    /**
     * Starts (or restarts) the preview at [volumePercent]. [onFinished] runs
     * when the track ends or fails, so the caller can reset its UI state.
     */
    fun play(
        context: Context,
        sound: AdhanSound,
        volumePercent: Int,
        onFinished: () -> Unit = {}
    ) {
        stop()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val gain = AdhanVolume.gain(volumePercent)
        val mp = MediaPlayer()
        player = mp
        Log.d(TAG, "preview ${sound.rawName} at ${AdhanVolume.normalize(volumePercent)}% (gain $gain)")

        mp.setAudioAttributes(attributes)
        try {
            val descriptor = AdhanSoundProvider.openDescriptor(context, sound)
                ?: throw IllegalStateException("${sound.rawName} is missing")
            descriptor.use {
                mp.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            mp.prepare()
            mp.setVolume(gain, gain)
            mp.setOnCompletionListener {
                stop()
                onFinished()
            }
            mp.start()
        } catch (error: Exception) {
            Log.w(TAG, "preview failed for ${sound.rawName}", error)
            stop()
            onFinished()
        }
    }

    /** Stops and releases the current preview, if any. */
    fun stop() {
        val current = player ?: return
        player = null
        runCatching { if (current.isPlaying) current.stop() }
        runCatching { current.release() }
    }
}
