package com.mawaqiti.plus.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.mawaqiti.plus.data.CityRepository
import com.mawaqiti.plus.data.SettingsRepository
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Alarm schedule owner.
 *
 * Every event gets a fixed request code and a unique data URI per occurrence,
 * so AlarmManager treats the ~14 alarms in the rolling 48-hour window as
 * distinct while the identity of an event never changes. Settings are read at
 * *schedule* time only for the table inputs (city/summer time); whether an
 * event actually notifies, and in which mode, is read again in
 * [AlarmReceiver] at fire time.
 */
object NotificationScheduler {

    const val ACTION_FIRE = "com.nidaa.prayer.action.FIRE_PRAYER"
    /** beta22: the silent vibration reminder that fires before the adhan. */
    const val ACTION_PRE_ALERT = "com.nidaa.prayer.action.PRE_ALERT"
    const val ACTION_MIDNIGHT = "com.nidaa.prayer.action.MIDNIGHT"

    const val EXTRA_EVENT = "event"
    const val EXTRA_MINUTES = "minutes"
    const val EXTRA_PRE_MINUTES = "pre_minutes"

    private const val EXTRA_EPOCH = "epoch"

    private const val PREFS = "mawaqiti_scheduler"
    private const val KEY_PLAN = "plan"
    /** beta22: pre-alerts are tracked separately so 0 can cancel every one. */
    private const val KEY_PRE_PLAN = "pre_plan"

    private const val MIDNIGHT_REQUEST_CODE = 4200
    /** beta22: keeps pre-alert request codes clear of events, midnight and open. */
    private const val PRE_ALERT_REQUEST_CODE_OFFSET = 200
    private const val WINDOW_LENGTH_MS = 15 * 60 * 1000L
    private const val MIDNIGHT_SLACK_SECONDS = 3L

    /** Serialises concurrent callers (Application + ViewModel + receiver). */
    private val planMutex = Mutex()

    fun scheduleAllAsync(context: Context) {
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { scheduleAll(app) }
        }
    }

    suspend fun scheduleAll(context: Context) = planMutex.withLock {
        scheduleAllLocked(context)
    }

    private suspend fun scheduleAllLocked(context: Context) {
        val settings = SettingsRepository(context).settings.first()
        val city = CityRepository(context).byOrder(settings.cityOrder)
        val source = TableDayTimesSource(city, settings.summerTime)
        val now = ZonedDateTime.now()
        val manager = context.getSystemService(AlarmManager::class.java) ?: return

        cancelPreviousPlan(context, manager)
        cancelPreviousPreAlerts(context, manager)

        val alarms = SchedulePlanner.plan(now, source)
        val scheduled = HashSet<String>()
        for (alarm in alarms) {
            val epoch = alarm.fireAt.toInstant().toEpochMilli()
            val minutes = alarm.fireAt.hour * 60 + alarm.fireAt.minute
            val pending = firePendingIntent(
                context = context,
                event = alarm.event,
                epoch = epoch,
                minutes = minutes
            )
            setExactOrWindow(manager, epoch, pending)
            scheduled += planKey(alarm.event, epoch)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PLAN, scheduled)
            .apply()

        // beta22: one pre-alert per upcoming prayer, from the same plan and
        // through the same exact/window mechanism. 0 plans nothing.
        schedulePreAlerts(context, manager, alarms, now, settings.preAlertMinutes)

        scheduleMidnight(context, manager, now)
        WidgetUpdater.updateAll(context)
    }

    /** True when exact alarms can currently be scheduled (always true below API 31). */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return true
        return manager.canScheduleExactAlarms()
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun setExactOrWindow(manager: AlarmManager, triggerAt: Long, pending: PendingIntent) {
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (exact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            manager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, WINDOW_LENGTH_MS, pending)
        }
    }

    private fun firePendingIntent(
        context: Context,
        event: NotifyEvent,
        epoch: Long,
        minutes: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("mawaqiti://event/${event.storageKey}/$epoch")
            putExtra(EXTRA_EVENT, event.storageKey)
            putExtra(EXTRA_MINUTES, minutes)
            putExtra(EXTRA_EPOCH, epoch)
        }
        return PendingIntent.getBroadcast(
            context,
            event.requestCodeBase,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelPreviousPlan(context: Context, manager: AlarmManager) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val plan = prefs.getStringSet(KEY_PLAN, emptySet()).orEmpty()
        for (entry in plan) {
            val parts = entry.split('|')
            if (parts.size != 2) continue
            val event = NotifyEvent.fromKey(parts[0]) ?: continue
            val epoch = parts[1].toLongOrNull() ?: continue
            manager.cancel(firePendingIntent(context, event, epoch, 0))
        }
        prefs.edit().remove(KEY_PLAN).apply()
    }

    /**
     * beta22: schedules this plan's pre-alerts. Any past reminder is skipped by
     * [PreAlertPlanner], so a disabled setting or a stale plan never leaves an
     * alarm behind - the caller has already cancelled the previous pre-plan.
     */
    private fun schedulePreAlerts(
        context: Context,
        manager: AlarmManager,
        alarms: List<PlannedAlarm>,
        now: ZonedDateTime,
        minutesBefore: Int
    ) {
        val scheduled = HashSet<String>()
        for (alert in PreAlertPlanner.plan(alarms, now, minutesBefore)) {
            val epoch = alert.fireAt.toInstant().toEpochMilli()
            setExactOrWindow(
                manager,
                epoch,
                preAlertPendingIntent(context, alert.event, epoch, minutesBefore)
            )
            scheduled += planKey(alert.event, epoch)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PRE_PLAN, scheduled)
            .apply()
    }

    private fun preAlertPendingIntent(
        context: Context,
        event: NotifyEvent,
        epoch: Long,
        minutesBefore: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PRE_ALERT
            data = Uri.parse("mawaqiti://prealert/${event.storageKey}/$epoch")
            putExtra(EXTRA_EVENT, event.storageKey)
            putExtra(EXTRA_PRE_MINUTES, minutesBefore)
        }
        return PendingIntent.getBroadcast(
            context,
            event.requestCodeBase + PRE_ALERT_REQUEST_CODE_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelPreviousPreAlerts(context: Context, manager: AlarmManager) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val plan = prefs.getStringSet(KEY_PRE_PLAN, emptySet()).orEmpty()
        for (entry in plan) {
            val parts = entry.split('|')
            if (parts.size != 2) continue
            val event = NotifyEvent.fromKey(parts[0]) ?: continue
            val epoch = parts[1].toLongOrNull() ?: continue
            manager.cancel(preAlertPendingIntent(context, event, epoch, 0))
        }
        prefs.edit().remove(KEY_PRE_PLAN).apply()
    }

    private fun scheduleMidnight(
        context: Context,
        manager: AlarmManager,
        now: ZonedDateTime
    ) {
        val nextMidnight = ZonedDateTime.of(
            now.toLocalDate().plusDays(1),
            LocalTime.MIDNIGHT,
            now.zone
        ).plusSeconds(MIDNIGHT_SLACK_SECONDS)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MIDNIGHT
            data = Uri.parse("mawaqiti://midnight/${nextMidnight.toLocalDate()}")
        }
        val pending = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactOrWindow(manager, nextMidnight.toInstant().toEpochMilli(), pending)
    }

    private fun planKey(event: NotifyEvent, epoch: Long): String =
        "${event.storageKey}|$epoch"
}
