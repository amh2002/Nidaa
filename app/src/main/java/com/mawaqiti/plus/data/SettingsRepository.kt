package com.mawaqiti.plus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
                languageInitialized = prefs[Keys.languageInitialized] ?: false
            )
        }

    suspend fun setCityOrder(order: Int) = edit { it[Keys.cityOrder] = order }

    suspend fun setSummerTime(enabled: Boolean) = edit { it[Keys.summerTime] = enabled }

    suspend fun setLanguage(language: AppLanguage) = edit {
        it[Keys.language] = language.tag
        it[Keys.languageInitialized] = true
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs -> block(prefs) }
    }
}
