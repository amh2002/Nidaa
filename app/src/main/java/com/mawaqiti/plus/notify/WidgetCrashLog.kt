package com.mawaqiti.plus.notify

import android.content.Context
import android.os.Build
import android.util.Log
import com.mawaqiti.plus.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * beta6 / W5: every widget failure is recorded instead of being left to the
 * launcher's "Problem loading widget" placeholder.
 *
 * Each record contains the widget id, app version, API level, manufacturer,
 * model and the complete original stack trace. It is written twice:
 *
 *  1. `Log.e("MawaqitiWidget", …)` — visible in `adb logcat -s MawaqitiWidget`.
 *  2. `<external files dir>/widget-crash.log` — the on-device counterpart of
 *     `C:\rea_ws\report\log.md`. A phone cannot write to a Windows path, so the
 *     desktop report and this file carry the same records; pull it with
 *     `adb pull /sdcard/Android/data/com.nidaa.prayer/files/widget-crash.log`.
 */
object WidgetCrashLog {

    const val FILE_NAME = "widget-crash.log"
    private const val TAG = "MawaqitiWidget"
    private const val TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS Z"

    /** [widgetId] is -1 for a failure that is not tied to one instance. */
    fun record(context: Context, widgetId: Int, throwable: Throwable) {
        val stack = StringWriter().also { writer ->
            throwable.printStackTrace(PrintWriter(writer))
        }.toString()

        val header = buildString {
            append("----- ")
            append(SimpleDateFormat(TIME_PATTERN, Locale.US).format(Date()))
            append(" | widget=").append(widgetId)
            append(" | Nidaa ").append(BuildConfig.VERSION_NAME)
            append(" (").append(BuildConfig.VERSION_CODE).append(")")
            append(" | API ").append(Build.VERSION.SDK_INT)
            append(" | ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            append('\n')
            append(throwable.javaClass.name).append(": ").append(throwable.message).append('\n')
            append(stack)
            if (!stack.endsWith("\n")) append('\n')
        }

        Log.e(TAG, "widget $widgetId update failed; fallback pushed", throwable)

        runCatching {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            File(dir, FILE_NAME).appendText(header)
        }
    }
}
