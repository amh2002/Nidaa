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
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.notify.NotificationScheduler
import com.mawaqiti.plus.notify.NotifyEvent
import com.mawaqiti.plus.notify.NotifyMode
import com.mawaqiti.plus.notify.WidgetUpdater
import com.mawaqiti.plus.ui.theme.SkyPhase
import java.time.LocalDate
import java.time.LocalTime
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

    /** All 90 extracted entries, ready for the country → city picker. */
    val cities: List<City> = cityRepository.all()

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val _nowMinutes = MutableStateFlow(minutesNow())
    val nowMinutes: StateFlow<Int> = _nowMinutes.asStateFlow()

    /** Null until DataStore has emitted; used to make first-launch language setup atomic. */
    val settingsState: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<AppSettings> = settingsState
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val city: StateFlow<City> = settings
        .map { cityRepository.byOrder(it.cityOrder) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, cityRepository.defaultCity())

    private val initialDay: TableDay = TableEngine.day(
        profile = cityRepository.defaultCity().profile,
        minDiff = cityRepository.defaultCity().minDiff,
        date = LocalDate.now(),
        summerTime = false
    )

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
        initialDay
    )

    /**
     * A build flag (`-Pmawaqiti.skyPhase=night`) pins the phase for visual
     * verification. It is empty in shipped builds.
     */
    private val phaseOverride: SkyPhase? = BuildConfig.SKY_PHASE_OVERRIDE
        .takeIf { it.isNotBlank() }
        ?.let { value -> SkyPhase.entries.firstOrNull { it.name.equals(value, true) } }

    val skyPhase: StateFlow<SkyPhase> = combine(day, _nowMinutes) { value, minute ->
        phaseOverride ?: SkyPhase.forMinutes(
            minuteOfDay = minute,
            fajr = value.fajr,
            sunrise = value.sunrise,
            asr = value.asr,
            maghrib = value.maghrib
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        phaseOverride ?: SkyPhase.forMinutes(
            minutesNow(),
            initialDay.fajr,
            initialDay.sunrise,
            initialDay.asr,
            initialDay.maghrib
        )
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                refreshClock()
            }
        }
    }

    fun refreshClock() {
        val now = LocalDate.now()
        if (_today.value != now) _today.value = now
        _nowMinutes.value = minutesNow()
    }

    fun selectCity(city: City) = viewModelScope.launch {
        settingsRepository.setCityOrder(city.order)
        scheduleNotifications()
    }

    fun setSummerTime(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setSummerTime(enabled)
        scheduleNotifications()
    }

    fun setClockFormat(format: ClockFormat) = viewModelScope.launch {
        settingsRepository.setClockFormat(format)
        WidgetUpdater.updateAsync(getApplication())
    }
    /** beta 7.9: in-app text size; observed by the root provider, no restart. */
    fun setFontScale(scale: Float) = viewModelScope.launch {
        settingsRepository.setFontScale(scale)
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        settingsRepository.setLanguage(language)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.tag)
        )
        WidgetUpdater.updateAsync(getApplication())
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotificationsEnabled(enabled)
        scheduleNotifications()
    }

    fun setEventEnabled(event: NotifyEvent, enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setEventEnabled(event, enabled)
        scheduleNotifications()
    }

    fun setEventMode(event: NotifyEvent, mode: NotifyMode) = viewModelScope.launch {
        settingsRepository.setEventMode(event, mode)
        scheduleNotifications()
    }

    fun applyModeToEnabledEvents(mode: NotifyMode) = viewModelScope.launch {
        settingsRepository.setModeForEnabledEvents(mode)
        scheduleNotifications()
    }

    /**
     * Widget-only language. Re-renders the widget immediately (no waiting for
     * the next prayer boundary) and never touches the app UI language.
     */
    fun setWidgetLanguage(language: AppLanguage) = viewModelScope.launch {
        settingsRepository.setWidgetLanguage(language)
        WidgetUpdater.updateAsync(getApplication())
    }

    /** Fires one event's notification in its own mode five seconds from now. */
    fun testEvent(event: NotifyEvent, mode: NotifyMode) {
        NotificationScheduler.scheduleTest(getApplication(), event, mode)
    }

    /** Debug group: one direct test per delivery type. */
    fun testMode(mode: NotifyMode) {
        val event = when (mode) {
            NotifyMode.ADHAN -> NotifyEvent.MAGHRIB
            NotifyMode.SILENT -> NotifyEvent.FAJR
            NotifyMode.VIBRATE -> NotifyEvent.ASR
        }
        NotificationScheduler.scheduleTest(getApplication(), event, mode)
    }

    private suspend fun scheduleNotifications() {
        NotificationScheduler.scheduleAll(getApplication())
    }

    fun citiesFor(country: Country, isArabic: Boolean): List<City> =
        cityRepository.forCountry(country, isArabic)

    fun cityCount(country: Country): Int = cityRepository.countFor(country)

    fun cityByOrder(order: Int): City = cityRepository.byOrder(order)

    private fun minutesNow(): Int = LocalTime.now().toSecondOfDay() / 60
}
