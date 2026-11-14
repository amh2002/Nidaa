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
 * Receives prayer, midnight and test alarms.
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
                    NotificationScheduler.ACTION_TEST -> handleTest(app, intent)
                    NotificationScheduler.ACTION_MIDNIGHT -> Unit
                }
                if (intent.action != NotificationScheduler.ACTION_TEST) {
                    runCatching { NotificationScheduler.scheduleAll(app) }
                }
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
            isTest = false
        )
    }

    private suspend fun handleTest(context: Context, intent: Intent) {
        val settings = SettingsRepository(context).settings.first()
        val event = NotifyEvent.fromKey(intent.getStringExtra(NotificationScheduler.EXTRA_EVENT))
            ?: return
        val mode = NotifyMode.fromKey(intent.getStringExtra(NotificationScheduler.EXTRA_MODE))
        val city = CityRepository(context).byOrder(settings.cityOrder)
        val minutes = intent.getIntExtra(NotificationScheduler.EXTRA_MINUTES, 0)
        val displayMinutes = if (minutes > 0) minutes else currentMinutes()
        NotificationDispatcher.deliver(
            context = context,
            event = event,
            mode = mode,
            city = city,
            minutes = displayMinutes,
            clockFormat = settings.clockFormat,
            language = settings.language,
            isTest = true
        )
    }

    private fun currentMinutes(): Int {
        val now = java.time.LocalTime.now()
        return now.hour * 60 + now.minute
    }
}

/** Reschedules everything after boot, clock changes and app updates. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.scheduleAllAsync(context)
    }
}
