package com.mawaqiti.plus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.notify.NotificationChannels
import com.mawaqiti.plus.notify.NotificationRouting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta6 Item 2 guard: lock-screen visibility is PUBLIC on all three channels,
 * the silent channel is IMPORTANCE_DEFAULT, and the immutable beta5 channels are
 * replaced by versioned ids (and deleted) so installed devices actually change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationChannelsTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val manager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    private fun channel(id: String): NotificationChannel? =
        manager.getNotificationChannel(id)

    @Test
    fun `all channels are public on the lock screen`() {
        NotificationChannels.ensure(context)
        for (id in listOf(
            NotificationRouting.CHANNEL_ADHAN,
            NotificationRouting.CHANNEL_SILENT,
            NotificationRouting.CHANNEL_VIBRATE
        )) {
            val channel = channel(id)
            assertNotNull("missing channel $id", channel)
            assertEquals(
                "channel $id must show its content on the lock screen",
                Notification.VISIBILITY_PUBLIC,
                channel!!.lockscreenVisibility
            )
        }
    }

    @Test
    fun `silent channel is default importance so it is not hidden on the lock screen`() {
        NotificationChannels.ensure(context)
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            channel(NotificationRouting.CHANNEL_SILENT)!!.importance
        )
        // Still silent: no sound, no vibration.
        val silent = channel(NotificationRouting.CHANNEL_SILENT)!!
        assertNull(silent.sound)
        assertEquals(false, silent.shouldVibrate())
    }

    @Test
    fun `versioned ids replace the immutable beta5 channels`() {
        // Simulate the owner's device: beta5 channels already exist.
        manager.createNotificationChannel(
            NotificationChannel("silent", "Silent", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel("adhan", "Adhan", NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel("vibrate", "Vibrate", NotificationManager.IMPORTANCE_HIGH)
        )

        NotificationChannels.ensure(context)

        for (legacy in NotificationRouting.LEGACY_CHANNEL_IDS) {
            assertNull("legacy channel $legacy must be deleted", channel(legacy))
        }
        assertNotNull(channel(NotificationRouting.CHANNEL_SILENT))
        assertNotNull(channel(NotificationRouting.CHANNEL_ADHAN))
        assertNotNull(channel(NotificationRouting.CHANNEL_VIBRATE))
    }

    @Test
    fun `channel ids are stable and versioned`() {
        assertEquals("adhan_v2", NotificationRouting.CHANNEL_ADHAN)
        assertEquals("silent_v2", NotificationRouting.CHANNEL_SILENT)
        assertEquals("vibrate_v2", NotificationRouting.CHANNEL_VIBRATE)
    }
}
