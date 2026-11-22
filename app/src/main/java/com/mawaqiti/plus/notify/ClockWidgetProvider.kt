package com.mawaqiti.plus.notify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews
import com.mawaqiti.plus.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * beta27: the transparent clock widget ("الساعة").
 *
 * The time is a [android.widget.TextClock], so it refreshes itself once a minute
 * with no alarm, service or worker of any kind. The day name is a plain
 * TextView that this provider fills, because a TextClock formats with the
 * *system* locale and would print "Sunday" on an English phone; the widget
 * belongs to an Arabic app, so the name is always rendered through the Arabic
 * locale. It is refreshed by the system's date/time broadcasts (see the manifest
 * intent-filter), which fire at midnight and on manual clock changes - again no
 * polling and no scheduled alarm.
 *
 * The widget is deliberately independent of [WidgetUpdater]: it shares no
 * layout, no content and no refresh cycle with the prayer widgets, so placing or
 * resizing it cannot alter their updates.
 */
class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        // Resize does not change the composition (the views are weighted), but
        // re-rendering keeps the day text and the click target current.
        appWidgetManager.updateAppWidget(appWidgetId, buildViews(context))
    }

    override fun onEnabled(context: Context) {
        refreshAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> refreshAll(context)
            else -> Unit
        }
    }

    private fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(
            ComponentName(context, ClockWidgetProvider::class.java)
        )
        ids.forEach { id -> manager.updateAppWidget(id, buildViews(context)) }
    }

    private fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_clock)
        views.setTextViewText(R.id.clock_widget_day, currentDayName())
        views.setOnClickPendingIntent(R.id.clock_widget_root, launchPendingIntent(context))
        return views
    }

    /** The weekday in Arabic, e.g. «الأحد». */
    private fun currentDayName(): String = LocalDate.now()
        .dayOfWeek
        .getDisplayName(TextStyle.FULL, Locale.forLanguageTag(ARABIC_TAG))

    /**
     * Tapping anywhere on the widget opens the device Clock app.
     *
     * The standard [AlarmClock.ACTION_SHOW_ALARMS] intent is sent straight from
     * the launcher when a clock app resolves to it. When none does, the tap goes
     * to [ClockWidgetLaunchReceiver], which shows the Arabic explanation - the
     * only way to catch an `ActivityNotFoundException` that would otherwise be
     * swallowed by the launcher.
     */
    private fun launchPendingIntent(context: Context): PendingIntent {
        val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val hasClockApp = context.packageManager.resolveActivity(alarmIntent, 0) != null
        return if (hasClockApp) {
            PendingIntent.getActivity(context, 0, alarmIntent, flags)
        } else {
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ClockWidgetLaunchReceiver::class.java),
                flags
            )
        }
    }

    private companion object {
        const val ARABIC_TAG = "ar"
    }
}
