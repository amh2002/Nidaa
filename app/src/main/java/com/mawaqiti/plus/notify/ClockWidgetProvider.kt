package com.mawaqiti.plus.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.RemoteViews
import com.mawaqiti.plus.R
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The transparent clock widget ("الساعة"), drawn as a bitmap.
 *
 * beta27 used a TextClock plus a TextView, which cannot express the reference:
 * a vertical gradient on the digits, a calligraphic day name and real z-order
 * overlap. The whole composition is now rendered by [ClockWidgetRenderer] into
 * one bitmap, placed in a single ImageView (see res/layout/widget_clock.xml),
 * and re-rendered on every update, resize and time/date change.
 *
 * beta29: the render also carries the Hijri date, and the minute tick below is
 * what advances it. Every tick re-renders the bitmap from the current date, so
 * the Hijri line changes at local midnight without any separate midnight alarm -
 * and [Intent.ACTION_DATE_CHANGED] makes a manual date change immediate.
 *
 * Refreshing is explicit: a single chained minute alarm (setAndAllowWhileIdle,
 * no setExact and therefore no exact-alarm permission) re-arms itself after each
 * fire, which also covers the Hijri date's midnight roll-over. It exists only while at least one instance of this widget is placed -
 * onUpdate/onDeleted/onDisabled re-check the instance list and cancel it when it
 * is empty. System broadcasts (time/date/timezone/locale/boot) refresh the
 * bitmap immediately and re-align the chain, so a manual clock change never
 * waits for the next tick.
 *
 * The widget stays independent of [WidgetUpdater]: it shares no layout, content
 * or refresh cycle with the prayer widgets.
 */
class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id -> updateOne(context, appWidgetManager, id) }
        syncMinuteAlarm(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Resize changes the bitmap's aspect, so the composition is redrawn.
        updateOne(context, appWidgetManager, appWidgetId)
        syncMinuteAlarm(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // No instances left -> no per-minute wake-up.
        syncMinuteAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelMinuteAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TICK,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                refreshAll(context)
                syncMinuteAlarm(context)
            }
            else -> Unit
        }
    }

    private fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(
            ComponentName(context, ClockWidgetProvider::class.java)
        )
        ids.forEach { id -> updateOne(context, manager, id) }
    }

    private fun updateOne(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val size = widgetSizePx(context, manager, appWidgetId)
        val density = context.resources.displayMetrics.density
        val views = RemoteViews(context.packageName, R.layout.widget_clock)
        val bitmap = runCatching {
            ClockWidgetRenderer.render(context, size.first, size.second)
        }.getOrNull() ?: return
        views.setImageViewBitmap(R.id.clock_widget_image, bitmap)
        views.setOnClickPendingIntent(R.id.clock_widget_root, launchPendingIntent(context))
        manager.updateAppWidget(appWidgetId, views)
        // Release-build evidence (Log.* is stripped by R8): the bitmap must equal
        // the cell's pixel size, or the ImageView is upscaling it again.
        println("NidaaClock bitmap=${size.first}x${size.second} density=$density")
    }

    /**
     * The bitmap size for this instance, in pixels.
     *
     * The launcher reports the current cell footprint in dp (MIN_WIDTH/MIN_HEIGHT
     * for portrait); that is what the bitmap is drawn at, so the composition is
     * drawn at native resolution and the ImageView shows it 1:1. MAX_BITMAP_PIXELS
     * is only a Binder sanity net for absurd footprints, not a downscale budget.
     */
    private fun widgetSizePx(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int
    ): Pair<Int, Int> {
        val options = runCatching { manager.getAppWidgetOptions(appWidgetId) }.getOrNull()
        val metrics = context.resources.displayMetrics
        // The bundle carries two size pairs (portrait and landscape). A home-screen
        // widget is narrower and taller in portrait, so the portrait cell is
        // min(width) / max(height) across the pairs. Using the pair this way also
        // survives launchers that only refresh the options at placement time
        // (Launcher3 on API 24 keeps the 4x2 numbers after a resize to 4x3).
        val widths = listOf(
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0,
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0) ?: 0
        ).filter { it >= MIN_WIDTH_DP }
        val heights = listOf(
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0,
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0
        ).filter { it >= MIN_HEIGHT_DP }
        var widthDp = widths.minOrNull() ?: DEFAULT_WIDTH_DP
        var heightDp = heights.maxOrNull() ?: DEFAULT_HEIGHT_DP
        var widthPx = (widthDp * metrics.density).roundToInt()
        var heightPx = (heightDp * metrics.density).roundToInt()
        val pixels = widthPx.toLong() * heightPx.toLong()
        if (pixels > MAX_BITMAP_PIXELS) {
            val scale = sqrt(MAX_BITMAP_PIXELS.toDouble() / pixels.toDouble())
            widthPx = (widthPx * scale).roundToInt()
            heightPx = (heightPx * scale).roundToInt()
        }
        return max(1, widthPx) to max(1, heightPx)
    }

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

    // ---- per-minute refresh ------------------------------------------------

    /** Arms the next minute tick, or cancels the chain when no instance is placed. */
    private fun syncMinuteAlarm(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = runCatching {
            manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
        }.getOrNull()
        if (ids == null || ids.isEmpty()) {
            cancelMinuteAlarm(context)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        val nextMinute = now - now % MINUTE_MS + MINUTE_MS
        // setAndAllowWhileIdle, never setExact: no SCHEDULE_EXACT_ALARM needed and
        // the tick may drift by a few seconds under Doze, which a clock widget
        // tolerates; manual clock changes arrive as TIME_SET and refresh at once.
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMinute,
                tickPendingIntent(context)
            )
        }
    }

    private fun cancelMinuteAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        runCatching { alarmManager.cancel(tickPendingIntent(context)) }
    }

    private fun tickPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ClockWidgetProvider::class.java).setAction(ACTION_TICK)
        return PendingIntent.getBroadcast(
            context,
            TICK_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val ACTION_TICK = "com.mawaqiti.plus.action.CLOCK_WIDGET_TICK"
        const val TICK_REQUEST = 27
        const val MINUTE_MS = 60_000L
        const val DEFAULT_WIDTH_DP = 250
        const val DEFAULT_HEIGHT_DP = 110
        const val MIN_WIDTH_DP = 140
        const val MIN_HEIGHT_DP = 70
        const val MAX_BITMAP_PIXELS = 2_000_000L
    }
}
