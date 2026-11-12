package com.mawaqiti.plus.notify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.RemoteViews
import com.mawaqiti.plus.MainActivity
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.CityRepository
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.data.SettingsRepository
import com.mawaqiti.plus.engine.table.TableEngine
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.format.ClockFormat
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
 * beta5 rules (kept unchanged):
 *  - The widget language comes from `widget.lang` (independent of the UI
 *    language); it selects one of two fully mirrored layouts, so the Arabic
 *    reading order is guaranteed on every launcher.
 *  - Row 1 renders the wordmark «نداء» and the city on opposite edges with a
 *    guaranteed 12 dp gap (a margin, not a <Space> view).
 *  - The size is fixed at 4x2 (`resizeMode="none"`).
 *
 * beta6 / W5 hardening:
 *  - The layout is pre-flighted with the platform's own RemoteViews filter, so
 *    a class the launcher would reject never reaches the host.
 *  - Every widget id is updated in its own try/catch. On any failure the
 *    minimal `widget_mawaqiti_fallback` layout (static background, city, next
 *    prayer time, system typeface) is pushed and the full stack trace is
 *    recorded by [WidgetCrashLog]. "Problem loading widget" is not an outcome
 *    this updater can produce.
 */
object WidgetUpdater {

    /** Not tied to one instance (settings read, id enumeration, …). */
    private const val ANY_WIDGET = -1
    private const val REQUEST_OPEN = 6200

    fun updateAsync(context: Context) {
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                update(app)
            } catch (error: Throwable) {
                WidgetCrashLog.record(app, ANY_WIDGET, error)
                pushFallbackToAll(app)
            }
        }
    }

    /** Layout id for a widget language. Pure, so the choice is unit-testable. */
    fun layoutFor(language: AppLanguage): Int = when (language) {
        AppLanguage.ARABIC -> R.layout.widget_mawaqiti_ar
        AppLanguage.ENGLISH -> R.layout.widget_mawaqiti_en
    }

    suspend fun update(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val component = ComponentName(context, MawaqitiWidgetProvider::class.java)
        val ids: IntArray = runCatching { manager.getAppWidgetIds(component) }
            .getOrNull()
            ?: return
        if (ids.isEmpty()) return

        // A settings/asset failure must not leave the ids untouched: fall back
        // for every widget instead of throwing out of the broadcast.
        val content = try {
            loadContent(context)
        } catch (error: Throwable) {
            WidgetCrashLog.record(context, ANY_WIDGET, error)
            null
        }

        for (id in ids) {
            updateOne(context, manager, id, content)
        }
    }

    private fun updateOne(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        content: WidgetContent?
    ) {
        try {
            if (content == null) {
                manager.updateAppWidget(widgetId, fallbackViews(context, AppLanguage.ARABIC))
                return
            }
            // Reject a layout exactly the way the launcher would, here.
            preflight(context, content.layoutId)
            manager.updateAppWidget(widgetId, fullViews(context, content))
        } catch (error: Throwable) {
            WidgetCrashLog.record(context, widgetId, error)
            runCatching {
                manager.updateAppWidget(
                    widgetId,
                    fallbackViews(
                        context,
                        content?.language ?: AppLanguage.ARABIC,
                        next = content?.nextLabel
                    )
                )
            }.onFailure { fallbackError ->
                WidgetCrashLog.record(context, widgetId, fallbackError)
            }
        }
    }

    private fun pushFallbackToAll(context: Context) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, MawaqitiWidgetProvider::class.java)
            for (id in manager.getAppWidgetIds(component)) {
                manager.updateAppWidget(id, fallbackViews(context, AppLanguage.ARABIC))
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // content
    // ----------------------------------------------------------------------- //

    private data class WidgetContent(
        val language: AppLanguage,
        val layoutId: Int,
        val texts: Map<Int, String>,
        val nextLabel: String
    )

    private suspend fun loadContent(context: Context): WidgetContent {
        val settings = SettingsRepository(context).settings.first()
        val city = CityRepository(context).byOrder(settings.cityOrder)
        val today = LocalDate.now()
        val day = TableEngine.day(
            profile = city.profile,
            minDiff = city.minDiff,
            date = today,
            summerTime = settings.summerTime && city.country == Country.PS
        )

        // Widget-only language: follows the UI language until the user picks one.
        val language = settings.effectiveWidgetLanguage
        val isArabic = language == AppLanguage.ARABIC
        val localized = NotificationText.localizedContext(context, language)
        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        val (nextEvent, nextMinutes) = WidgetText.nextPrayer(day, nowMinutes)

        val texts = LinkedHashMap<Int, String>()
        // The wordmark is always the short Arabic name, in both widget languages.
        texts[R.id.widget_wordmark] = localized.getString(R.string.brand_name)
        texts[R.id.widget_city] = city.displayName(isArabic)
        texts[R.id.widget_date] =
            WidgetText.gregorian(today, isArabic) + " · " + WidgetText.hijri(today, isArabic)
        texts[R.id.widget_next] =
            localized.getString(R.string.widget_next) + " · " +
                localized.getString(WidgetText.labelRes(nextEvent)) + " " +
                WidgetText.clock(nextMinutes, settings.clockFormat, isArabic)

        for (event in WidgetText.allEvents()) {
            texts[nameId(event)] = localized.getString(WidgetText.labelRes(event))
            texts[timeId(event)] =
                WidgetText.clock(day.minutesFor(event), settings.clockFormat, isArabic)
        }

        return WidgetContent(
            language = language,
            layoutId = layoutFor(language),
            texts = texts,
            nextLabel = localized.getString(WidgetText.labelRes(nextEvent)) + " " +
                WidgetText.clock(nextMinutes, settings.clockFormat, isArabic)
        )
    }

    // ----------------------------------------------------------------------- //
    // RemoteViews
    // ----------------------------------------------------------------------- //

    private fun fullViews(context: Context, content: WidgetContent): RemoteViews {
        val views = RemoteViews(context.packageName, content.layoutId)
        for ((viewId, text) in content.texts) {
            views.setTextViewText(viewId, text)
        }
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
        return views
    }

    /**
     * Minimal guaranteed-valid widget: static background drawable + city +
     * next-prayer time in the system typeface. Cannot reference a missing view
     * id, a font resource or a non-RemoteViews class.
     */
    private fun fallbackViews(
        context: Context,
        language: AppLanguage,
        next: String? = null
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_mawaqiti_fallback)
        val city = fallbackCity(context, language)
        val nextText = next ?: fallbackNext(context, language)
        views.setTextViewText(R.id.widget_fallback_city, city)
        views.setTextViewText(R.id.widget_fallback_next, nextText)
        views.setOnClickPendingIntent(R.id.widget_fallback_root, openApp(context))
        return views
    }

    private fun fallbackCity(context: Context, language: AppLanguage): String = runCatching {
        val isArabic = language == AppLanguage.ARABIC
        CityRepository(context).byOrder(City.DEFAULT_ORDER).displayName(isArabic)
    }.getOrElse { "" }

    private fun fallbackNext(context: Context, language: AppLanguage): String = runCatching {
        val isArabic = language == AppLanguage.ARABIC
        val localized = NotificationText.localizedContext(context, language)
        val city = CityRepository(context).byOrder(City.DEFAULT_ORDER)
        val day = TableEngine.day(
            profile = city.profile,
            minDiff = city.minDiff,
            date = LocalDate.now(),
            summerTime = false
        )
        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        val (event, minutes) = WidgetText.nextPrayer(day, nowMinutes)
        localized.getString(WidgetText.labelRes(event)) + " " +
            WidgetText.clock(minutes, ClockFormat.H24, isArabic)
    }.getOrElse { FALLBACK_TIME }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN,
        Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * Inflates the layout with the platform's own RemoteViews filter
     * (`RemoteViews` implements `LayoutInflater.Filter` and accepts only
     * `@RemoteView` classes). Beta5 shipped `<Space>` in both layouts; Space is
     * not `@RemoteView`, so the launcher threw
     * `InflateException: Class not allowed to be inflated android.widget.Space`
     * and rendered "Problem loading widget". This check turns that class of
     * defect into a caught exception before anything is pushed.
     */
    private fun preflight(context: Context, layoutId: Int) {
        val inflater = LayoutInflater.from(context).cloneInContext(context)
        val rule = RemoteViews(context.packageName, layoutId)
        inflater.setFilter { clazz -> rule.onLoadClass(clazz) }
        inflater.inflate(layoutId, null)
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

    private const val FALLBACK_TIME = "--:--"
}
