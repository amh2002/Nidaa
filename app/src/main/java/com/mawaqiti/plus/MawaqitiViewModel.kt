package com.mawaqiti.plus

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.CityRepository
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.data.SettingsRepository
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEngine
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MawaqitiViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val cityRepository = CityRepository(application)

    /** All 90 extracted entries, ready for the two-step cascade. */
    val cities: List<City> = cityRepository.all()

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** Null until DataStore has emitted; used to make first-launch language setup atomic. */
    val settingsState: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<AppSettings> = settingsState
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val city: StateFlow<City> = settings
        .map { cityRepository.byOrder(it.cityOrder) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, cityRepository.defaultCity())

    val day: StateFlow<TableDay> = combine(settings, city, _today) { value, selected, date ->
        val summer = value.summerTime && selected.country == Country.PS
        TableEngine.day(
            profile = selected.profile,
            minDiff = selected.minDiff,
            date = date,
            summerTime = summer
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TableEngine.day(
            profile = cityRepository.defaultCity().profile,
            minDiff = cityRepository.defaultCity().minDiff,
            date = LocalDate.now(),
            summerTime = false
        )
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                refreshDate()
            }
        }
    }

    fun refreshDate() {
        val now = LocalDate.now()
        if (_today.value != now) _today.value = now
    }

    fun selectCity(city: City) = viewModelScope.launch {
        settingsRepository.setCityOrder(city.order)
    }

    fun setSummerTime(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setSummerTime(enabled)
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        settingsRepository.setLanguage(language)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.tag)
        )
    }

    fun citiesFor(country: Country, isArabic: Boolean): List<City> =
        cityRepository.forCountry(country, isArabic)

    fun cityCount(country: Country): Int = cityRepository.countFor(country)

    fun cityByOrder(order: Int): City = cityRepository.byOrder(order)
}
