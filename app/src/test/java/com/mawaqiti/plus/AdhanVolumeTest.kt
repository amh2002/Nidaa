package com.mawaqiti.plus

import com.mawaqiti.plus.notify.AdhanVolume
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * beta17: the plain arithmetic behind the adhan volume slider.
 *
 * The slider, the preview and the prayer playback all go through these two
 * functions, so this is the one place where the percentage -> gain contract can
 * break. 100 % must be exactly 1.0f: that is the loudness every install had
 * before the slider existed, and it is also the default value the repository
 * writes when the key is absent.
 */
class AdhanVolumeTest {

    @Test
    fun `default is the pre-beta17 loudness`() {
        assertEquals(100, AdhanVolume.DEFAULT)
        assertEquals(1.0f, AdhanVolume.gain(AdhanVolume.DEFAULT), 0f)
    }

    @Test
    fun `range is zero to one hundred`() {
        assertEquals(0, AdhanVolume.MIN)
        assertEquals(100, AdhanVolume.MAX)
    }

    @Test
    fun `gain is linear across the range`() {
        assertEquals(0.0f, AdhanVolume.gain(0), 0.0001f)
        assertEquals(0.25f, AdhanVolume.gain(25), 0.0001f)
        assertEquals(0.5f, AdhanVolume.gain(50), 0.0001f)
        assertEquals(0.75f, AdhanVolume.gain(75), 0.0001f)
        assertEquals(1.0f, AdhanVolume.gain(100), 0.0001f)
    }

    @Test
    fun `normalize clamps stored or incoming values`() {
        assertEquals(100, AdhanVolume.normalize(140))
        assertEquals(0, AdhanVolume.normalize(-20))
        assertEquals(37, AdhanVolume.normalize(37))
    }

    @Test
    fun `gain never leaves the media player range`() {
        listOf(-500, -1, 0, 1, 50, 99, 100, 101, 5000).forEach { percent ->
            val gain = AdhanVolume.gain(percent)
            assertEquals(
                "gain out of range for $percent%",
                true,
                gain in 0.0f..1.0f
            )
        }
    }
}
