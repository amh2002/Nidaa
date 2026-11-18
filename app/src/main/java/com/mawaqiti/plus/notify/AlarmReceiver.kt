package com.mawaqiti.plus.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mawaqiti.plus.data.CityRepository
import com.mawaqiti.plus.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives prayer and midnight alarms.
 *
 * The event's enabled flag and mode are read from DataStore here, at fire
 * time. Changing a mode after an alarm was scheduled can therefore never
 * misfire: the already-scheduled alarm simply follows the current setting.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationScheduler.ACTION_FIRE -> handleFire(app, intent)
                    NotificationScheduler.ACTION_MIDNIGHT -> Unit
                }
                runCatching { NotificationScheduler.scheduleAll(app) }
                WidgetUpdater.updateAll(app)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleFire(context: Context, intent: Intent) {
        val settings = SettingsRepository(context).settings.first()
        val event = NotifyEvent.fromKey(intent.getStringExtra(NotificationScheduler.EXTRA_EVENT))
            ?: return
        if (!NotificationRouting.shouldDeliver(settings.notifications, event)) return

        val city = CityRepository(context).byOrder(settings.cityOrder)
        val minutes = intent.getIntExtra(NotificationScheduler.EXTRA_MINUTES, 0)
        NotificationDispatcher.deliver(
            context = context,
            event = event,
            mode = settings.notifications.forEvent(event).mode,
            city = city,
            minutes = minutes,
            clockFormat = settings.clockFormat,
            language = settings.language,
            adhanSound = AdhanSoundProvider.resolve(settings),
            adhanVolume = settings.adhanVolume
        )
    }
}

/** Reschedules everything after boot, clock changes and app updates. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.scheduleAllAsync(context)
    }
}
