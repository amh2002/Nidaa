package com.mawaqiti.plus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.notify.WidgetUpdater
import java.io.File
import java.io.FileOutputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Deterministic widget renders for the beta 7.95 report.
 *
 * The layout is chosen by [WidgetUpdater.mainLayoutFor], i.e. the exact rule the
 * app uses at render time, so the PNGs prove what is actually shipped. Written
 * into `<reportDir>/widget_resize_beta795` with a single prefix constant.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetShotsTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val prefix = "after"

    private fun size(widthDp: Int, heightDp: Int) =
        WidgetUpdater.WidgetSize(widthDp, heightDp, widthDp, heightDp)

    private fun shotMain(widthDp: Int, heightDp: Int, name: String) {
        val layout = WidgetUpdater.mainLayoutFor(AppLanguage.ARABIC, size(widthDp, heightDp))
        val root = LayoutInflater.from(context).inflate(layout, null) as ViewGroup
        if (root.findViewById<View>(R.id.widget_compact_root) != null) {
            fillCompact(root)
        } else {
            fillMain(root)
        }
        val dm = context.resources.displayMetrics
        root.measure(
            View.MeasureSpec.makeMeasureSpec((widthDp * dm.density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((heightDp * dm.density).toInt(), View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        val bitmap = Bitmap.createBitmap(
            root.measuredWidth,
            root.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        root.draw(Canvas(bitmap))
        val dir = File(
            System.getProperty("mawaqiti.reportDir") ?: "../report",
            "widget_resize_beta795"
        ).apply { mkdirs() }
        FileOutputStream(File(dir, "${prefix}_$name.png")).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun shotNext(widthDp: Int, heightDp: Int, name: String) {
        val root = LayoutInflater.from(context).inflate(R.layout.widget_next, null) as ViewGroup
        fillNext(root)
        val dm = context.resources.displayMetrics
        root.measure(
            View.MeasureSpec.makeMeasureSpec((widthDp * dm.density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((heightDp * dm.density).toInt(), View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        val bitmap = Bitmap.createBitmap(
            root.measuredWidth,
            root.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        root.draw(Canvas(bitmap))
        val dir = File(
            System.getProperty("mawaqiti.reportDir") ?: "../report",
            "widget_resize_beta795"
        ).apply { mkdirs() }
        FileOutputStream(File(dir, "${prefix}_$name.png")).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun fillMain(root: ViewGroup) {
        fun tv(id: Int, text: String) {
            root.findViewById<View>(id)?.let { (it as TextView).text = text }
        }
        tv(R.id.widget_city, "طولكرم - فلسطين")
        tv(R.id.widget_wordmark, "نداء")
        tv(R.id.widget_date, "الجمعة، 13 نوفمبر 2026 · ٢ جمادى الآخرة ١٤٤٨")
        tv(R.id.widget_next, "العشاء 20:00")
        tv(R.id.widget_fajr_name, "الفجر")
        tv(R.id.widget_fajr_time, "05:00")
        tv(R.id.widget_sunrise_name, "الشروق")
        tv(R.id.widget_sunrise_time, "06:12")
        tv(R.id.widget_dhuhr_name, "الظهر")
        tv(R.id.widget_dhuhr_time, "11:34")
        tv(R.id.widget_asr_name, "العصر")
        tv(R.id.widget_asr_time, "14:41")
        tv(R.id.widget_maghrib_name, "المغرب")
        tv(R.id.widget_maghrib_time, "17:12")
        tv(R.id.widget_isha_name, "العشاء")
        tv(R.id.widget_isha_time, "18:38")
    }

    private fun fillCompact(root: ViewGroup) {
        fun tv(id: Int, text: String) {
            root.findViewById<View>(id)?.let { (it as TextView).text = text }
        }
        tv(R.id.widget_compact_city, "طولكرم - فلسطين")
        tv(R.id.widget_compact_wordmark, "نداء")
        tv(R.id.widget_compact_name, "العشاء")
        tv(R.id.widget_compact_time, "20:00")
        tv(R.id.widget_compact_left, "بعد 2س 5د")
    }

    private fun fillNext(root: ViewGroup) {
        fun tv(id: Int, text: String) {
            root.findViewById<View>(id)?.let { (it as TextView).text = text }
        }
        tv(R.id.widget_next_label, "الصلاة القادمة")
        tv(R.id.widget_next_name, "العشاء")
        tv(R.id.widget_next_time, "18:38")
        tv(R.id.widget_next_left, "بعد 2س 5د")
    }

    @Test
    fun `capture widget renders`() {
        // Main widget, layout chosen exactly like the app does:
        //   default 4x2 (250x140) and the 320 maximum use the full grid;
        //   the shortest height (110dp) and the minimum width use compact.
        shotMain(250, 140, "main_250x140_default")
        shotMain(250, 180, "main_250x180")
        shotMain(320, 320, "main_320x320_max")
        shotMain(250, 110, "main_250x110_short")
        shotMain(180, 110, "main_180x110_min")
        shotMain(180, 180, "main_180x180_min_tall")

        // Next-prayer widget: intentional at min, at the old 220 max and at the
        // new 250x140 maximum.
        shotNext(110, 110, "next_110x110_min")
        shotNext(220, 220, "next_220x220_oldmax")
        shotNext(250, 140, "next_250x140_newmax")
    }
}
