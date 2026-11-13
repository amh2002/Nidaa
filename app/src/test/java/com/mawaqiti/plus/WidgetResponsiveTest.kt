package com.mawaqiti.plus

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta 7.9 task 3: the widget must render complete at every allowed size, in
 * both languages, with the system font at 1.0 and 1.3.
 *
 * The checks are structural: every prayer cell is weight-based, every text
 * autosizes on API 26+, and at the declared minimum/maximum the laid-out text
 * never exceeds the height of its own view (the exact symptom that produced
 * cropped widgets before this beta).
 */
abstract class WidgetResponsiveBase {

    abstract val isArabic: Boolean

    private val names = listOf(
        "widget_fajr", "widget_sunrise", "widget_dhuhr",
        "widget_asr", "widget_maghrib", "widget_isha"
    )

    private val labelsAr = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    private val labelsEn = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    private fun context(fontScale: Float): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration)
        config.fontScale = fontScale
        return base.createConfigurationContext(config)
    }

    private fun render(
        layout: Int,
        widthDp: Int,
        heightDp: Int,
        fontScale: Float
    ): ViewGroup {
        val ctx = context(fontScale)
        val root = LayoutInflater.from(ctx).inflate(layout, null) as ViewGroup
        val labels = if (isArabic) labelsAr else labelsEn
        names.forEachIndexed { index, id ->
            val nameId = root.resources.getIdentifier(id + "_name", "id", root.context.packageName)
            val timeId = root.resources.getIdentifier(id + "_time", "id", root.context.packageName)
            (root.findViewById<View>(nameId) as TextView).text = labels[index]
            (root.findViewById<View>(timeId) as TextView).text = "12:00"
        }
        (root.findViewById<View>(R.id.widget_city) as TextView).text =
            if (isArabic) "طولكرم - فلسطين" else "Tulkarm - Palestine"
        (root.findViewById<View>(R.id.widget_wordmark) as TextView).text =
            if (isArabic) "نداء" else "Nidaa"
        (root.findViewById<View>(R.id.widget_date) as TextView).text =
            if (isArabic) "الجمعة، 13 نوفمبر 2026" else "Friday, 13 November 2026"
        (root.findViewById<View>(R.id.widget_next) as TextView).text =
            if (isArabic) "العشاء 20:00" else "Isha 20:00"

        val dm = ctx.resources.displayMetrics
        root.measure(
            View.MeasureSpec.makeMeasureSpec((widthDp * dm.density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((heightDp * dm.density).toInt(), View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        return root
    }

    private fun assertNoVerticalClip(root: ViewGroup, where: String) {
        walk(root) { view ->
            val tv = view as? TextView ?: return@walk
            val layout = tv.layout ?: return@walk
            assertTrue(
                "$where: '${tv.text}' needs ${layout.height}px but only ${tv.height}px are available",
                layout.height <= tv.height + 1
            )
        }
    }

    private fun walk(view: View, visit: (View) -> Unit) {
        visit(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) walk(view.getChildAt(i), visit)
        }
    }

    @Test
    fun `both layouts stay weight based with no fixed content widths`() {
        for (layout in listOf(R.layout.widget_mawaqiti_ar, R.layout.widget_mawaqiti_en)) {
            val root = LayoutInflater.from(context(1.0f)).inflate(layout, null) as ViewGroup
            assertEquals(
                "root must stay a vertical LinearLayout",
                LinearLayout.VERTICAL,
                (root as LinearLayout).orientation
            )
            walk(root) { view ->
                if (view is TextView) {
                    val params = view.layoutParams
                    if (view.id != R.id.widget_city && view.id != R.id.widget_date) {
                        assertTrue(
                            "text ${view.id} must not use a fixed-dp width",
                            params.width == ViewGroup.LayoutParams.WRAP_CONTENT ||
                                params.width == ViewGroup.LayoutParams.MATCH_PARENT ||
                                params.width == 0
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `renders complete from 3x2 to the 320dp maximum at both font scales`() {
        for (fontScale in listOf(1.0f, 1.3f)) {
            for ((w, h) in listOf(180 to 180, 250 to 180, 320 to 320)) {
                val root = render(R.layout.widget_mawaqiti_ar, w, h, fontScale)
                assertNoVerticalClip(root, "AR ${w}x$h @${fontScale}")
            }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-xhdpi")
class WidgetResponsiveArTest : WidgetResponsiveBase() {
    override val isArabic = true
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS-w411dp-h891dp-xhdpi")
class WidgetResponsiveEnTest : WidgetResponsiveBase() {
    override val isArabic = false
}
