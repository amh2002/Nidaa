package com.mawaqiti.plus

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.Space
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta6 / W5 regression guard.
 *
 * Beta5 shipped `<Space>` in both widget layouts to create the W1 gap. Space is
 * **not** annotated `android.widget.RemoteViews.RemoteView`, and the launcher
 * inflates widget layouts with `RemoteViews` as a `LayoutInflater.Filter`, which
 * accepts only `@RemoteView` classes. The host therefore threw
 * `InflateException: Class not allowed to be inflated android.widget.Space` and
 * rendered "Problem loading widget".
 *
 * These tests assert two things:
 *  1. the platform filter really rejects Space (the root cause), and
 *  2. every shipped widget layout passes that same filter and contains only
 *     whitelisted RemoteViews classes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetRemoteViewsTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val shippedLayouts = listOf(
        R.layout.widget_mawaqiti_ar,
        R.layout.widget_mawaqiti_en,
        R.layout.widget_mawaqiti_fallback
    )

    /** Exactly the platform `RemoteViews` acceptance rule. */
    private fun platformRule(layoutId: Int) = RemoteViews(context.packageName, layoutId)

    private val whitelist = setOf(
        LinearLayout::class.java.simpleName,
        "FrameLayout",
        "RelativeLayout",
        "GridLayout",
        TextView::class.java.simpleName,
        "ImageView",
        "Button",
        "ImageButton",
        "ProgressBar",
        "Chronometer",
        "TextClock",
        "AnalogClock",
        "ViewFlipper",
        "AdapterViewFlipper",
        "StackView",
        "GridView",
        "ListView"
    )

    @Test
    fun `space is rejected by the platform RemoteViews filter`() {
        val rule = platformRule(R.layout.widget_mawaqiti_ar)
        assertFalse(
            "Space is not @RemoteView, which is why beta5's widget failed",
            rule.onLoadClass(Space::class.java)
        )
        assertTrue(rule.onLoadClass(LinearLayout::class.java))
        assertTrue(rule.onLoadClass(TextView::class.java))
    }

    @Test
    fun `every shipped widget layout passes the launcher filter`() {
        for (layoutId in shippedLayouts) {
            val rule = platformRule(layoutId)
            val inflater = LayoutInflater.from(context).cloneInContext(context)
            inflater.setFilter { clazz -> rule.onLoadClass(clazz) }
            // Must not throw: this is exactly what the launcher does.
            inflater.inflate(layoutId, null)
        }
    }

    @Test
    fun `every shipped widget layout contains only whitelisted views`() {
        for (layoutId in shippedLayouts) {
            val root = LayoutInflater.from(context).inflate(layoutId, null)
            val pending = ArrayDeque<View>()
            pending.add(root)
            while (pending.isNotEmpty()) {
                val view = pending.removeLast()
                val name = view.javaClass.simpleName
                assertTrue(
                    "layout $layoutId uses non-RemoteViews class $name",
                    whitelist.contains(name)
                )
                if (view is ViewGroup) {
                    for (index in 0 until view.childCount) pending.add(view.getChildAt(index))
                }
            }
        }
    }
}
