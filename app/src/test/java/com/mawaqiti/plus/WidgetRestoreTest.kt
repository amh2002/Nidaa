package com.mawaqiti.plus

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.notify.MawaqitiWidgetProvider
import com.mawaqiti.plus.notify.NextPrayerWidgetProvider
import com.mawaqiti.plus.notify.WidgetUpdater
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * beta 7.95 regression guard for defect A: a main widget resized to the
 * smallest size and then back to the default size must render EXACTLY what a
 * fresh add at the default size renders.
 *
 * Why it used to fail:
 *  - `minResizeHeight` was 180dp while the default (fresh-add) height is
 *    110dp, so the launcher could never return an instance to the default
 *    height at all;
 *  - `onAppWidgetOptionsChanged` re-rendered every instance from the manager's
 *    cached options instead of the final options the launcher had just sent,
 *    so the compact variant could survive a restore.
 *
 * The fix makes `WidgetSize.fromOptions` + `WidgetUpdater.mainLayoutFor` the one
 * size -> layout rule, and the resize callback renders the FINAL size through
 * `WidgetUpdater.renderOne`. This test drives those exact entry points and
 * compares the resulting view trees.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-xhdpi")
class WidgetRestoreTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val manager: AppWidgetManager
        get() = AppWidgetManager.getInstance(context)

    private fun options(widthDp: Int, heightDp: Int): Bundle = Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
    }

    private fun createWidget(): Int =
        Shadows.shadowOf(manager).createWidget(
            MawaqitiWidgetProvider::class.java,
            R.layout.widget_mawaqiti_ar
        )

    private fun render(widgetId: Int, widthDp: Int, heightDp: Int) = runBlocking {
        WidgetUpdater.renderOne(
            context,
            widgetId,
            WidgetUpdater.WidgetSize.fromOptions(options(widthDp, heightDp)),
            WidgetUpdater.WidgetKind.MAIN
        )
    }

    private fun viewOf(widgetId: Int): View {
        val view = Shadows.shadowOf(manager).getViewFor(widgetId)
        assertNotNull("widget $widgetId was never rendered", view)
        return view!!
    }

    /** Stable structural fingerprint: ids, classes and digit-normalized text. */
    private fun signature(view: View): String = buildString { appendView(view, this) }

    private fun appendView(view: View, builder: StringBuilder) {
        builder.append(view.javaClass.simpleName).append('#').append(view.id)
        if (view is TextView) {
            builder.append('=').append(view.text.replace(Regex("\\d"), "#"))
        }
        builder.append('{')
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) appendView(view.getChildAt(index), builder)
        }
        builder.append('}')
    }

    private fun hasId(view: View, id: Int): Boolean {
        if (view.id == id) return true
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                if (hasId(view.getChildAt(index), id)) return true
            }
        }
        return false
    }

    // ------------------------------------------------------------------ sizes

    @Test
    fun `final launcher options map to the expected widget sizes`() {
        val min = WidgetUpdater.WidgetSize.fromOptions(options(180, 110))
        assertEquals(180, min.widthDp)
        assertEquals(110, min.heightDp)
        assertTrue("the minimum width must use the compact layout", min.isCompact)

        val default = WidgetUpdater.WidgetSize.fromOptions(options(250, 140))
        assertEquals(250, default.widthDp)
        assertFalse("the default 4x2 size must NOT use the compact layout", default.isCompact)

        val short = WidgetUpdater.WidgetSize.fromOptions(options(250, 110))
        assertTrue("the shortest height must use the compact layout", short.isCompact)

        val max = WidgetUpdater.WidgetSize.fromOptions(options(320, 320))
        assertEquals(320, max.widthDp)
        assertFalse(max.isCompact)

        // An empty bundle falls back to the declared default (4x2).
        assertFalse(WidgetUpdater.WidgetSize.fromOptions(Bundle()).isCompact)
    }

    @Test
    fun `layout selection is a pure function of the final size`() {
        val min = WidgetUpdater.WidgetSize.fromOptions(options(180, 110))
        val default = WidgetUpdater.WidgetSize.fromOptions(options(250, 140))
        val max = WidgetUpdater.WidgetSize.fromOptions(options(320, 320))

        assertEquals(
            R.layout.widget_mawaqiti_compact_ar,
            WidgetUpdater.mainLayoutFor(com.mawaqiti.plus.data.AppLanguage.ARABIC, min)
        )
        assertEquals(
            R.layout.widget_mawaqiti_ar,
            WidgetUpdater.mainLayoutFor(com.mawaqiti.plus.data.AppLanguage.ARABIC, default)
        )
        assertEquals(
            R.layout.widget_mawaqiti_ar,
            WidgetUpdater.mainLayoutFor(com.mawaqiti.plus.data.AppLanguage.ARABIC, max)
        )
        assertEquals(
            R.layout.widget_mawaqiti_compact_en,
            WidgetUpdater.mainLayoutFor(com.mawaqiti.plus.data.AppLanguage.ENGLISH, min)
        )
        assertEquals(
            R.layout.widget_mawaqiti_en,
            WidgetUpdater.mainLayoutFor(com.mawaqiti.plus.data.AppLanguage.ENGLISH, default)
        )
    }

    // ---------------------------------------------------------------- restore

    @Test
    fun `restoring the default size renders exactly what a fresh add renders`() {
        val fresh = createWidget()
        render(fresh, 250, 140)
        val freshSignature = signature(viewOf(fresh))

        val restored = createWidget()
        render(restored, 250, 140)   // fresh add at the default size
        render(restored, 180, 110)   // user drags to the smallest size
        render(restored, 250, 140)   // and back to the default size

        assertEquals(
            "a restored widget must be identical to a fresh add at the same size",
            freshSignature,
            signature(viewOf(restored))
        )
    }

    @Test
    fun `compact is used only while small and swapped out on restore`() {
        val id = createWidget()

        render(id, 180, 110)
        val compact = viewOf(id)
        assertTrue(
            "a narrow instance must render the compact layout",
            hasId(compact, R.id.widget_compact_root)
        )
        assertFalse(
            "the compact layout must not carry the six-prayer grid",
            hasId(compact, R.id.widget_fajr_time)
        )

        render(id, 320, 320)
        val large = viewOf(id)
        assertFalse(
            "the enlarged instance must leave the compact layout immediately",
            hasId(large, R.id.widget_compact_root)
        )
        assertTrue(
            "the enlarged instance must render the full grid",
            hasId(large, R.id.widget_fajr_time)
        )

        render(id, 180, 110)
        assertTrue(
            "shrinking again must go back to the compact layout",
            hasId(viewOf(id), R.id.widget_compact_root)
        )
    }

    @Test
    fun `every render carries real content in both variants`() {
        val id = createWidget()

        render(id, 180, 110)
        val compact = viewOf(id) as ViewGroup
        val compactTexts = (0 until compact.childCount)
            .map { compact.getChildAt(it) }
            .filterIsInstance<TextView>()
            .joinToString("|") { it.text.toString() }
        assertTrue("compact render must be filled with content", compactTexts.isNotBlank())
        assertNotEquals("--:--", compactTexts)

        render(id, 250, 140)
        val full = viewOf(id)
        val time = full.findViewById<TextView>(R.id.widget_dhuhr_time)
        assertNotNull("full render must fill every prayer time", time)
        assertTrue(time!!.text.toString().isNotBlank())
    }

    // --------------------------------------------------------------- ordering

    @Test
    fun `the resize callback renders the final options of its own provider`() {
        // Guard against a regression where the callback ignores newOptions and
        // asks for a full refresh of every instance instead.
        val source = java.io.File(
            "src/main/java/com/mawaqiti/plus/notify/SafeWidgetProvider.kt"
        ).readText()
        assertTrue(
            "onAppWidgetOptionsChanged must pass the final size to renderOne",
            source.contains("WidgetUpdater.renderOne")
        )
        assertTrue(
            "the final size must come from the launcher's new options",
            source.contains("WidgetSize.fromOptions(newOptions)")
        )
        val next = java.io.File(
            "src/main/java/com/mawaqiti/plus/notify/NextPrayerWidgetProvider.kt"
        ).readText()
        assertTrue(
            "the next-prayer provider must declare its own kind",
            next.contains("WidgetKind.NEXT")
        )
    }
}
