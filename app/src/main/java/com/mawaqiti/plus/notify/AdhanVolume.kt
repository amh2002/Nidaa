package com.mawaqiti.plus.notify

/**
 * beta17: the adhan loudness scalar.
 *
 * The prayer playback ([AdhanPlaybackService]) and the settings preview
 * ([AdhanPreview]) both render the packaged adhan through a `MediaPlayer` with
 * `USAGE_ALARM`, so a single linear gain value is enough to honour the user's
 * choice on every Android version. The user-facing value is a whole percentage
 * (0…100) stored in DataStore next to the sound choice.
 *
 * 100 % is the default and maps to gain 1.0, i.e. exactly the loudness the app
 * had before this setting existed - existing installs hear no change.
 */
object AdhanVolume {

    const val MIN = 0
    const val MAX = 100

    /** Default percentage: the pre-beta17 loudness. */
    const val DEFAULT = MAX

    /** Coerces a stored or incoming value into the supported percentage range. */
    fun normalize(percent: Int): Int = percent.coerceIn(MIN, MAX)

    /**
     * Linear amplitude scalar for `MediaPlayer.setVolume`, in `0f..1f`.
     * 0 % is silent, 100 % is the untouched track.
     */
    fun gain(percent: Int): Float = normalize(percent) / MAX.toFloat()
}
