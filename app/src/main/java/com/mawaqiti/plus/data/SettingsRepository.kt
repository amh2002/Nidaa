package com.mawaqiti.plus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.notify.EventNotification
import com.mawaqiti.plus.notify.NotificationSettings
import com.mawaqiti.plus.notify.NotifyEvent
import com.mawaqiti.plus.notify.NotifyMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mawaqiti_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val cityOrder = intPreferencesKey("city_order")
        val summerTime = booleanPreferencesKey("summer_time")
        val language = stringPreferencesKey("language")
        val languageInitialized = booleanPreferencesKey("language_initialized")
        val clockFormat = stringPreferencesKey("clock_format")
        /** beta5: widget language, intentionally independent of the UI language. */
        val widgetLanguage = stringPreferencesKey("widget.lang")
        /** beta 7.9: in-app text multiplier, default 1.0. */
        val fontScale = floatPreferencesKey("font_scale")
        val notifyMaster = booleanPreferencesKey("notify_master")
        fun eventEnabled(event: NotifyEvent) =
            booleanPreferencesKey("notify_${event.storageKey}_enabled")
        fun eventMode(event: NotifyEvent) =
            stringPreferencesKey("notify_${event.storageKey}_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { prefs ->
            AppSettings(
                cityOrder = prefs[Keys.cityOrder] ?: City.DEFAULT_ORDER,
                summerTime = prefs[Keys.summerTime] ?: false,
                language = AppLanguage.fromTag(prefs[Keys.language]),
                languageInitialized = prefs[Keys.languageInitialized] ?: false,
                clockFormat = ClockFormat.fromKey(prefs[Keys.clockFormat]),
                fontScale = FontScale.normalize(prefs[Keys.fontScale] ?: FontScale.DEFAULT),
                notifications = NotificationSettings(
                    masterEnabled = prefs[Keys.notifyMaster] ?: true,
                    events = NotifyEvent.entries.associateWith { event ->
                        val default = NotificationSettings.DEFAULT_EVENTS.getValue(event)
                        EventNotification(
                            enabled = prefs[Keys.eventEnabled(event)] ?: default.enabled,
                            mode = prefs[Keys.eventMode(event)]
                                ?.let(NotifyMode::fromKey)
                                ?: default.mode
                        )
                    }
                ),
                widgetLanguage = AppLanguage.fromTagOrNull(prefs[Keys.widgetLanguage])
            )
        }

    suspend fun setCityOrder(order: Int) = edit { it[Keys.cityOrder] = order }

    suspend fun setSummerTime(enabled: Boolean) = edit { it[Keys.summerTime] = enabled }

    suspend fun setClockFormat(format: ClockFormat) = edit {
        it[Keys.clockFormat] = format.storageKey
    }

    /** beta 7.9: stores one of [FontScale.STEPS]; applied without restart. */
    suspend fun setFontScale(scale: Float) = edit {
        it[Keys.fontScale] = FontScale.normalize(scale)
    }

    suspend fun setLanguage(language: AppLanguage) = edit {
        it[Keys.language] = language.tag
        it[Keys.languageInitialized] = true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) = edit {
        it[Keys.notifyMaster] = enabled
    }

    suspend fun setEventEnabled(event: NotifyEvent, enabled: Boolean) = edit {
        it[Keys.eventEnabled(event)] = enabled
    }

    suspend fun setEventMode(event: NotifyEvent, mode: NotifyMode) = edit {
        it[Keys.eventMode(event)] = mode.storageKey
    }

    /**
     * Sets the same mode for every event that is currently enabled. Disabled
     * events keep their stored mode, so re-enabling one restores its old choice.
     */
    suspend fun setModeForEnabledEvents(mode: NotifyMode) = edit { prefs ->
        for (event in NotifyEvent.entries) {
            val default = NotificationSettings.DEFAULT_EVENTS.getValue(event)
            val enabled = prefs[Keys.eventEnabled(event)] ?: default.enabled
            if (enabled) prefs[Keys.eventMode(event)] = mode.storageKey
        }
    }

    /** Widget-only language. Does not touch the app UI language. */
    suspend fun setWidgetLanguage(language: AppLanguage) = edit {
        it[Keys.widgetLanguage] = language.tag
    }

    /**
     * beta6 migration, run once at process start.
     *
     * Earlier betas let the user pick an adhan file through the Storage Access
     * Framework and persisted the resulting `content://` URI under
     * `notify_adhan_uri`, together with a persisted read grant. That feature is
     * gone: the app has exactly one adhan source (`res/raw/adhan.mp3`). This
     * removes the stale key and returns it so the caller can release the grant.
     */
    suspend fun purgeLegacyAdhanUri(): String? {
        var stored: String? = null
        context.dataStore.edit { prefs ->
            stored = prefs[LEGACY_ADHAN_URI_KEY]
            prefs.remove(LEGACY_ADHAN_URI_KEY)
        }
        return stored
    }

    private companion object {
        /**
         * Migration-only key. It is deliberately not part of [AppSettings]:
         * nothing can read, write or display a custom adhan file any more.
         */
        val LEGACY_ADHAN_URI_KEY = stringPreferencesKey("notify_adhan_uri")
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs -> block(prefs) }
    }
}
