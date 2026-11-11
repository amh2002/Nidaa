package com.mawaqiti.plus

import android.content.Context
import android.view.View
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.notify.NotificationText
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta5 rename gate.
 *
 * Locks the approved naming: one identical launcher label that contains the
 * Arabic name, a short Arabic wordmark for in-app surfaces, an About title that
 * pairs both scripts, and zero occurrences of the old brand in any string
 * resource (the resources are what ships to the device).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl")
class BrandIdentityTest {

    private val context: Context
        get() = androidx.test.core.app.ApplicationProvider.getApplicationContext()

    private fun localized(language: AppLanguage): Context =
        NotificationText.localizedContext(context, language)

    @Test
    fun `launcher label is identical in both locales and carries the arabic name`() {
        val arabic = localized(AppLanguage.ARABIC).getString(R.string.app_name)
        val english = localized(AppLanguage.ENGLISH).getString(R.string.app_name)

        assertEquals("نداء - Nidaa", arabic)
        assertEquals("the label must not be localized", arabic, english)
        assertTrue(arabic.contains("نداء"))
        // Transliteration is intentional: never "Call"/"Nida".
        assertFalse(arabic.contains("Call", ignoreCase = true))
    }

    @Test
    fun `in-app wordmark stays the short arabic name in both locales`() {
        assertEquals("نداء", localized(AppLanguage.ARABIC).getString(R.string.brand_name))
        assertEquals("نداء", localized(AppLanguage.ENGLISH).getString(R.string.brand_name))
    }

    @Test
    fun `about title pairs both scripts and the claim covers both countries`() {
        val arabic = localized(AppLanguage.ARABIC)
        val english = localized(AppLanguage.ENGLISH)

        assertEquals("نداء · Nidaa", arabic.getString(R.string.settings_about_brand))
        assertEquals("نداء · Nidaa", english.getString(R.string.settings_about_brand))

        assertTrue(arabic.getString(R.string.settings_about_timetable).contains("فلسطين"))
        assertTrue(arabic.getString(R.string.settings_about_timetable).contains("الأردن"))
        assertTrue(english.getString(R.string.settings_about_timetable).contains("Palestine"))
        assertTrue(english.getString(R.string.settings_about_timetable).contains("Jordan"))
    }

    @Test
    fun `developer credit is its own line and never a mid-sentence fragment`() {
        for (language in AppLanguage.entries) {
            val credit = localized(language).getString(R.string.settings_about_credit)
            assertTrue(credit.startsWith("تطوير:"))
            assertTrue(credit.contains("عبد الرحمن حسين"))
            assertTrue(credit.contains("Abdulrahman Hussein"))
            // The credit is not embedded in either claim.
            assertFalse(credit.contains("الجدول"))
            assertFalse(credit.contains("دون إنترنت"))
        }
    }

    @Test
    fun `no string resource still ships the old brand`() {
        // Written as escapes on purpose: the shipped tree must contain no literal
        // occurrence of the retired brand, and this sentinel is the only place
        // that still knows how to spell it.
        val legacy = listOf("\u0645\u0648\u0627\u0642\u064a\u062a\u064a", "Mawaqiti")
        val offenders = mutableListOf<String>()

        for (field in R.string::class.java.fields) {
            if (field.type != Int::class.javaPrimitiveType) continue
            val id = field.getInt(null)
            val value = runCatching { context.getString(id) }.getOrNull() ?: continue
            for (needle in legacy) {
                if (value.contains(needle, ignoreCase = true)) {
                    offenders += "${field.name}=$value"
                }
            }
        }

        assertTrue("legacy brand still present in: $offenders", offenders.isEmpty())
    }

    @Test
    fun `locales config still declares both languages`() {
        val arabic = localized(AppLanguage.ARABIC).resources.configuration
        val english = localized(AppLanguage.ENGLISH).resources.configuration

        assertEquals("ar", arabic.locales[0]?.language ?: Locale.forLanguageTag("ar").language)
        assertEquals("en", english.locales[0]?.language ?: Locale.ENGLISH.language)
        assertEquals(
            View.LAYOUT_DIRECTION_RTL,
            arabic.layoutDirection
        )
    }
}
