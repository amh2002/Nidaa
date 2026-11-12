package com.mawaqiti.plus.notify

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AppWidget entry point. All real work is delegated to [WidgetUpdater] so the
 * receiver itself only decides *when* to refresh:
 *  - system update period (30 min safety net),
 *  - widget added / resized,
 *  - boot and settings changes (through the shared reschedule path),
 *  - every prayer boundary (the scheduling alarms call the same updater).
 *
 * beta6 / W5: the broadcast is handled with [goAsync] on a coroutine so the
 * DataStore read and the RemoteViews build never run on the main thread, and
 * the [PendingResult] is finished in `finally`. Nothing in this class or in
 * [WidgetUpdater] is allowed to throw out of a broadcast: a failure records the
 * stack trace and pushes the fallback layout instead.
 */
class MawaqitiWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        refresh(context)
    }

    override fun onEnabled(context: Context) {
        refresh(context)
    }

    /**
     * AppWidgetProvider dispatches APPWIDGET_UPDATE / APPWIDGET_DELETED /
     * APPWIDGET_ENABLED / … from here. Wrapping `super` keeps a malformed or
     * unexpected broadcast from crashing the process.
     */
    override fun onReceive(context: Context, intent: Intent) {
        try {
            super.onReceive(context, intent)
        } catch (error: Throwable) {
            WidgetCrashLog.record(context, ANY_WIDGET, error)
        }
    }

    private fun refresh(context: Context) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WidgetUpdater.update(app)
            } catch (error: Throwable) {
                WidgetCrashLog.record(app, ANY_WIDGET, error)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val ANY_WIDGET = -1
    }
}
