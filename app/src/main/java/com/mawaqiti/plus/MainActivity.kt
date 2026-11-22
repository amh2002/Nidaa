package com.mawaqiti.plus

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.LayoutMode
import com.mawaqiti.plus.ui.components.DockDestination
import com.mawaqiti.plus.ui.components.NidaaDock
import com.mawaqiti.plus.ui.components.NidaaSplash
import com.mawaqiti.plus.ui.components.isArabicLocale
import com.mawaqiti.plus.ui.screens.AboutDialog
import com.mawaqiti.plus.ui.screens.CityPickerSheet
import com.mawaqiti.plus.ui.screens.PrayerScreen
import com.mawaqiti.plus.ui.screens.QiblaScreen
import com.mawaqiti.plus.ui.screens.SettingsScreen
import com.mawaqiti.plus.ui.screens.TvHomeScreen
import com.mawaqiti.plus.ui.theme.GoldenPalettes
import com.mawaqiti.plus.ui.theme.GoldenPhase
import com.mawaqiti.plus.ui.theme.GoldenSkyBackground
import com.mawaqiti.plus.ui.theme.LocalGoldenPhase
import com.mawaqiti.plus.ui.theme.LocalStarExclusions
import com.mawaqiti.plus.ui.theme.MawaqitiTheme
import com.mawaqiti.plus.ui.theme.NidaaDimens
import com.mawaqiti.plus.ui.theme.StarExclusions
import com.nidaa.prayer.update.ReleaseInfo
import com.nidaa.prayer.update.UpdateManager
import com.nidaa.prayer.update.UpdateState
import com.nidaa.prayer.update.UpdatesStore

class MainActivity : AppCompatActivity() {

    /** Incremented for every update-notification / banner deep link. */
    private var openUpdatesSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleUpdateIntent(intent)
        val viewModel: MawaqitiViewModel by viewModels()
        applyDebugPhase(intent, viewModel)
        applyDebugLayout(intent, viewModel)
        setContent {
            val vm: MawaqitiViewModel = viewModel()
            val golden by vm.goldenPhase.collectAsState()
            // beta23: the splash is a pure overlay on top of an already composed
            // app, so the ViewModel, the settings read and the first table
            // lookup all run while it is visible. rememberSaveable keeps it from
            // replaying on a configuration change.
            var showSplash by rememberSaveable { mutableStateOf(true) }
            // beta24: one registry shared by the star field and every screen, so
            // no night star can land inside the 24 dp margin of a text box.
            val starExclusions = remember { StarExclusions() }
            MawaqitiTheme(paletteOverride = GoldenPalettes.of(golden)) {
                CompositionLocalProvider(
                    LocalGoldenPhase provides golden,
                    LocalStarExclusions provides starExclusions
                ) {
                    Box(Modifier.fillMaxSize()) {
                        MawaqitiApp(
                            viewModel = vm,
                            openUpdatesSignal = openUpdatesSignal
                        )
                        if (showSplash) {
                            NidaaSplash(onFinish = { showSplash = false })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
        val viewModel: MawaqitiViewModel by viewModels()
        applyDebugPhase(intent, viewModel)
        applyDebugLayout(intent, viewModel)
    }

    override fun onResume() {
        super.onResume()
        // Returning from the "install unknown apps" system page resumes the
        // install that was waiting for permission.
        UpdateManager.resumeAfterPermission(this)
    }

    /** Update notifications and the home banner deep-link to «التحديثات». */
    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATES, false) == true) {
            intent.removeExtra(EXTRA_OPEN_UPDATES)
            openUpdatesSignal++
        }
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

    /**
     * beta23 debug-only layout pin, used to capture the TV dashboard without
     * tapping through Settings:
     * `adb shell am start -n com.nidaa.prayer/.MainActivity --es nidaa_layout tv`
     * (or `mobile`). No-op in release builds, exactly like [applyDebugPhase].
     */
    private fun applyDebugLayout(intent: Intent?, viewModel: MawaqitiViewModel) {
        if (!BuildConfig.DEBUG) return
        val raw = intent?.getStringExtra(EXTRA_LAYOUT) ?: return
        val mode = if (raw.equals("tv", ignoreCase = true)) LayoutMode.TV else LayoutMode.MOBILE
        viewModel.setLayoutMode(mode)
    }

    companion object {
        const val EXTRA_PHASE = "nidaa_phase"

        /** Debug-only layout pin (`tv` / `mobile`). */
        const val EXTRA_LAYOUT = "nidaa_layout"

        /** Explicit deep link from update notifications into «التحديثات». */
        const val EXTRA_OPEN_UPDATES = "nidaa_open_updates"
    }
}

@Composable
fun MawaqitiApp(
    viewModel: MawaqitiViewModel = viewModel(),
    openUpdatesSignal: Int = 0
) {
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
    val activityContext = LocalContext.current
    val appContext = activityContext.applicationContext
    val updatesStore = remember(appContext) { UpdatesStore(appContext) }
    val updatesEnabled by updatesStore.enabled.collectAsState(initial = true)
    val lastInstall by updatesStore.lastInstallAttempt.collectAsState(initial = null)
    val updateState by UpdateManager.state.collectAsState()
    val permissionPrompt by UpdateManager.permissionPrompt.collectAsState()
    val updateRelease: ReleaseInfo? = when (val current = updateState) {
        is UpdateState.Available -> current.release
        is UpdateState.Downloading -> current.release
        is UpdateState.ReadyToInstall -> current.release
        is UpdateState.NeedsPermission -> current.release
        is UpdateState.Installing -> current.release
        else -> null
    }
    LaunchedEffect(appContext) {
        UpdateManager.loadPending(appContext)
    }

    var permissionDialogFor by remember { mutableStateOf(0L) }
    LaunchedEffect(permissionPrompt) {
        if (permissionPrompt > 0) permissionDialogFor = permissionPrompt
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(DockDestination.HOME.tab) }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }
    // beta24: the About dialog replaces the beta10 About screen.
    var showAbout by rememberSaveable { mutableStateOf(false) }

    // Notification / banner deep link: land on Settings, where «التحديثات» lives.
    LaunchedEffect(openUpdatesSignal) {
        if (openUpdatesSignal > 0) {
            showCityPicker = false
            selectedTab = DockDestination.SETTINGS.tab
        }
    }

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
    ApplyLayoutOrientation(settings.layoutMode)

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
                // beta24: About is a dialog over the home screen now, so the
                // dock is present on every tab and no screen loses its height.
                NidaaDock(
                    selectedTab = selectedTab,
                    onSelect = { destination -> selectedTab = destination.tab }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // beta12: one lookup drives the whole content switch; unknown
                // tabs fall back to Home, so a corrupted saved state can never
                // leave a blank screen or steal a future dock slot. beta24 took
                // About out of this switch - it is a dialog now.
                when (DockDestination.forTab(selectedTab)) {
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
                        onInstallUpdate = { UpdateManager.install(activityContext) },
                        onOpenReleasesPage = { UpdateManager.openReleasesPage(appContext) },
                        lastInstallAttempt = lastInstall,
                        onOpenInstallSettings = {
                            UpdateManager.openPermissionSettings(activityContext)
                        },
                        onSelectTheme = viewModel::setTheme,
                        onSelectLayout = viewModel::setLayoutMode,
                        onSelectPreAlert = viewModel::setPreAlertMinutes,
                        onSelectHomeDhikr = viewModel::setHomeDhikr,
                        onSelectHomeCountdown = viewModel::setHomeCountdown,
                        onSelectHomeMoonPhase = viewModel::setHomeMoonPhase
                    )

                    DockDestination.HOME, null -> {
                        // beta21: the TV dashboard (and its landscape orientation
                        // hint) lives only in TV mode. Below 600 dp of *window*
                        // width - e.g. a phone in portrait - we fall back to the
                        // unchanged mobile home, so nothing can overlap. Mobile
                        // mode never reaches this branch at all.
                        val home: @Composable () -> Unit = {
                            PrayerScreen(
                                city = city,
                                day = day,
                                date = today,
                                isArabic = isArabic,
                                clockFormat = settings.clockFormat,
                                onChooseCity = { showCityPicker = true },
                                onOpenAbout = { showAbout = true },
                                updateRelease = updateRelease,
                                onOpenUpdate = { selectedTab = DockDestination.SETTINGS.tab },
                                // beta25: the adaptive content slot reads the same
                                // settings flow as the rest of the app.
                                homeDhikr = settings.homeDhikr,
                                homeCountdown = settings.homeCountdown,
                                homeMoonPhase = settings.homeMoonPhase
                            )
                        }
                        if (settings.layoutMode == LayoutMode.TV) {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                if (maxWidth >= TV_MIN_WIDTH) {
                                    TvHomeScreen(
                                        city = city,
                                        day = day,
                                        date = today,
                                        isArabic = isArabic,
                                        clockFormat = settings.clockFormat,
                                        onChooseCity = { showCityPicker = true },
                                        onOpenAbout = { showAbout = true },
                                        updateRelease = updateRelease,
                                        onOpenUpdate = {
                                            selectedTab = DockDestination.SETTINGS.tab
                                        }
                                    )
                                } else {
                                    home()
                                }
                            }
                        } else {
                            home()
                        }
                    }
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

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    if (permissionDialogFor > 0) {
        AlertDialog(
            onDismissRequest = { permissionDialogFor = 0L },
            title = { Text(stringResource(R.string.update_permission_title)) },
            text = { Text(stringResource(R.string.update_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    permissionDialogFor = 0L
                    UpdateManager.openPermissionSettings(activityContext)
                }) {
                    Text(stringResource(R.string.update_permission_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    permissionDialogFor = 0L
                    UpdateManager.openReleasesPage(activityContext)
                }) {
                    Text(stringResource(R.string.update_permission_open_releases))
                }
            }
        )
    }
    }
}

/**
 * beta21: the minimum *window* width for the TV dashboard.
 *
 * 600 dp is the platform's "large screen" line. A narrower window - notably a
 * phone held in portrait while TV mode is selected - keeps the unchanged mobile
 * home, so the landscape composition can never be squeezed into an overlap.
 */
private val TV_MIN_WIDTH = NidaaDimens.tvDashboardMinWidth

/**
 * beta21: applies the orientation policy of the selected layout.
 *
 * - جوال: exactly the manifest's portrait lock (the setter is skipped because
 *   the activity already reports PORTRAIT, so mobile behaviour is untouched).
 * - تلفاز on a television or a large screen: prefer landscape, rotating with
 *   the sensor so both landscape sides work.
 * - تلفاز on a phone: no lock at all; the user can rotate, and the home falls
 *   back to the mobile layout whenever the window is too narrow.
 */
@Composable
private fun ApplyLayoutOrientation(layoutMode: LayoutMode) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val configuration = LocalConfiguration.current
    val smallestWidth = configuration.smallestScreenWidthDp
    val isTelevision =
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION

    LaunchedEffect(layoutMode, activity, smallestWidth, isTelevision) {
        val host = activity ?: return@LaunchedEffect
        val requested = when {
            layoutMode == LayoutMode.MOBILE -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isTelevision || smallestWidth >= 600 ->
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        if (host.requestedOrientation != requested) {
            host.requestedOrientation = requested
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
