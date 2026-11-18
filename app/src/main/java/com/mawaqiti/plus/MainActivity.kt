package com.mawaqiti.plus

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.mawaqiti.plus.ui.components.DockDestination
import com.mawaqiti.plus.ui.components.NidaaDock
import com.mawaqiti.plus.ui.components.isArabicLocale
import com.mawaqiti.plus.ui.screens.AboutScreen
import com.mawaqiti.plus.ui.screens.CityPickerSheet
import com.mawaqiti.plus.ui.screens.PrayerScreen
import com.mawaqiti.plus.ui.screens.QiblaScreen
import com.mawaqiti.plus.ui.screens.SettingsScreen
import com.mawaqiti.plus.ui.theme.GoldenPalettes
import com.mawaqiti.plus.ui.theme.GoldenPhase
import com.mawaqiti.plus.ui.theme.GoldenSkyBackground
import com.mawaqiti.plus.ui.theme.LocalGoldenPhase
import com.mawaqiti.plus.ui.theme.MawaqitiTheme
import com.nidaa.prayer.update.ReleaseInfo
import com.nidaa.prayer.update.UpdateManager
import com.nidaa.prayer.update.UpdateState
import com.nidaa.prayer.update.UpdatesStore

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel: MawaqitiViewModel by viewModels()
        applyDebugPhase(intent, viewModel)
        setContent {
            val vm: MawaqitiViewModel = viewModel()
            val golden by vm.goldenPhase.collectAsState()
            MawaqitiTheme(paletteOverride = GoldenPalettes.of(golden)) {
                CompositionLocalProvider(LocalGoldenPhase provides golden) {
                    MawaqitiApp(viewModel = vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val viewModel: MawaqitiViewModel by viewModels()
        applyDebugPhase(intent, viewModel)
    }

    /**
     * Debug-only phase pin: `adb shell am start -n com.nidaa.prayer/.MainActivity
     * --es nidaa_phase night|dawn|morning|afternoon|sunset` (or `auto` to clear).
     * The whole function is a no-op in release builds, and the manifest keeps
     * the activity `singleTop`, so re-launching re-delivers the extra through
     * [onNewIntent].
     */
    private fun applyDebugPhase(intent: Intent?, viewModel: MawaqitiViewModel) {
        if (!BuildConfig.DEBUG) return
        val raw = intent?.getStringExtra(EXTRA_PHASE) ?: return
        viewModel.setPhaseOverride(GoldenPhase.fromKey(raw))
    }

    companion object {
        const val EXTRA_PHASE = "nidaa_phase"
    }
}

/**
 * beta12: the only tab that is not a dock destination.
 *
 * Home (0), Qibla (1) and Settings (2) are owned by [DockDestination], which is
 * the single registry for the dock. About is a Home sub-screen parked far above
 * that range so it can never collide with a future dock entry.
 */
private const val TAB_ABOUT = 99

@Composable
fun MawaqitiApp(viewModel: MawaqitiViewModel = viewModel()) {
    val storedSettings by viewModel.settingsState.collectAsState()
    val settings = storedSettings ?: AppSettings()
    val city by viewModel.city.collectAsState()
    val day by viewModel.day.collectAsState()
    val today by viewModel.today.collectAsState()
    val goldenPhase by viewModel.goldenPhase.collectAsState()
    val previewingAdhan by viewModel.previewing.collectAsState()
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

    var selectedTab by rememberSaveable { mutableIntStateOf(DockDestination.HOME.tab) }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = selectedTab != DockDestination.HOME.tab) {
        selectedTab = DockDestination.HOME.tab
    }

    // First launch: pin the app to Arabic explicitly, then persist through
    // AppCompat's autoStoreLocales + our DataStore flag. Using the exact
    // emitted object avoids overwriting a stored English choice.
    LaunchedEffect(storedSettings) {
        val value = storedSettings
        if (value != null && !value.languageInitialized) {
            viewModel.setLanguage(AppLanguage.ARABIC)
        }
    }

    SetSystemBarAppearance(dark = GoldenPalettes.of(goldenPhase).dark)

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
        GoldenSkyBackground()

        Scaffold(
            containerColor = Color.Transparent,
            // beta12: the floating dock lives in the Scaffold's bottomBar slot.
            // The slot is transparent apart from the pill itself, and the dock
            // owns the navigation-bar inset plus its 14 dp bottom margin, so the
            // Scaffold hands that whole height to the screens as content
            // padding: nothing can ever hide behind the dock, including the
            // official-timetable note at the end of the home column.
            bottomBar = {
                // About is a Home sub-screen and keeps the full height (no dock).
                if (selectedTab != TAB_ABOUT) {
                    NidaaDock(
                        selectedTab = selectedTab,
                        onSelect = { destination -> selectedTab = destination.tab }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // beta12: one lookup drives the whole content switch. About is
                // the only screen outside the dock; unknown tabs fall back to
                // Home, so a corrupted saved state can never leave a blank
                // screen or steal a future dock slot.
                if (selectedTab == TAB_ABOUT) {
                    AboutScreen(onBack = { selectedTab = DockDestination.HOME.tab })
                } else when (DockDestination.forTab(selectedTab)) {
                    DockDestination.QIBLA -> QiblaScreen(
                        city = city,
                        isArabic = isArabic,
                        onBack = { selectedTab = DockDestination.HOME.tab }
                    )

                    DockDestination.SETTINGS -> SettingsScreen(
                        settings = settings,
                        city = city,
                        day = day,
                        isArabic = isArabic,
                        onBack = { selectedTab = DockDestination.HOME.tab },
                        onOpenCityPicker = { showCityPicker = true },
                        onSelectLanguage = viewModel::setLanguage,
                        onSelectClockFormat = viewModel::setClockFormat,
                        onSelectFontScale = viewModel::setFontScale,
                        onSelectWidgetLanguage = viewModel::setWidgetLanguage,
                        onSummerTimeChange = viewModel::setSummerTime,
                        onNotificationsEnabled = viewModel::setNotificationsEnabled,
                        onSelectAdhanSound = viewModel::setAdhanSound,
                        onEventEnabled = viewModel::setEventEnabled,
                        onEventMode = viewModel::setEventMode,
                        onApplyModeToEnabled = viewModel::applyModeToEnabledEvents,
                        onAdhanVolumeChange = viewModel::setAdhanVolume,
                        previewingAdhan = previewingAdhan,
                        onToggleAdhanPreview = viewModel::toggleAdhanPreview,
                        onStopAdhanPreview = viewModel::stopAdhanPreview,
                        updatesEnabled = updatesEnabled,
                        updateState = updateState,
                        onUpdatesEnabled = { enabled ->
                            UpdateManager.setEnabled(appContext, enabled)
                        },
                        onCheckForUpdates = { UpdateManager.checkNow(appContext, manual = true) },
                        onDownloadUpdate = { UpdateManager.download(appContext) },
                        onInstallUpdate = { UpdateManager.install(appContext) },
                        onOpenReleasesPage = { UpdateManager.openReleasesPage(appContext) },
                        onSelectTheme = viewModel::setTheme
                    )

                    DockDestination.HOME, null -> PrayerScreen(
                        city = city,
                        day = day,
                        date = today,
                        isArabic = isArabic,
                        clockFormat = settings.clockFormat,
                        onChooseCity = { showCityPicker = true },
                        onOpenAbout = { selectedTab = TAB_ABOUT },
                        updateRelease = updateRelease,
                        onOpenUpdate = { selectedTab = DockDestination.SETTINGS.tab }
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
