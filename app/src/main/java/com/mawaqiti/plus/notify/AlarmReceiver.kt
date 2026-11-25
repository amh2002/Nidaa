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
                    NotificationScheduler.ACTION_PRE_ALERT -> handlePreAlert(app, intent)
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
        // beta35 (BUG-1): one delivery per occurrence, checked and set atomically.
        // A clock rollback that re-arms this alarm makes the second fire land here
        // and drop silently instead of posting and playing the adhan again.
        if (!claimOccurrence(context, intent, event, NotificationLedger.Kind.FIRE)) return

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

    /**
     * beta22: the pre-adhan reminder. The lead time is re-read here, so turning
     * the setting off between scheduling and firing can never produce a stale
     * reminder, and a reminder is only delivered for a prayer whose own
     * notification is still enabled.
     */
    private suspend fun handlePreAlert(context: Context, intent: Intent) {
        val settings = SettingsRepository(context).settings.first()
        val minutesBefore = settings.preAlertMinutes
        if (minutesBefore <= 0) return

        val event = NotifyEvent.fromKey(intent.getStringExtra(NotificationScheduler.EXTRA_EVENT))
            ?: return
        if (event !in NotifyEvent.PRE_ALERT_EVENTS) return
        if (!NotificationRouting.shouldDeliver(settings.notifications, event)) return
        // beta35 (BUG-1): same atomic check-and-set for the reminder.
        if (!claimOccurrence(context, intent, event, NotificationLedger.Kind.PRE_ALERT)) return

        NotificationDispatcher.deliverPreAlert(
            context = context,
            event = event,
            minutesBefore = minutesBefore,
            language = settings.language
        )
    }

    /**
     * beta35: atomically claims this occurrence in [NotificationLedger].
     *
     * Returns true the first time only. An older build (or an alarm armed before
     * this version) carries no epoch extra; such an alarm is left unledgered so
     * its delivery behaves exactly as it did in 2.2.0 rather than being dropped.
     */
    private fun claimOccurrence(
        context: Context,
        intent: Intent,
        event: NotifyEvent,
        kind: NotificationLedger.Kind
    ): Boolean {
        val epoch = intent.getLongExtra(NotificationScheduler.EXTRA_EPOCH, 0L)
        if (epoch <= 0L) return true
        return NotificationLedger.claim(
            context,
            NotificationLedger.key(event, epoch, kind)
        )
    }
}

/** Reschedules everything after boot, clock changes and app updates. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.scheduleAllAsync(context)
    }
}
