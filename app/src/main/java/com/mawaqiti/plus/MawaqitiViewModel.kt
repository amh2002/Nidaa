package com.mawaqiti.plus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.CityRepository
import com.mawaqiti.plus.data.LocationHelper
import com.mawaqiti.plus.data.LocationSource
import com.mawaqiti.plus.data.SettingsRepository
import com.mawaqiti.plus.engine.AsrMadhab
import com.mawaqiti.plus.engine.CalculationMethod
import com.mawaqiti.plus.engine.HighLatitudeRule
import com.mawaqiti.plus.engine.PrayerDay
import com.mawaqiti.plus.engine.PrayerEngine
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MawaqitiViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val cityRepository = CityRepository(application)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _day = MutableStateFlow<PrayerDay?>(null)
    val day: StateFlow<PrayerDay?> = _day.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating.asStateFlow()

    private val _messageRes = MutableStateFlow<Int?>(null)
    val messageRes: StateFlow<Int?> = _messageRes.asStateFlow()

    val cities: List<City> = cityRepository.all()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { value ->
                _loaded.value = true
                recompute(value)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                if (_day.value?.date != LocalDate.now()) {
                    recompute(settings.value)
                }
            }
        }
    }

    private fun recompute(value: AppSettings) {
        try {
            _day.value = PrayerEngine.calculate(
                date = LocalDate.now(),
                latitude = value.latitude,
                longitude = value.longitude,
                zone = ZoneId.systemDefault(),
                settings = value.toCalculationSettings()
            )
            _hasError.value = false
        } catch (error: Exception) {
            _day.value = null
            _hasError.value = true
        }
    }

    fun setMethod(method: CalculationMethod) = viewModelScope.launch {
        settingsRepository.setMethod(method)
    }

    fun setMadhab(madhab: AsrMadhab) = viewModelScope.launch {
        settingsRepository.setMadhab(madhab)
    }

    fun setHighLatitudeRule(rule: HighLatitudeRule) = viewModelScope.launch {
        settingsRepository.setHighLatitudeRule(rule)
    }

    fun setCustomAngles(fajr: Double, isha: Double) = viewModelScope.launch {
        settingsRepository.setCustomAngles(fajr, isha)
    }

    fun selectCity(city: City) = viewModelScope.launch {
        settingsRepository.setLocation(
            latitude = city.latitude,
            longitude = city.longitude,
            cityAr = city.nameAr,
            cityEn = city.nameEn,
            source = LocationSource.MANUAL
        )
    }

    fun setManualLocation(latitude: Double, longitude: Double) = viewModelScope.launch {
        val app = getApplication<Application>()
        settingsRepository.setLocation(
            latitude = latitude,
            longitude = longitude,
            cityAr = app.getString(R.string.current_location),
            cityEn = app.getString(R.string.current_location),
            source = LocationSource.MANUAL
        )
    }

    fun refreshLocation() {
        if (_locating.value) return
        viewModelScope.launch {
            _locating.value = true
            _messageRes.value = null
            val location = LocationHelper.currentLocation(getApplication())
            if (location == null) {
                _messageRes.value = R.string.location_failed
            } else {
                val nearest = cityRepository.nearest(location.latitude, location.longitude)
                val app = getApplication<Application>()
                if (nearest != null && nearest.distanceKm <= 150.0) {
                    settingsRepository.setLocation(
                        location.latitude,
                        location.longitude,
                        nearest.city.nameAr,
                        nearest.city.nameEn,
                        LocationSource.GPS
                    )
                } else {
                    settingsRepository.setLocation(
                        location.latitude,
                        location.longitude,
                        app.getString(R.string.current_location),
                        app.getString(R.string.current_location),
                        LocationSource.GPS
                    )
                }
            }
            _locating.value = false
        }
    }

    fun completeOnboarding() = viewModelScope.launch {
        settingsRepository.setOnboardingDone(true)
    }

    fun notifyPermissionDenied() {
        _messageRes.value = R.string.permission_denied
    }

    fun clearMessage() {
        _messageRes.value = null
    }

    fun searchCities(query: String): List<City> = cityRepository.search(query)
}
