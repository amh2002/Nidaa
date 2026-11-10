package com.mawaqiti.plus.notify

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

/**
 * AppWidget entry point. All real work is delegated to [WidgetUpdater] so the
 * receiver itself only decides *when* to refresh:
 *  - system update period (30 min safety net),
 *  - widget added / resized,
 *  - boot and settings changes (through the shared reschedule path),
 *  - every prayer boundary (the scheduling alarms call the same updater).
 */
class MawaqitiWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdater.updateAsync(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        WidgetUpdater.updateAsync(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdater.updateAsync(context)
    }
}
