package com.nidaa.prayer.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.updatesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nidaa_updates"
)

/**
 * Updater-only preferences. Kept in a separate DataStore file so the updater
 * package stays isolated from the prayer settings store.
 *
 * `updates_enabled` is the owner-visible opt-out (default ON). The other keys
 * make the 24-hour cadence and the one-notification-per-release rule survive
 * process death.
 */
class UpdatesStore(context: Context) {

    private val appContext = context.applicationContext

    private val preferences = appContext.updatesDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    val enabled: Flow<Boolean> = preferences.map { it[Keys.enabled] ?: true }

    suspend fun enabledNow(): Boolean = enabled.first()

    suspend fun lastAutoCheckMs(): Long =
        preferences.first()[Keys.lastAutoCheck] ?: 0L

    suspend fun markAutoChecked(nowMs: Long) = edit { it[Keys.lastAutoCheck] = nowMs }

    suspend fun notifiedTag(): String? =
        preferences.first()[Keys.notifiedTag]?.takeIf { it.isNotBlank() }

    suspend fun markNotified(tag: String) = edit { it[Keys.notifiedTag] = tag }

    suspend fun pendingRelease(): ReleaseInfo? =
        ReleaseInfo.fromStorage(preferences.first()[Keys.pending])

    suspend fun setPending(release: ReleaseInfo?) = edit { prefs ->
        if (release == null) prefs.remove(Keys.pending)
        else prefs[Keys.pending] = release.storageJson()
    }

    suspend fun setEnabled(enabled: Boolean) = edit { it[Keys.enabled] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        appContext.updatesDataStore.edit { prefs -> block(prefs) }
    }

    private object Keys {
        val enabled = booleanPreferencesKey("updates_enabled")
        val lastAutoCheck = longPreferencesKey("last_auto_check_ms")
        val notifiedTag = stringPreferencesKey("last_notified_tag")
        val pending = stringPreferencesKey("pending_release")
    }
}
