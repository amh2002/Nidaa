package com.mawaqiti.plus

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.AppTheme
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.CityRepository
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.data.SettingsRepository
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEngine
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.notify.AdhanPreview
import com.mawaqiti.plus.notify.AdhanSound
import com.mawaqiti.plus.notify.AdhanSoundProvider
import com.mawaqiti.plus.notify.NotificationScheduler
import com.mawaqiti.plus.notify.NotifyEvent
import com.mawaqiti.plus.notify.NotifyMode
import com.mawaqiti.plus.notify.WidgetUpdater
import com.mawaqiti.plus.ui.theme.GoldenPhase
import com.mawaqiti.plus.ui.theme.PhaseBounds
import com.mawaqiti.plus.ui.theme.resolvePhase
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

    /** beta17: true while the settings sound preview is audible. */
    private val _previewing = MutableStateFlow(false)
    val previewing: StateFlow<Boolean> = _previewing.asStateFlow()

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

    /**
     * Beta 9 "Golden Night": the five-phase look used by the main screen.
     *
     * [phaseOverride] is only fed by the debug intent extra; it is null in
     * release because the call site is behind BuildConfig.DEBUG.
     *
     * beta 10: the persisted [AppTheme] joins the chain — NIGHT pins the
     * deep-night golden palette at every hour, AUTO follows the day.
     */
    private val _goldenPhaseOverride = MutableStateFlow<GoldenPhase?>(null)
    val goldenPhaseOverride: StateFlow<GoldenPhase?> = _goldenPhaseOverride.asStateFlow()

    val goldenPhase: StateFlow<GoldenPhase> =
        combine(day, _nowMinutes, _goldenPhaseOverride, settings) { value, minute, override, stored ->
            override ?: if (stored.theme == AppTheme.NIGHT) {
                GoldenPhase.NIGHT
            } else {
                resolvePhase(minute, PhaseBounds.fromDay(value))
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            resolvePhase(minutesNow(), PhaseBounds.fromDay(initialDay))
        )

    fun setPhaseOverride(phase: GoldenPhase?) {
        _goldenPhaseOverride.value = phase
    }

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

    /** beta10: appearance choice, applied through [goldenPhase]. */
    fun setTheme(theme: AppTheme) = viewModelScope.launch {
        settingsRepository.setTheme(theme)
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

    /**
     * beta14: the adhan sound is read at fire time by [com.mawaqiti.plus.notify.AlarmReceiver],
     * so persisting it is enough - an alarm already scheduled plays the new
     * sound without being rescheduled. Nothing else needs to react.
     */
    fun setAdhanSound(sound: AdhanSound) = viewModelScope.launch {
        settingsRepository.setAdhanSound(sound)
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
    fun setAdhanVolume(percent: Int) = viewModelScope.launch {
        settingsRepository.setAdhanVolume(percent)
    }

    /**
     * beta17: the sound-only preview shown beside the picker.
     *
     * It reads the persisted sound and volume, hands them to [AdhanPreview] -
     * the same provider and the same `USAGE_ALARM` MediaPlayer path the prayer
     * playback uses - and never posts a notification. [previewing] only drives
     * the button label; the audio state itself lives in [AdhanPreview].
     */
    fun toggleAdhanPreview() {
        if (_previewing.value) {
            stopAdhanPreview()
            return
        }
        val current = settings.value
        _previewing.value = true
        AdhanPreview.play(
            context = getApplication(),
            sound = AdhanSoundProvider.resolve(current),
            volumePercent = current.adhanVolume
        ) {
            _previewing.value = false
        }
    }

    fun stopAdhanPreview() {
        AdhanPreview.stop()
        _previewing.value = false
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
