package com.mawaqiti.plus.notify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
 *  - beta 7.9: resizable 3x2 .. 5x3 (weights + autosizing); the same data set
 *    fills the layout at any allowed size.
 *  - beta 7.9: [updateAll] fills the main widget AND the compact next-prayer
 *    widget in the same pass, so every refresh point keeps both in sync. The
 *    next-prayer widget only shows name + time + a short "in Xh Ym" computed
 *    here; it never ticks and adds no alarm.
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

    /** Below this width (or height) the main widget uses its compact layout. */
    private const val COMPACT_WIDTH_DP = 220
    private const val COMPACT_HEIGHT_DP = 130

    enum class WidgetKind { MAIN, NEXT }

    /**
     * Final widget bounds as reported by the launcher. [widthDp]/[heightDp]
     * describe the current (minimum) window; the max values are only a
     * fallback when a launcher omits the minimum.
     */
    data class WidgetSize(
        val minWidthDp: Int = 0,
        val minHeightDp: Int = 0,
        val maxWidthDp: Int = 0,
        val maxHeightDp: Int = 0
    ) {
        val widthDp: Int get() = if (minWidthDp > 0) minWidthDp else maxWidthDp
        val heightDp: Int get() = if (minHeightDp > 0) minHeightDp else maxHeightDp
        val isCompact: Boolean
            get() = (widthDp > 0 && widthDp < COMPACT_WIDTH_DP) ||
                (heightDp > 0 && heightDp < COMPACT_HEIGHT_DP)

        companion object {
            val DEFAULT = WidgetSize(250, 140, 250, 140)

            fun fromOptions(options: Bundle?): WidgetSize {
                if (options == null || options.isEmpty) return DEFAULT
                fun read(key: String): Int = options.getInt(key, 0)
                val size = WidgetSize(
                    minWidthDp = read(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                    minHeightDp = read(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                    maxWidthDp = read(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                    maxHeightDp = read(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                )
                return if (size.widthDp <= 0 || size.heightDp <= 0) DEFAULT else size
            }
        }
    }

    fun updateAsync(context: Context) {
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateAll(app)
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

    /** Compact (narrow) layout for a widget language. */
    fun compactLayoutFor(language: AppLanguage): Int = when (language) {
        AppLanguage.ARABIC -> R.layout.widget_mawaqiti_compact_ar
        AppLanguage.ENGLISH -> R.layout.widget_mawaqiti_compact_en
    }

    /**
     * THE single layout-selection rule for the main widget.
     *
     * A fresh add, a scheduled update, a boot refresh and the end of a resize
     * all call this with the FINAL size, so one size always maps to one layout.
     * That is the restore guarantee: after shrinking to the minimum and growing
     * back to the default size, the instance renders exactly what a fresh add at
     * the default size renders.
     */
    fun mainLayoutFor(language: AppLanguage, size: WidgetSize): Int =
        if (size.isCompact) compactLayoutFor(language) else layoutFor(language)

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val mainIds = widgetIds(manager, mainComponent(context))
        val nextIds = widgetIds(manager, nextComponent(context))
        if (mainIds.isEmpty() && nextIds.isEmpty()) return

        // A settings/asset failure must not leave the ids untouched: fall back
        // for every widget instead of throwing out of the broadcast.
        val content = try {
            loadContent(context)
        } catch (error: Throwable) {
            WidgetCrashLog.record(context, ANY_WIDGET, error)
            null
        }

        for (id in mainIds) {
            renderForSize(context, manager, id, sizeOf(manager, id), content, WidgetKind.MAIN)
        }
        for (id in nextIds) {
            renderForSize(context, manager, id, sizeOf(manager, id), content, WidgetKind.NEXT)
        }
    }

    /**
     * Resize-end entry point. The provider passes the FINAL options bundle, so
     * the widget is rendered for exactly that size — no intermediate or stale
     * compact render can be left behind.
     */
    suspend fun renderOne(
        context: Context,
        widgetId: Int,
        size: WidgetSize,
        kind: WidgetKind
    ) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val content = try {
            loadContent(context)
        } catch (error: Throwable) {
            WidgetCrashLog.record(context, widgetId, error)
            null
        }
        renderForSize(context, manager, widgetId, size, content, kind)
    }

    private fun mainComponent(context: Context): ComponentName =
        ComponentName(context, MawaqitiWidgetProvider::class.java)

    private fun nextComponent(context: Context): ComponentName =
        ComponentName(context, NextPrayerWidgetProvider::class.java)

    private fun widgetIds(manager: AppWidgetManager, component: ComponentName): IntArray =
        runCatching { manager.getAppWidgetIds(component) }.getOrNull() ?: IntArray(0)

    private fun sizeOf(manager: AppWidgetManager, widgetId: Int): WidgetSize =
        runCatching { WidgetSize.fromOptions(manager.getAppWidgetOptions(widgetId)) }
            .getOrDefault(WidgetSize.DEFAULT)

    /**
     * THE only function that builds and pushes widget views. Every entry point
     * (add, update period, boot, prayer boundary, settings change, resize end,
     * fallback) ends here. RemoteViews are rebuilt from XML for every call, so
     * no size-dependent view tree is ever cached between renders.
     */
    private fun renderForSize(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        size: WidgetSize,
        content: WidgetContent?,
        kind: WidgetKind
    ) {
        try {
            if (content == null) {
                manager.updateAppWidget(widgetId, fallbackFor(context, kind, AppLanguage.ARABIC))
                return
            }
            val views = when (kind) {
                WidgetKind.MAIN -> mainViews(context, content, size)
                WidgetKind.NEXT -> nextViews(context, content)
            }
            // Reject a layout exactly the way the launcher would, here.
            preflight(context, views.layoutId)
            manager.updateAppWidget(widgetId, views)
        } catch (error: Throwable) {
            WidgetCrashLog.record(context, widgetId, error)
            runCatching {
                manager.updateAppWidget(
                    widgetId,
                    fallbackFor(
                        context,
                        kind,
                        content?.language ?: AppLanguage.ARABIC
                    )
                )
            }.onFailure { fallbackError ->
                WidgetCrashLog.record(context, widgetId, fallbackError)
            }
        }
    }

    private fun fallbackFor(
        context: Context,
        kind: WidgetKind,
        language: AppLanguage
    ): RemoteViews = when (kind) {
        WidgetKind.MAIN -> fallbackViews(context, language)
        WidgetKind.NEXT -> nextFallbackViews(context, language)
    }

    private fun pushFallbackToAll(context: Context) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context) ?: return
            for (id in widgetIds(manager, mainComponent(context))) {
                renderForSize(
                    context, manager, id, sizeOf(manager, id), null, WidgetKind.MAIN
                )
            }
            for (id in widgetIds(manager, nextComponent(context))) {
                renderForSize(
                    context, manager, id, sizeOf(manager, id), null, WidgetKind.NEXT
                )
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // content
    // ----------------------------------------------------------------------- //

    private data class WidgetContent(
        val language: AppLanguage,
        val texts: Map<Int, String>,
        val compactTexts: Map<Int, String>,
        val nextTexts: Map<Int, String>
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

        // Compact main widget (small instances): city + wordmark, next prayer
        // name/time large, and the remaining-time pill. Same next-prayer data
        // as the full widget, so growing back to the default size immediately
        // returns to the full grid.
        val compactTexts = linkedMapOf(
            R.id.widget_compact_city to city.displayName(isArabic),
            R.id.widget_compact_wordmark to localized.getString(R.string.brand_name),
            R.id.widget_compact_name to localized.getString(WidgetText.labelRes(nextEvent)),
            R.id.widget_compact_time to
                WidgetText.clock(nextMinutes, settings.clockFormat, isArabic),
            R.id.widget_compact_left to
                WidgetText.remaining(WidgetText.minutesUntil(nextMinutes, nowMinutes), isArabic)
        )

        // Compact next-prayer widget: name + time + a short "in Xh Ym" that is
        // computed now and only refreshed by the shared update pipeline.
        val nextTexts = linkedMapOf(
            R.id.widget_next_label to localized.getString(R.string.widget_next_caption),
            R.id.widget_next_name to localized.getString(WidgetText.labelRes(nextEvent)),
            R.id.widget_next_time to
                WidgetText.clock(nextMinutes, settings.clockFormat, isArabic),
            R.id.widget_next_left to
                WidgetText.remaining(WidgetText.minutesUntil(nextMinutes, nowMinutes), isArabic)
        )

        return WidgetContent(
            language = language,
            texts = texts,
            compactTexts = compactTexts,
            nextTexts = nextTexts
        )
    }

    // ----------------------------------------------------------------------- //
    // RemoteViews
    // ----------------------------------------------------------------------- //

    /**
     * Main widget views for the FINAL size. Rebuilt from XML on every call; the
     * compact layout is chosen only for narrow instances and is replaced by the
     * full layout as soon as the instance is wide again.
     */
    private fun mainViews(
        context: Context,
        content: WidgetContent,
        size: WidgetSize
    ): RemoteViews {
        val compact = size.isCompact
        val layoutId = mainLayoutFor(content.language, size)
        val texts = if (compact) content.compactTexts else content.texts
        val rootId = if (compact) R.id.widget_compact_root else R.id.widget_root
        val views = RemoteViews(context.packageName, layoutId)
        for ((viewId, text) in texts) {
            views.setTextViewText(viewId, text)
        }
        views.setOnClickPendingIntent(rootId, openApp(context))
        return views
    }

    /** Compact next-prayer widget; same click target, so tap opens the app. */
    private fun nextViews(context: Context, content: WidgetContent): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_next)
        for ((viewId, text) in content.nextTexts) {
            views.setTextViewText(viewId, text)
        }
        views.setOnClickPendingIntent(R.id.widget_next_root, openApp(context))
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

    /**
     * beta 7.9 fallback for the compact widget. Uses the same minimal, valid
     * RemoteViews tree as the full widget fallback (LinearLayout + TextView,
     * static drawable, system typeface) so it can never render "Problem loading
     * widget"; the remaining-time pill is left empty rather than showing stale
     * information.
     */
    private fun nextFallbackViews(context: Context, language: AppLanguage): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_next)
        val localized = runCatching {
            NotificationText.localizedContext(context, language)
        }.getOrNull()
        views.setTextViewText(
            R.id.widget_next_label,
            localized?.getString(R.string.widget_next_caption) ?: ""
        )
        views.setTextViewText(R.id.widget_next_name, "")
        views.setTextViewText(R.id.widget_next_time, fallbackNext(context, language))
        views.setTextViewText(R.id.widget_next_left, "")
        views.setOnClickPendingIntent(R.id.widget_next_root, openApp(context))
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
