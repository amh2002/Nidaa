package com.mawaqiti.plus

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.notify.AdhanSound
import com.mawaqiti.plus.notify.AdhanSoundProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * beta14: the adhan registry is the single source of truth.
 *
 * These assertions cover what the owner can actually check:
 *  - exactly the seven shipped sounds, in the specified order;
 *  - the default is أذان الأقصى تكبيرة;
 *  - every entry points at a packaged `res/raw` file with the required
 *    lower-case/underscore name, and that file is byte-for-byte the master
 *    (same length as the source file in `C:\rea_ws\adhan`);
 *  - the Arabic display names and the new UI strings come from `strings.xml`;
 *  - [AdhanSoundProvider] is the one read path (resource + label + selection).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rSA-ldrtl")
class AdhanSoundTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Source lengths from `C:\rea_ws\adhan`, bytes copied unmodified. */
    private val masterLengths = mapOf(
        AdhanSound.AQSA_FULL to 433_801L,
        AdhanSound.AQSA_TKBERA to 26_795L,
        AdhanSound.MAKKAH_FULL to 409_088L,
        AdhanSound.MAKKAH_TKBERA to 22_406L,
        AdhanSound.MASR_FULL to 737_408L,
        AdhanSound.MASR_TKBERA to 12_467L,
        AdhanSound.ALKURD_FULL to 7_192_725L
    )

    @Test
    fun `registry has the seven shipped sounds in the owner order`() {
        assertEquals(
            listOf(
                AdhanSound.AQSA_FULL,
                AdhanSound.AQSA_TKBERA,
                AdhanSound.MAKKAH_FULL,
                AdhanSound.MAKKAH_TKBERA,
                AdhanSound.MASR_FULL,
                AdhanSound.MASR_TKBERA,
                AdhanSound.ALKURD_FULL
            ),
            AdhanSound.ordered
        )
        assertEquals(7, AdhanSound.entries.size)
    }

    @Test
    fun `default is al aqsa takbir`() {
        assertEquals(AdhanSound.AQSA_TKBERA, AdhanSound.DEFAULT)
        assertEquals(AdhanSound.AQSA_TKBERA, AppSettings().adhanSound)
        assertEquals(AdhanSound.AQSA_TKBERA, AdhanSound.fromKey(null))
    }

    @Test
    fun `keys are unique and round trip`() {
        val keys = AdhanSound.entries.map { it.storageKey }
        assertEquals(keys.size, keys.toSet().size)
        AdhanSound.entries.forEach { sound ->
            assertEquals(sound, AdhanSound.fromKey(sound.storageKey))
        }
        assertEquals(AdhanSound.DEFAULT, AdhanSound.fromKey("future_sound"))
        assertEquals(AdhanSound.DEFAULT, AdhanSound.fromKey(""))
    }

    @Test
    fun `every sound maps to the required lowercase raw resource`() {
        AdhanSound.entries.forEach { sound ->
            val entryName = context.resources.getResourceEntryName(sound.rawRes)
            assertEquals(sound.rawName, entryName)
            assertTrue(entryName.startsWith("adhan_"))
            assertTrue(entryName.all { it.isLowerCase() || it.isDigit() || it == '_' })
        }
    }

    @Test
    fun `packaged audio is the untouched master file`() {
        AdhanSound.entries.forEach { sound ->
            val length = context.resources.openRawResource(sound.rawRes).use { input ->
                var total = 0L
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                }
                total
            }
            assertEquals(sound.rawName, masterLengths.getValue(sound), length)
        }
    }

    @Test
    fun `provider returns the registry resource and the string resource label`() {
        AdhanSound.entries.forEach { sound ->
            assertEquals(sound.rawRes, AdhanSoundProvider.rawResource(sound))
            assertEquals(context.getString(sound.labelRes), AdhanSoundProvider.displayName(context, sound))
            assertEquals(sound, AdhanSoundProvider.resolve(AppSettings(adhanSound = sound)))
        }
    }

    @Test
    fun `arabic display names are the specified ones`() {
        assertEquals("أذان الأقصى كامل", context.getString(R.string.adhan_alaqsa_full))
        assertEquals("أذان الأقصى تكبيرة", context.getString(R.string.adhan_alaqsa_tkbera))
        assertEquals("أذان الحرم المكي كامل", context.getString(R.string.adhan_makkah_full))
        assertEquals("أذان الحرم المكي تكبيرة", context.getString(R.string.adhan_makkah_tkbera))
        assertEquals("أذان مصر كامل", context.getString(R.string.adhan_masr_full))
        assertEquals("أذان مصر تكبيرة", context.getString(R.string.adhan_masr_tkbera))
        assertEquals("أذان الكرد كامل", context.getString(R.string.adhan_alkurd_full))
    }

    @Test
    fun `settings and current-time strings come from strings xml`() {
        assertEquals("صوت الأذان", context.getString(R.string.settings_adhan_sound_title))
        assertEquals("الوقت الآن", context.getString(R.string.current_time_prefix))
        assertNotEquals("", context.getString(R.string.settings_adhan_sound_desc))
    }
}
