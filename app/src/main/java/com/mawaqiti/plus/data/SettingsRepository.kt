package com.mawaqiti.plus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        val notifyMaster = booleanPreferencesKey("notify_master")
        val adhanUri = stringPreferencesKey("notify_adhan_uri")
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
                    },
                    adhanUri = prefs[Keys.adhanUri]
                )
            )
        }

    suspend fun setCityOrder(order: Int) = edit { it[Keys.cityOrder] = order }

    suspend fun setSummerTime(enabled: Boolean) = edit { it[Keys.summerTime] = enabled }

    suspend fun setClockFormat(format: ClockFormat) = edit {
        it[Keys.clockFormat] = format.storageKey
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

    suspend fun setAdhanUri(uri: String?) = edit {
        if (uri == null) it.remove(Keys.adhanUri) else it[Keys.adhanUri] = uri
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs -> block(prefs) }
    }
}
