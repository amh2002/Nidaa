package com.mawaqiti.plus

import com.mawaqiti.plus.notify.EventNotification
import com.mawaqiti.plus.notify.NotificationRouting
import com.mawaqiti.plus.notify.NotificationSettings
import com.mawaqiti.plus.notify.NotifyEvent
import com.mawaqiti.plus.notify.NotifyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Routing must follow each event's OWN mode. The mixed configuration is the
 * exact example from the beta4 brief: different modes active simultaneously.
 */
class NotificationRoutingTest {

    private fun mixedSettings() = NotificationSettings(
        masterEnabled = true,
        events = mapOf(
            NotifyEvent.FAJR to EventNotification(true, NotifyMode.ADHAN),
            NotifyEvent.SUNRISE to EventNotification(false, NotifyMode.SILENT),
            NotifyEvent.DHUHR to EventNotification(true, NotifyMode.SILENT),
            NotifyEvent.ASR to EventNotification(true, NotifyMode.VIBRATE),
            NotifyEvent.MAGHRIB to EventNotification(true, NotifyMode.ADHAN),
            NotifyEvent.ISHA to EventNotification(true, NotifyMode.VIBRATE),
            NotifyEvent.LAST_THIRD to EventNotification(false, NotifyMode.SILENT)
        )
    )

    @Test
    fun `mixed modes map to three distinct channels`() {
        val plan = NotificationRouting.deliveryPlan(mixedSettings())

        assertEquals("adhan", plan[NotifyEvent.FAJR])
        assertEquals("silent", plan[NotifyEvent.DHUHR])
        assertEquals("vibrate", plan[NotifyEvent.ASR])
        assertEquals("adhan", plan[NotifyEvent.MAGHRIB])
        assertEquals("vibrate", plan[NotifyEvent.ISHA])
        assertEquals("silent", plan[NotifyEvent.SUNRISE])
    }

    @Test
    fun `delivery follows the event own switch`() {
        val settings = mixedSettings()

        assertTrue(NotificationRouting.shouldDeliver(settings, NotifyEvent.FAJR))
        assertTrue(NotificationRouting.shouldDeliver(settings, NotifyEvent.DHUHR))
        assertTrue(NotificationRouting.shouldDeliver(settings, NotifyEvent.ASR))
        assertFalse(NotificationRouting.shouldDeliver(settings, NotifyEvent.SUNRISE))
        assertFalse(NotificationRouting.shouldDeliver(settings, NotifyEvent.LAST_THIRD))
    }

    @Test
    fun `master switch overrides every per-event switch`() {
        val settings = mixedSettings().copy(masterEnabled = false)

        assertTrue(NotifyEvent.entries.none { NotificationRouting.shouldDeliver(settings, it) })
    }

    @Test
    fun `defaults match the beta4 specification`() {
        val defaults = NotificationSettings()

        assertTrue(defaults.masterEnabled)
        assertEquals(NotifyMode.ADHAN, defaults.forEvent(NotifyEvent.FAJR).mode)
        assertTrue(defaults.forEvent(NotifyEvent.FAJR).enabled)
        assertFalse(defaults.forEvent(NotifyEvent.SUNRISE).enabled)
        assertEquals(NotifyMode.SILENT, defaults.forEvent(NotifyEvent.SUNRISE).mode)
        assertTrue(defaults.forEvent(NotifyEvent.DHUHR).enabled)
        assertTrue(defaults.forEvent(NotifyEvent.ASR).enabled)
        assertTrue(defaults.forEvent(NotifyEvent.MAGHRIB).enabled)
        assertTrue(defaults.forEvent(NotifyEvent.ISHA).enabled)
        assertFalse(defaults.forEvent(NotifyEvent.LAST_THIRD).enabled)
        assertEquals(NotifyMode.SILENT, defaults.forEvent(NotifyEvent.LAST_THIRD).mode)
    }
}
