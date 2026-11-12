package com.mawaqiti.plus

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import java.io.File

/**
 * Structural guard for the beta5 widget fixes W1/W2/W4.
 *
 * The two layouts are inflated with Robolectric and asserted by child index,
 * because reading order is a structural property: an Arabic widget is only
 * correct when الفجر is the LAST child of row A, i.e. rightmost in an LTR
 * container. Same for the wordmark/city anchors in row 1.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl")
class WidgetLayoutTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun inflate(layout: Int): ViewGroup =
        LayoutInflater.from(context).inflate(layout, null) as ViewGroup

    private fun row(root: ViewGroup, index: Int): ViewGroup = root.getChildAt(index) as ViewGroup

    private fun text(view: View): TextView = view as TextView

    /** Row 1 = wordmark/city, row 2 = date/pill, row A = 2, row B = 3. */
    private fun assertRowOne(layout: Int, wordmarkFirst: Boolean) {
        val root = inflate(layout)
        val row1 = row(root, 0)

        val wordmark = row1.findViewById<TextView>(R.id.widget_wordmark)
        val city = row1.findViewById<TextView>(R.id.widget_city)
        assertNotNull(wordmark)
        assertNotNull(city)

        // W1: the two texts are separate views anchored to opposite edges.
        val wordmarkIndex = row1.indexOfChild(wordmark)
        val cityIndex = row1.indexOfChild(city)
        assertTrue("wordmark and city must not be the same view", wordmarkIndex != cityIndex)
        assertTrue(
            "the weighted city must fill the space up to the heading",
            text(city).layoutParams.width == 0
        )

        if (wordmarkFirst) {
            assertEquals(0, wordmarkIndex)
            assertEquals(1, cityIndex)
        } else {
            assertEquals(0, cityIndex)
            assertEquals(1, wordmarkIndex)
        }

        assertEquals(
            "the city must absorb the free space so it can ellipsize",
            1.0f,
            (city.layoutParams as LinearLayout.LayoutParams).weight,
            0.001f
        )

        // beta6: the guaranteed 12 dp gap is a margin on the city, not a
        // spacer view — Space is not @RemoteView and broke RemoteViews inflation.
        assertEquals(
            "row 1 must contain exactly the wordmark and the city",
            2,
            row1.childCount
        )
        val params = city.layoutParams as LinearLayout.LayoutParams
        val gap = context.resources.getDimensionPixelSize(R.dimen.widget_row_gap)
        if (wordmarkFirst) {
            assertEquals("city must keep 12dp clear of the wordmark", gap, params.marginStart)
        } else {
            assertEquals("city must keep 12dp clear of the wordmark", gap, params.marginEnd)
        }

        // W1: the city can never wrap or paint over the wordmark.
        assertEquals(1, text(city).maxLines)
        assertEquals(TextUtils.TruncateAt.END, text(city).ellipsize)
        assertEquals(1, text(wordmark).maxLines)
    }

    private fun assertPrayerOrder(layout: Int, ids: List<Int>) {
        val root = inflate(layout)
        val rowA = row(root, 2)
        val rowB = row(root, 3)
        assertEquals(
            "row A must contain exactly the three first prayers",
            ids.subList(0, 3),
            (0 until rowA.childCount).map { (rowA.getChildAt(it) as ViewGroup).getChildAt(0).id }
        )
        assertEquals(
            "row B must contain exactly the three last prayers",
            ids.subList(3, 6),
            (0 until rowB.childCount).map { (rowB.getChildAt(it) as ViewGroup).getChildAt(0).id }
        )
    }

    @Test
    fun `arabic layout reads right to left and is mirrored in english`() {
        // Arabic: rightmost of row A is Fajr, rightmost of row B is Asr.
        assertPrayerOrder(
            R.layout.widget_mawaqiti_ar,
            listOf(
                R.id.widget_dhuhr_name, R.id.widget_sunrise_name, R.id.widget_fajr_name,
                R.id.widget_isha_name, R.id.widget_maghrib_name, R.id.widget_asr_name
            )
        )
        // English: exact mirror, Fajr leftmost.
        assertPrayerOrder(
            R.layout.widget_mawaqiti_en,
            listOf(
                R.id.widget_fajr_name, R.id.widget_sunrise_name, R.id.widget_dhuhr_name,
                R.id.widget_asr_name, R.id.widget_maghrib_name, R.id.widget_isha_name
            )
        )
    }

    @Test
    fun `row one keeps wordmark and city on opposite edges with a real gap`() {
        assertRowOne(R.layout.widget_mawaqiti_ar, wordmarkFirst = false)
        assertRowOne(R.layout.widget_mawaqiti_en, wordmarkFirst = true)
    }

    @Test
    fun `both layouts declare the same ids so RemoteViews can fill either one`() {
        val ar = inflate(R.layout.widget_mawaqiti_ar)
        val en = inflate(R.layout.widget_mawaqiti_en)
        val ids = listOf(
            R.id.widget_root, R.id.widget_wordmark, R.id.widget_city, R.id.widget_date,
            R.id.widget_next, R.id.widget_fajr_name, R.id.widget_fajr_time,
            R.id.widget_sunrise_name, R.id.widget_sunrise_time, R.id.widget_dhuhr_name,
            R.id.widget_dhuhr_time, R.id.widget_asr_name, R.id.widget_asr_time,
            R.id.widget_maghrib_name, R.id.widget_maghrib_time, R.id.widget_isha_name,
            R.id.widget_isha_time
        )
        for (id in ids) {
            assertNotNull("missing in AR layout: $id", ar.findViewById<View>(id))
            assertNotNull("missing in EN layout: $id", en.findViewById<View>(id))
        }
    }

    @Test
    fun `provider xml is a fixed 4x2 widget with no resize handles`() {
        val xml = File("src/main/res/xml/mawaqiti_widget_info.xml").readText()
        val doc = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(xml.byteInputStream())
        val provider = doc.documentElement as Element

        fun attr(name: String): String? =
            provider.getAttributeNS("http://schemas.android.com/apk/res/android", name)
                .ifEmpty { null }

        // W4: no resize handles at all.
        assertEquals("none", attr("resizeMode"))
        // Correct declared geometry for 4x2 on both API 31+ and older APIs.
        assertEquals("4", attr("targetCellWidth"))
        assertEquals("2", attr("targetCellHeight"))
        assertEquals("home_screen", attr("widgetCategory"))
        assertEquals("@string/widget_description", attr("description"))
        assertEquals("@layout/widget_mawaqiti_ar", attr("initialLayout"))
        assertEquals("@layout/widget_mawaqiti_ar", attr("previewLayout"))

        // 4 cells ≈ 250 dp, 2 cells ≈ 110 dp (70 * n - 30 rule).
        assertEquals("250dp", attr("minWidth"))
        assertEquals("110dp", attr("minHeight"))
    }

    @Test
    fun `no widget layout references a font resource or a non RemoteViews spacer`() {
        // beta6 / W5: @font/ breaks RemoteViews inflation on API 24-25 and a
        // Space view is rejected by the platform filter — both are banned.
        for (name in listOf(
            "widget_mawaqiti_ar",
            "widget_mawaqiti_en",
            "widget_mawaqiti_fallback"
        )) {
            val xml = File("src/main/res/layout/$name.xml").readText()
            assertFalse("$name must not use @font/", xml.contains("@font/"))
            assertFalse(
                "$name must not contain a Space element",
                Regex("<\\s*Space\\b").containsMatchIn(xml)
            )
            assertFalse("$name must not use android:fontFamily", xml.contains("fontFamily"))
        }
    }
}
