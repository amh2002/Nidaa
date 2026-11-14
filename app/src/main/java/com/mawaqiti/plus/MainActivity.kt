package com.mawaqiti.plus

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.ui.components.isArabicLocale
import com.mawaqiti.plus.ui.screens.CityPickerSheet
import com.mawaqiti.plus.ui.screens.PrayerScreen
import com.mawaqiti.plus.ui.screens.QiblaScreen
import com.mawaqiti.plus.ui.screens.SettingsScreen
import com.mawaqiti.plus.ui.theme.LivingSkyBackground
import com.mawaqiti.plus.ui.theme.MawaqitiTheme
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.nidaa.prayer.update.ReleaseInfo
import com.nidaa.prayer.update.UpdateManager
import com.nidaa.prayer.update.UpdateState
import com.nidaa.prayer.update.UpdatesStore

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MawaqitiViewModel = viewModel()
            val phase by viewModel.skyPhase.collectAsState()
            MawaqitiTheme(phase = phase) {
                MawaqitiApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MawaqitiApp(viewModel: MawaqitiViewModel = viewModel()) {
    val storedSettings by viewModel.settingsState.collectAsState()
    val settings = storedSettings ?: AppSettings()
    val city by viewModel.city.collectAsState()
    val day by viewModel.day.collectAsState()
    val today by viewModel.today.collectAsState()
    val phase by viewModel.skyPhase.collectAsState()
    val isArabic = isArabicLocale()

    // beta 7.95: isolated updater state. The toggle is read from the updater's
    // own DataStore; the banner only appears for an actually newer release.
    val appContext = LocalContext.current.applicationContext
    val updatesStore = remember(appContext) { UpdatesStore(appContext) }
    val updatesEnabled by updatesStore.enabled.collectAsState(initial = true)
    val updateState by UpdateManager.state.collectAsState()
    val updateRelease: ReleaseInfo? = when (val current = updateState) {
        is UpdateState.Available -> current.release
        is UpdateState.Downloading -> current.release
        is UpdateState.ReadyToInstall -> current.release
        else -> null
    }
    LaunchedEffect(appContext) {
        UpdateManager.loadPending(appContext)
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }

    // First launch: pin the app to Arabic explicitly, then persist through
    // AppCompat's autoStoreLocales + our DataStore flag. Using the exact
    // emitted object avoids overwriting a stored English choice.
    LaunchedEffect(storedSettings) {
        val value = storedSettings
        if (value != null && !value.languageInitialized) {
            viewModel.setLanguage(AppLanguage.ARABIC)
        }
    }

    SetSystemBarAppearance(dark = SkyPalette.of(phase).dark)

    // beta 7.9: the user's in-app multiplier composes with the system font
    // scale; settings is observed here, so a change re-renders immediately.
    val systemDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = systemDensity.density,
            fontScale = systemDensity.fontScale * settings.fontScale
        )
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        LivingSkyBackground()

        Scaffold(containerColor = Color.Transparent) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTab) {
                    0 -> PrayerScreen(
                        city = city,
                        day = day,
                        date = today,
                        isArabic = isArabic,
                        clockFormat = settings.clockFormat,
                        onChooseCity = { showCityPicker = true },
                        onOpenQibla = { selectedTab = 1 },
                        onOpenSettings = { selectedTab = 2 },
                        updateRelease = updateRelease,
                        onOpenUpdate = { selectedTab = 2 }
                    )

                    1 -> QiblaScreen(
                        city = city,
                        isArabic = isArabic,
                        onBack = { selectedTab = 0 }
                    )

                    2 -> SettingsScreen(
                        settings = settings,
                        city = city,
                        day = day,
                        isArabic = isArabic,
                        onBack = { selectedTab = 0 },
                        onOpenCityPicker = { showCityPicker = true },
                        onSelectLanguage = viewModel::setLanguage,
                        onSelectClockFormat = viewModel::setClockFormat,
                        onSelectFontScale = viewModel::setFontScale,
                        onSelectWidgetLanguage = viewModel::setWidgetLanguage,
                        onSummerTimeChange = viewModel::setSummerTime,
                        onNotificationsEnabled = viewModel::setNotificationsEnabled,
                        onEventEnabled = viewModel::setEventEnabled,
                        onEventMode = viewModel::setEventMode,
                        onApplyModeToEnabled = viewModel::applyModeToEnabledEvents,
                        onTestEvent = viewModel::testEvent,
                        onTestMode = viewModel::testMode,
                        updatesEnabled = updatesEnabled,
                        updateState = updateState,
                        onUpdatesEnabled = { enabled ->
                            UpdateManager.setEnabled(appContext, enabled)
                        },
                        onCheckForUpdates = { UpdateManager.checkNow(appContext, manual = true) },
                        onDownloadUpdate = { UpdateManager.download(appContext) },
                        onInstallUpdate = { UpdateManager.install(appContext) },
                        onOpenReleasesPage = { UpdateManager.openReleasesPage(appContext) }
                    )
                }
            }
        }
    }

    if (showCityPicker) {
        CityPickerSheet(
            cities = viewModel.cities,
            selectedCity = city,
            isArabic = isArabic,
            onDismiss = { showCityPicker = false },
            onSelect = viewModel::selectCity
        )
    }
    }
}

@Composable
private fun SetSystemBarAppearance(dark: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    val window = context.findActivity()?.window

    SideEffect {
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
