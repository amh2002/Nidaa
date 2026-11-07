package com.mawaqiti.plus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mawaqiti.plus.engine.AsrMadhab
import com.mawaqiti.plus.engine.CalculationMethod
import com.mawaqiti.plus.engine.HighLatitudeRule
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mawaqiti_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val method = stringPreferencesKey("method")
        val madhab = stringPreferencesKey("madhab")
        val highLatitude = stringPreferencesKey("high_latitude")
        val customFajr = doublePreferencesKey("custom_fajr")
        val customIsha = doublePreferencesKey("custom_isha")
        val latitude = doublePreferencesKey("latitude")
        val longitude = doublePreferencesKey("longitude")
        val cityAr = stringPreferencesKey("city_ar")
        val cityEn = stringPreferencesKey("city_en")
        val locationSource = stringPreferencesKey("location_source")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val imsakOffset = intPreferencesKey("imsak_offset")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { prefs ->
            AppSettings(
                method = prefs[Keys.method]?.let { runCatching { CalculationMethod.valueOf(it) }.getOrNull() }
                    ?: CalculationMethod.MWL,
                asrMadhab = prefs[Keys.madhab]?.let { runCatching { AsrMadhab.valueOf(it) }.getOrNull() }
                    ?: AsrMadhab.SHAFI,
                highLatitudeRule = prefs[Keys.highLatitude]
                    ?.let { runCatching { HighLatitudeRule.valueOf(it) }.getOrNull() }
                    ?: HighLatitudeRule.NIGHT_MIDDLE,
                customFajrAngle = prefs[Keys.customFajr] ?: 18.0,
                customIshaAngle = prefs[Keys.customIsha] ?: 17.0,
                latitude = prefs[Keys.latitude] ?: 31.7683,
                longitude = prefs[Keys.longitude] ?: 35.2137,
                cityNameAr = prefs[Keys.cityAr] ?: "القدس",
                cityNameEn = prefs[Keys.cityEn] ?: "Al-Quds",
                locationSource = prefs[Keys.locationSource]
                    ?.let { runCatching { LocationSource.valueOf(it) }.getOrNull() }
                    ?: LocationSource.DEFAULT,
                onboardingDone = prefs[Keys.onboardingDone] ?: false,
                imsakOffsetMinutes = prefs[Keys.imsakOffset] ?: 10
            )
        }

    suspend fun setMethod(method: CalculationMethod) = edit { it[Keys.method] = method.name }

    suspend fun setMadhab(madhab: AsrMadhab) = edit { it[Keys.madhab] = madhab.name }

    suspend fun setHighLatitudeRule(rule: HighLatitudeRule) =
        edit { it[Keys.highLatitude] = rule.name }

    suspend fun setCustomAngles(fajr: Double, isha: Double) = edit {
        it[Keys.customFajr] = fajr
        it[Keys.customIsha] = isha
    }

    suspend fun setLocation(
        latitude: Double,
        longitude: Double,
        cityAr: String,
        cityEn: String,
        source: LocationSource
    ) = edit {
        it[Keys.latitude] = latitude
        it[Keys.longitude] = longitude
        it[Keys.cityAr] = cityAr
        it[Keys.cityEn] = cityEn
        it[Keys.locationSource] = source.name
    }

    suspend fun setOnboardingDone(done: Boolean) = edit { it[Keys.onboardingDone] = done }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs -> block(prefs) }
    }
}
