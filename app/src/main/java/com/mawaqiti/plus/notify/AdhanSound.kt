package com.mawaqiti.plus.notify

import android.content.Context
import android.content.res.AssetFileDescriptor
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * beta14: the ONE registry of adhan sounds.
 *
 * Declaration order is the order the picker renders (and the order the owner
 * specified): Al-Aqsa full, Al-Aqsa takbir, Makkah full, Makkah takbir, Egypt
 * full, Egypt takbir, Al-Kurd full.
 *
 * Each entry binds three things in one place:
 *  - [storageKey] - the stable DataStore value (never rename a shipped key);
 *  - [rawRes] - the packaged audio in `res/raw`, byte-identical to the owner's
 *    master files (only the resource filename was normalised: lower case,
 *    underscores, because `res/raw` forbids hyphens);
 *  - [labelRes] - the Arabic display name from `strings.xml`.
 *
 * Adding a future adhan is therefore exactly: copy the file into `res/raw`,
 * add one `strings.xml` line and append one entry here. Nothing else - the
 * settings row, the dropdown and the playback service all read this list.
 *
 * [DEFAULT] is the owner's choice for beta14: the Al-Aqsa takbir.
 */
enum class AdhanSound(
    val storageKey: String,
    @RawRes val rawRes: Int,
    @StringRes val labelRes: Int
) {
    AQSA_FULL("alaqsa_full", R.raw.adhan_alaqsa_full, R.string.adhan_alaqsa_full),
    AQSA_TKBERA("alaqsa_tkbera", R.raw.adhan_alaqsa_tkbera, R.string.adhan_alaqsa_tkbera),
    MAKKAH_FULL("makkah_full", R.raw.adhan_makkah_full, R.string.adhan_makkah_full),
    MAKKAH_TKBERA("makkah_tkbera", R.raw.adhan_makkah_tkbera, R.string.adhan_makkah_tkbera),
    MASR_FULL("masr_full", R.raw.adhan_masr_full, R.string.adhan_masr_full),
    MASR_TKBERA("masr_tkbera", R.raw.adhan_masr_tkbera, R.string.adhan_masr_tkbera),
    ALKURD_FULL("alkurd_full", R.raw.adhan_alkurd_full, R.string.adhan_alkurd_full);

    /** The packaged resource filename, e.g. `adhan_alaqsa_tkbera`. */
    val rawName: String get() = "adhan_$storageKey"

    companion object {
        /** beta14 default: أذان الأقصى تكبيرة. */
        val DEFAULT: AdhanSound = AQSA_TKBERA

        /** Every sound in the owner's order; the picker renders exactly this. */
        val ordered: List<AdhanSound> get() = entries.toList()

        /**
         * Stored key -> sound. An unknown or missing key (first launch, a
         * downgrade, a corrupted preference) falls back to [DEFAULT] instead of
         * failing, so the picker always has a valid selection.
         */
        fun fromKey(key: String?): AdhanSound =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}

/**
 * beta14: the single read path for the selected adhan.
 *
 * Both the settings screen and the notification/playback code go through this
 * object, so "what is selected" can never be computed twice with different
 * rules:
 *  - [selected] reads the persisted choice (DataStore) and normalises it;
 *  - [resolve] does the same for an [AppSettings] snapshot a caller already has
 *    (the alarm receiver reads settings once per fire);
 *  - [displayName] is the Arabic label the picker and the report use;
 *  - [openDescriptor] hands the raw resource to `MediaPlayer` without any
 *    conversion, trimming or copy.
 */
object AdhanSoundProvider {

    /** The persisted selection, read once. */
    suspend fun selected(context: Context): AdhanSound =
        SettingsRepository(context).settings.first().adhanSound

    /** The selection from an already-loaded settings snapshot. */
    fun resolve(settings: AppSettings): AdhanSound = settings.adhanSound

    /** The packaged resource id, for notifications/logs. */
    @RawRes
    fun rawResource(sound: AdhanSound): Int = sound.rawRes

    /** The Arabic display name from `strings.xml`. */
    fun displayName(context: Context, sound: AdhanSound): String =
        context.getString(sound.labelRes)

    /** File descriptor for MediaPlayer; null only if the resource is missing. */
    fun openDescriptor(context: Context, sound: AdhanSound): AssetFileDescriptor? =
        runCatching { context.resources.openRawResourceFd(sound.rawRes) }.getOrNull()
}
