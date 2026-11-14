package com.mawaqiti.plus.notify

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Shared AppWidget entry point for both Nidaa widgets (beta 7.9).
 *
 * Every provider only decides *when* to refresh; the actual work is delegated
 * to [WidgetUpdater.updateAll], which refreshes the placed instances of BOTH
 * widgets in one pass. That keeps a single update pipeline for every refresh
 * point: system update period, widget added / resized, boot, settings changes
 * and every prayer boundary.
 *
 * beta6 / W5 hardening, kept here so no provider can bypass it:
 *  - the broadcast is handled with [goAsync] on a coroutine, so the DataStore
 *    read and the RemoteViews build never run on the main thread;
 *  - the [android.content.BroadcastReceiver.PendingResult] is finished in
 *    `finally`;
 *  - nothing is allowed to throw out of a broadcast: a failure records the
 *    stack trace and the updater pushes the guaranteed-valid fallback layout.
 */
abstract class SafeWidgetProvider : AppWidgetProvider() {

    /** Which layout family this provider owns; used by the shared renderer. */
    open val kind: WidgetUpdater.WidgetKind
        get() = WidgetUpdater.WidgetKind.MAIN

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        // Resize end: re-render THIS instance with the FINAL options the
        // launcher just reported. Intermediate sizes are never kept.
        refreshOne(context, appWidgetId, WidgetUpdater.WidgetSize.fromOptions(newOptions))
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
                WidgetUpdater.updateAll(app)
            } catch (error: Throwable) {
                WidgetCrashLog.record(app, ANY_WIDGET, error)
            } finally {
                pending?.finish()
            }
        }
    }

    private fun refreshOne(
        context: Context,
        widgetId: Int,
        size: WidgetUpdater.WidgetSize
    ) {
        val app = context.applicationContext
        val pending = goAsync()
        val providerKind = kind
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WidgetUpdater.renderOne(app, widgetId, size, providerKind)
            } catch (error: Throwable) {
                WidgetCrashLog.record(app, widgetId, error)
            } finally {
                pending?.finish()
            }
        }
    }

    private companion object {
        const val ANY_WIDGET = -1
    }
}
