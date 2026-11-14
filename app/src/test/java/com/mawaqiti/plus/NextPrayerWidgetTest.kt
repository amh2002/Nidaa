package com.mawaqiti.plus

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.notify.MawaqitiWidgetProvider
import com.mawaqiti.plus.notify.NextPrayerWidgetProvider
import com.mawaqiti.plus.notify.SafeWidgetProvider
import com.mawaqiti.plus.notify.WidgetText
import com.mawaqiti.plus.notify.WidgetUpdater
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.w3c.dom.Element

/**
 * beta 7.9 gate for the compact next-prayer widget.
 *
 * The widget must (1) compute the remaining time once per update with correct
 * midnight / Fajr-tomorrow rollover, (2) ship a resizable 2x2 provider that is
 * registered exactly like the main widget, and (3) be refreshed by the same
 * [WidgetUpdater.updateAll] pass as the main widget.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl-w411dp-h891dp-xhdpi")
class NextPrayerWidgetTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val day = TableDay(
        fajr = 300,
        sunrise = 360,
        dhuhr = 720,
        asr = 930,
        maghrib = 1140,
        isha = 1200,
        lastThird = 60,
        fajrTomorrow = 301,
        ishaYesterday = 1201,
        row = 0
    )

    @Test
    fun `remaining time reads naturally in both languages`() {
        assertEquals("in 2h 5m", WidgetText.remaining(125, isArabic = false))
        assertEquals("in 2h", WidgetText.remaining(120, isArabic = false))
        assertEquals("in 5m", WidgetText.remaining(5, isArabic = false))
        assertEquals("بعد 2س 5د", WidgetText.remaining(125, isArabic = true))
        assertEquals("بعد 2س", WidgetText.remaining(120, isArabic = true))
        assertEquals("بعد 5د", WidgetText.remaining(5, isArabic = true))
    }

    @Test
    fun `minutes until wraps past midnight`() {
        // Same day.
        assertEquals(541, WidgetText.minutesUntil(301, 1200))
        // Exactly midnight, next prayer is today's Fajr.
        assertEquals(300, WidgetText.minutesUntil(300, 0))
        // One minute before midnight.
        assertEquals(302, WidgetText.minutesUntil(301, 1439))
        // Negative deltas must never surface as "-…".
        assertTrue(WidgetText.minutesUntil(1, 1439) > 0)
    }

    @Test
    fun `rollover picks Sunrise after Fajr and tomorrow's Fajr after Isha`() {
        assertEquals(TableEvent.FAJR to 300, WidgetText.nextPrayer(day, 299))
        assertEquals(TableEvent.SUNRISE to 360, WidgetText.nextPrayer(day, 300))
        assertEquals(TableEvent.SUNRISE to 360, WidgetText.nextPrayer(day, 359))
        // Isha at 20:00 -> next is Fajr tomorrow (301).
        assertEquals(TableEvent.FAJR to 301, WidgetText.nextPrayer(day, 1200))
        assertEquals(
            "in 9h 1m",
            WidgetText.remaining(
                WidgetText.minutesUntil(day.fajrTomorrow, 1200),
                isArabic = false
            )
        )
        // After midnight the new day's Fajr is still ahead.
        assertEquals(TableEvent.FAJR to 300, WidgetText.nextPrayer(day, 0))
        assertEquals(
            "in 5h",
            WidgetText.remaining(
                WidgetText.minutesUntil(day.fajr, 0),
                isArabic = false
            )
        )
    }

    @Test
    fun `provider is a resizable 2x2 with no self-update period`() {
        val provider = providerElement("src/main/res/xml/widget_next_info.xml")
        fun attr(name: String): String? =
            provider.getAttributeNS("http://schemas.android.com/apk/res/android", name)
                .ifEmpty { null }

        assertEquals("@string/widget_next_description", attr("description"))
        assertEquals("@layout/widget_next", attr("initialLayout"))
        assertEquals("@layout/widget_next", attr("previewLayout"))
        assertNotNull("older launchers still need a PNG preview", attr("previewImage"))
        assertEquals("horizontal|vertical", attr("resizeMode"))
        assertEquals("2", attr("targetCellWidth"))
        assertEquals("2", attr("targetCellHeight"))
        assertEquals("110dp", attr("minWidth"))
        assertEquals("110dp", attr("minHeight"))
        assertEquals("110dp", attr("minResizeWidth"))
        assertEquals("110dp", attr("minResizeHeight"))
        assertEquals("220dp", attr("maxResizeWidth"))
        assertEquals("220dp", attr("maxResizeHeight"))
        // Refreshed only by the shared pipeline, never by the system period.
        assertEquals("0", attr("updatePeriodMillis"))
    }

    @Test
    fun `receiver is registered like the main widget`() {
        val manifest = providerElement("src/main/AndroidManifest.xml")
        val receivers = manifest.getElementsByTagName("receiver")
        var next: Element? = null
        for (index in 0 until receivers.length) {
            val element = receivers.item(index) as Element
            if (element.getAttributeNS(ANDROID, "name").endsWith("NextPrayerWidgetProvider")) {
                next = element
            }
        }
        assertNotNull("next-prayer receiver missing from the manifest", next)
        assertEquals("false", next!!.getAttributeNS(ANDROID, "exported"))
        assertEquals("@string/widget_next_label", next.getAttributeNS(ANDROID, "label"))

        var hasMetaData = false
        val children = next.getElementsByTagName("meta-data")
        for (index in 0 until children.length) {
            val meta = children.item(index) as Element
            if (meta.getAttributeNS(ANDROID, "resource") == "@xml/widget_next_info") {
                hasMetaData = true
            }
        }
        assertTrue("widget provider info not linked", hasMetaData)

        // Both widgets share the same safety base class.
        assertTrue(SafeWidgetProvider::class.java.isAssignableFrom(
            NextPrayerWidgetProvider::class.java
        ))
        assertTrue(SafeWidgetProvider::class.java.isAssignableFrom(
            MawaqitiWidgetProvider::class.java
        ))
    }

    @Test
    fun `both layouts expose the same ids and autosize on api 26`() {
        val ids = listOf(
            "widget_next_root", "widget_next_label", "widget_next_name",
            "widget_next_time", "widget_next_left"
        )
        for (dir in listOf("layout", "layout-v26")) {
            val xml = File("src/main/res/$dir/widget_next.xml").readText()
            for (id in ids) {
                assertTrue("$dir/widget_next.xml is missing @+id/$id", xml.contains("@+id/$id"))
            }
            assertTrue(
                "$dir/widget_next.xml must keep the shared widget background",
                xml.contains("@drawable/widget_background")
            )
        }
        val v26 = File("src/main/res/layout-v26/widget_next.xml").readText()
        assertTrue(v26.contains("android:autoSizeTextType=\"uniform\""))
        val base = File("src/main/res/layout/widget_next.xml").readText()
        assertFalse("API 24-25 ignores autosizing attributes", base.contains("autoSizeTextType"))
    }

    @Test
    fun `one update pass fills both widgets`() = runBlocking {
        val manager = AppWidgetManager.getInstance(context)
        val shadow = Shadows.shadowOf(manager)
        val mainId = shadow.createWidget(
            MawaqitiWidgetProvider::class.java,
            R.layout.widget_mawaqiti_ar
        )
        val nextId = shadow.createWidget(
            NextPrayerWidgetProvider::class.java,
            R.layout.widget_next
        )
        val mainComponent = ComponentName(context, MawaqitiWidgetProvider::class.java)
        val nextComponent = ComponentName(context, NextPrayerWidgetProvider::class.java)
        assertTrue(manager.getAppWidgetIds(mainComponent).contains(mainId))
        assertTrue(manager.getAppWidgetIds(nextComponent).contains(nextId))

        WidgetUpdater.updateAll(context)

        assertNotNull("main widget was not refreshed", shadow.getViewFor(mainId))
        val nextView = shadow.getViewFor(nextId)
        assertNotNull("next-prayer widget was not refreshed", nextView)
    }

    private fun providerElement(path: String): Element {
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File(path).readBytes().inputStream())
        return document.documentElement as Element
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
