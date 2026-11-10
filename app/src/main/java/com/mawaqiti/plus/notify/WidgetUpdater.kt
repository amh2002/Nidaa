package com.mawaqiti.plus.notify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mawaqiti.plus.MainActivity
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.CityRepository
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.data.SettingsRepository
import com.mawaqiti.plus.engine.table.TableEngine
import com.mawaqiti.plus.engine.table.TableEvent
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fills every placed widget with the current city, settings and table day.
 *
 * RemoteViews was chosen over Glance on purpose: it needs no new dependency,
 * works on minSdk 24, and the 4x2 layout is simple enough that RemoteViews is
 * easier to keep pixel-stable. The widget never runs a per-second countdown.
 */
object WidgetUpdater {

    fun updateAsync(context: Context) {
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { update(app) }
        }
    }

    suspend fun update(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val component = ComponentName(context, MawaqitiWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val settings = SettingsRepository(context).settings.first()
        val city = CityRepository(context).byOrder(settings.cityOrder)
        val today = LocalDate.now()
        val day = TableEngine.day(
            profile = city.profile,
            minDiff = city.minDiff,
            date = today,
            summerTime = settings.summerTime && city.country == Country.PS
        )

        val isArabic = settings.language == AppLanguage.ARABIC
        val localized = NotificationText.localizedContext(context, settings.language)
        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        val (nextEvent, nextMinutes) = WidgetText.nextPrayer(day, nowMinutes)

        val views = RemoteViews(context.packageName, R.layout.widget_mawaqiti)
        views.setTextViewText(R.id.widget_city, city.displayName(isArabic))
        views.setTextViewText(R.id.widget_wordmark, localized.getString(R.string.app_name))
        views.setTextViewText(
            R.id.widget_date,
            WidgetText.gregorian(today, isArabic) + " · " + WidgetText.hijri(today, isArabic)
        )
        views.setTextViewText(
            R.id.widget_next,
            localized.getString(R.string.widget_next) + " · " +
                localized.getString(WidgetText.labelRes(nextEvent)) + " " +
                WidgetText.clock(nextMinutes, settings.clockFormat, isArabic)
        )

        for (event in WidgetText.allEvents()) {
            val nameId = nameId(event)
            val timeId = timeId(event)
            views.setTextViewText(nameId, localized.getString(WidgetText.labelRes(event)))
            views.setTextViewText(
                timeId,
                WidgetText.clock(day.minutesFor(event), settings.clockFormat, isArabic)
            )
        }

        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)

        for (id in ids) manager.updateAppWidget(id, views)
    }

    private fun nameId(event: TableEvent): Int = when (event) {
        TableEvent.FAJR -> R.id.widget_fajr_name
        TableEvent.SUNRISE -> R.id.widget_sunrise_name
        TableEvent.DHUHR -> R.id.widget_dhuhr_name
        TableEvent.ASR -> R.id.widget_asr_name
        TableEvent.MAGHRIB -> R.id.widget_maghrib_name
        TableEvent.ISHA -> R.id.widget_isha_name
    }

    private fun timeId(event: TableEvent): Int = when (event) {
        TableEvent.FAJR -> R.id.widget_fajr_time
        TableEvent.SUNRISE -> R.id.widget_sunrise_time
        TableEvent.DHUHR -> R.id.widget_dhuhr_time
        TableEvent.ASR -> R.id.widget_asr_time
        TableEvent.MAGHRIB -> R.id.widget_maghrib_time
        TableEvent.ISHA -> R.id.widget_isha_time
    }

    private const val REQUEST_OPEN = 6200
}
