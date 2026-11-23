package com.mawaqiti.plus.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mawaqiti.plus.BuildConfig
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.AppTheme
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.data.FontScale
import com.mawaqiti.plus.data.LayoutMode
import com.mawaqiti.plus.data.PreAlertMinutes
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.TimeFormatter
import com.mawaqiti.plus.notify.NotificationScheduler
import com.mawaqiti.plus.notify.NotifyEvent
import com.mawaqiti.plus.notify.NotifyMode
import com.mawaqiti.plus.notify.AdhanSound
import com.mawaqiti.plus.notify.AdhanVolume
import com.mawaqiti.plus.ui.components.AdhanSoundDropdown
import com.mawaqiti.plus.ui.components.GlassCard
import com.mawaqiti.plus.ui.components.InfoChip
import com.mawaqiti.plus.ui.components.PillOption
import com.mawaqiti.plus.ui.components.SegmentedPillRow
import com.mawaqiti.plus.ui.components.SettingsCard
import com.mawaqiti.plus.ui.components.SettingsSkyAmbient
import com.mawaqiti.plus.ui.components.SettingsVerse
import com.mawaqiti.plus.ui.components.SettingRow
import com.mawaqiti.plus.ui.components.SettingsSmallButton
import com.mawaqiti.plus.ui.components.SettingsToggleRow
import com.mawaqiti.plus.ui.components.mawaqitiSwitchColors
import com.mawaqiti.plus.ui.theme.NidaaDimens
import com.mawaqiti.plus.ui.theme.skyPalette
import com.nidaa.prayer.update.InstallAttempt
import com.nidaa.prayer.update.UpdateState
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

/**
 * beta25: the five collapsible Settings sections.
 *
 * Declaration order is the on-screen order. Every existing setting lives in
 * exactly one of them (see the beta25 report for the item-by-item mapping);
 * each header is a plain [GlassCard] row so the accordion inherits the same
 * glass surface, border and corner tokens as every other card in the list.
 */
enum class SettingsSection(
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    TIMES(R.string.settings_section_times, Icons.Filled.Schedule),
    ALERTS(R.string.settings_section_alerts, Icons.Filled.Notifications),
    HOME_CONTENT(R.string.settings_section_home_content, Icons.Filled.Widgets),
    APPEARANCE(R.string.settings_section_appearance, Icons.Filled.Palette),
    ABOUT(R.string.settings_section_about, Icons.Filled.Info)
}

/**
 * Settings entry point.
 *
 * This composable only owns Android side effects (permission launchers,
 * system-setting intents, live status). The visual body is [SettingsList],
 * which is a single scrollable list of framework items and is what the
 * overlap regression test renders.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    city: City,
    day: TableDay,
    isArabic: Boolean,
    onBack: () -> Unit,
    onOpenCityPicker: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectClockFormat: (ClockFormat) -> Unit,
    onSelectFontScale: (Float) -> Unit,
    onSelectWidgetLanguage: (AppLanguage) -> Unit,
    onSummerTimeChange: (Boolean) -> Unit,
    onNotificationsEnabled: (Boolean) -> Unit,
    /** beta14: the picked adhan; persisted and read by the playback service. */
    onSelectAdhanSound: (AdhanSound) -> Unit = {},
    onEventEnabled: (NotifyEvent, Boolean) -> Unit,
    onEventMode: (NotifyEvent, NotifyMode) -> Unit,
    onApplyModeToEnabled: (NotifyMode) -> Unit,
    /** beta17: persisted adhan loudness percentage (0…100). */
    onAdhanVolumeChange: (Int) -> Unit = {},
    /** beta17: true while the sound-only preview is audible. */
    previewingAdhan: Boolean = false,
    onToggleAdhanPreview: () -> Unit = {},
    onStopAdhanPreview: () -> Unit = {},
    updatesEnabled: Boolean = true,
    updateState: UpdateState = UpdateState.Idle,
    onUpdatesEnabled: (Boolean) -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onOpenReleasesPage: () -> Unit = {},
    lastInstallAttempt: InstallAttempt? = null,
    onOpenInstallSettings: () -> Unit = {},
    /** beta10: persisted appearance choice; AUTO is the default. */
    onSelectTheme: (AppTheme) -> Unit = {},
    /** beta21: persisted display layout; MOBILE is the default. */
    onSelectLayout: (LayoutMode) -> Unit = {},
    /** beta22: persisted pre-prayer lead time in minutes; 0 disables it. */
    onSelectPreAlert: (Int) -> Unit = {},
    /** beta25: the three home content widgets; dhikr defaults to ON. */
    onSelectHomeDhikr: (Boolean) -> Unit = {},
    onSelectHomeCountdown: (Boolean) -> Unit = {},
    onSelectHomeMoonPhase: (Boolean) -> Unit = {},
    /** beta30: home-only Arabic-Indic digits (`use_arabic_indic_numerals`). */
    onSelectArabicIndic: (Boolean) -> Unit = {},
    /** beta30: opens the lightweight «الأذكار» screen. */
    onOpenAdhkar: () -> Unit = {}
) {
    val context = LocalContext.current
    var statusTick by remember { mutableIntStateOf(0) }

    // Re-read system permission state when returning from system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) statusTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val canExact = remember(statusTick) { NotificationScheduler.canScheduleExact(context) }
    val notificationsGranted = remember(statusTick) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val batteryOptimized = remember(statusTick) {
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName)
            ?.not() ?: false
    }

    var showRationale by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { statusTick++ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showRationale = true
        }
    }

    // beta26: the ambient layer is a sibling of the list, never a child of
    // it, so it is painted behind every card and can never take a touch.
    // `sectionsCollapsed` mirrors the accordion state reported by [SettingsList].
    var sectionsCollapsed by remember { mutableStateOf(true) }
    // beta27: the two bands the ambient layer needs from the real layout - the
    // bottom of the collapsed header stack and the top of the verse block (both
    // in root pixels, 0 while unknown).
    var headerStackBottom by remember { mutableFloatStateOf(0f) }
    var verseTop by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (sectionsCollapsed) {
            SettingsSkyAmbient(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("settings_ambient"),
                contentTopPx = headerStackBottom,
                contentBottomPx = verseTop
            )
        }
        if (sectionsCollapsed) {
            SettingsVerse(
                headerBottomPx = headerStackBottom,
                onTopChange = { verseTop = it },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("settings_verse")
            )
        }

        SettingsList(
            settings = settings,
            city = city,
            day = day,
            isArabic = isArabic,
            canScheduleExact = canExact,
            notificationsGranted = notificationsGranted,
            batteryOptimized = batteryOptimized,
            onBack = onBack,
            onOpenCityPicker = onOpenCityPicker,
            onSelectLanguage = onSelectLanguage,
            onSelectClockFormat = onSelectClockFormat,
            onSelectFontScale = onSelectFontScale,
            onSelectWidgetLanguage = onSelectWidgetLanguage,
            onSummerTimeChange = onSummerTimeChange,
            onNotificationsEnabled = onNotificationsEnabled,
            onSelectAdhanSound = onSelectAdhanSound,
            onEventEnabled = onEventEnabled,
            onEventMode = onEventMode,
            onApplyModeToEnabled = onApplyModeToEnabled,
            onAdhanVolumeChange = onAdhanVolumeChange,
            previewingAdhan = previewingAdhan,
            onToggleAdhanPreview = onToggleAdhanPreview,
            onStopAdhanPreview = onStopAdhanPreview,
            onOpenExactAlarmSettings = { NotificationScheduler.openExactAlarmSettings(context) },
            onOpenNotificationSettings = { openNotificationSettings(context) },
            onOpenBatterySettings = { openBatterySettings(context) },
            updatesEnabled = updatesEnabled,
            updateState = updateState,
            onUpdatesEnabled = onUpdatesEnabled,
            onCheckForUpdates = onCheckForUpdates,
            onDownloadUpdate = onDownloadUpdate,
            onInstallUpdate = onInstallUpdate,
            onOpenReleasesPage = onOpenReleasesPage,
            lastInstallAttempt = lastInstallAttempt,
            onOpenInstallSettings = onOpenInstallSettings,
            onSelectTheme = onSelectTheme,
            onSelectLayout = onSelectLayout,
            onSelectPreAlert = onSelectPreAlert,
            onSelectHomeDhikr = onSelectHomeDhikr,
            onSelectHomeCountdown = onSelectHomeCountdown,
            onSelectHomeMoonPhase = onSelectHomeMoonPhase,
            onSelectArabicIndic = onSelectArabicIndic,
            onOpenAdhkar = onOpenAdhkar,
            onSectionsCollapsedChange = { sectionsCollapsed = it },
            onLastHeaderBottomChange = { headerStackBottom = it }
        )
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.notify_rationale_title)) },
            text = { Text(stringResource(R.string.notify_rationale_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) {
                    Text(stringResource(R.string.notify_rationale_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(stringResource(R.string.notify_rationale_later))
                }
            }
        )
    }
}

@Composable
fun SettingsList(
    settings: AppSettings,
    city: City,
    day: TableDay,
    isArabic: Boolean,
    canScheduleExact: Boolean,
    notificationsGranted: Boolean,
    batteryOptimized: Boolean,
    onBack: () -> Unit,
    onOpenCityPicker: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectClockFormat: (ClockFormat) -> Unit,
    onSelectFontScale: (Float) -> Unit,
    onSelectWidgetLanguage: (AppLanguage) -> Unit,
    onSummerTimeChange: (Boolean) -> Unit,
    onNotificationsEnabled: (Boolean) -> Unit,
    /** beta14: the picked adhan; persisted and read by the playback service. */
    onSelectAdhanSound: (AdhanSound) -> Unit = {},
    onEventEnabled: (NotifyEvent, Boolean) -> Unit,
    onEventMode: (NotifyEvent, NotifyMode) -> Unit,
    onApplyModeToEnabled: (NotifyMode) -> Unit,
    /** beta17: persisted adhan loudness percentage (0…100). */
    onAdhanVolumeChange: (Int) -> Unit = {},
    /** beta17: true while the sound-only preview is audible. */
    previewingAdhan: Boolean = false,
    onToggleAdhanPreview: () -> Unit = {},
    onStopAdhanPreview: () -> Unit = {},
    onOpenExactAlarmSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    updatesEnabled: Boolean = true,
    updateState: UpdateState = UpdateState.Idle,
    onUpdatesEnabled: (Boolean) -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onOpenReleasesPage: () -> Unit = {},
    lastInstallAttempt: InstallAttempt? = null,
    onOpenInstallSettings: () -> Unit = {},
    /** beta10: appearance choice (see [SettingsScreen]). */
    onSelectTheme: (AppTheme) -> Unit = {},
    /** beta21: display layout choice (see [SettingsScreen]). */
    onSelectLayout: (LayoutMode) -> Unit = {},
    /** beta22: pre-prayer lead time in minutes (see [SettingsScreen]). */
    onSelectPreAlert: (Int) -> Unit = {},
    /** beta25: home content widgets (see [SettingsScreen]). */
    onSelectHomeDhikr: (Boolean) -> Unit = {},
    onSelectHomeCountdown: (Boolean) -> Unit = {},
    onSelectHomeMoonPhase: (Boolean) -> Unit = {},
    /** beta30: home-only Arabic-Indic digits (see [SettingsScreen]). */
    onSelectArabicIndic: (Boolean) -> Unit = {},
    /** beta30: opens the «الأذكار» screen (see [SettingsScreen]). */
    onOpenAdhkar: () -> Unit = {},
    /** beta26: true while every accordion section is closed. */
    onSectionsCollapsedChange: (Boolean) -> Unit = {},
    /** beta27: bottom edge of the collapsed header stack, in root pixels. */
    onLastHeaderBottomChange: (Float) -> Unit = {},
    /**
     * Test/deep-link hatch: open every section on the first composition.
     * The app always passes the default, so a real entry starts collapsed.
     */
    initiallyExpanded: Boolean = false
) {
    val palette = skyPalette
    val previewMinutes = nextPreviewMinutes(day)
    val preview = TimeFormatter.format(previewMinutes, settings.clockFormat, isArabic)
    // beta17: leaving Settings must never leave the preview audible, and the
    // preview must never outlive the screen that started it.
    DisposableEffect(Unit) {
        onDispose { onStopAdhanPreview() }
    }
    val notifications = settings.notifications
    val masterEnabled = notifications.masterEnabled
    val modes = remember { listOf(NotifyMode.ADHAN, NotifyMode.SILENT, NotifyMode.VIBRATE) }
    var applyMode by remember { mutableStateOf(NotifyMode.ADHAN) }
    val widgetLanguage = settings.effectiveWidgetLanguage

    // beta25: accordion state. `remember` (not rememberSaveable) is deliberate:
    // every entry into Settings creates a fresh state, so all sections start
    // collapsed as the specification requires. Several sections may be open at
    // the same time - the map holds one flag per header.
    val expanded = remember {
        mutableStateMapOf<SettingsSection, Boolean>().apply {
            if (initiallyExpanded) {
                SettingsSection.entries.forEach { put(it, true) }
            }
        }
    }

    // beta26: the collapsed state drives the ambient layer of the screen. One
    // frame after a header is tapped this reports `false`, so the layer (and
    // both of its animations) leaves the composition; collapsing the last
    // section brings it straight back.
    // `none`, not `isEmpty`: the toggle above stores `false` for a closed
    // section, so the map keeps its keys and never empties again.
    val allCollapsed = expanded.values.none { it }
    LaunchedEffect(allCollapsed) { onSectionsCollapsedChange(allCollapsed) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .testTag("settings_list")
    ) {
        ScreenTopBar(title = stringResource(R.string.settings_title), palette = palette, onBack = onBack)

        SettingsSection.entries.forEach { section ->
            SettingsSectionBlock(
                section = section,
                expanded = expanded[section] == true,
                onToggle = { expanded[section] = expanded[section] != true },
                modifier = Modifier
                    .testTag("settings_section_${section.name.lowercase()}")
                    // beta27: the ABOUT block is the last of the stack, so its
                    // bottom edge is the top of the Settings open sky.
                    .then(
                        if (section == SettingsSection.ABOUT) {
                            Modifier.onGloballyPositioned {
                                onLastHeaderBottomChange(it.boundsInRoot().bottom)
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                when (section) {
                    SettingsSection.TIMES -> {
                        // 1. City
                        SettingsCard(
                            contentPadding = PaddingValues(0.dp),
                            onClick = onOpenCityPicker
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_city_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textMuted
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = city.label(isArabic),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.text
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.settings_city_change),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.accent
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = palette.accent
                                )
                            }
                        }

                        ItemGap()

                        // 2. Clock format (the live preview is built from the
                        //    same day the home uses).
                        SettingsCard {
                            SettingRowBlock(
                                title = stringResource(R.string.settings_clock_title),
                                description = stringResource(R.string.clock_preview) + ": " + preview
                            ) {
                                SegmentedPillRow(
                                    options = listOf(
                                        PillOption(
                                            stringResource(R.string.clock_24),
                                            settings.clockFormat == ClockFormat.H24
                                        ),
                                        PillOption(
                                            stringResource(R.string.clock_12),
                                            settings.clockFormat == ClockFormat.H12
                                        )
                                    ),
                                    onSelect = { index ->
                                        onSelectClockFormat(
                                            if (index == 0) ClockFormat.H24 else ClockFormat.H12
                                        )
                                    }
                                )
                            }
                        }

                        // 3. Summer time: Palestine only.
                        if (city.country == Country.PS) {
                            ItemGap()
                            SettingsCard {
                                SettingsToggleRow(
                                    title = stringResource(R.string.settings_summer_time),
                                    description = stringResource(R.string.summer_time_desc),
                                    checked = settings.summerTime,
                                    onCheckedChange = onSummerTimeChange
                                )
                            }
                        }
                    }

                    SettingsSection.ALERTS -> {
                        // 5b. Notifications master switch (beta18: first item of
                        //     the group so the on/off control is seen first).
                        SettingsCard(
                            title = stringResource(R.string.settings_notifications),
                            description = stringResource(R.string.settings_notifications_desc),
                            modifier = Modifier.testTag("settings_notifications_master_card")
                        ) {
                            SettingsToggleRow(
                                title = stringResource(R.string.notifications_master),
                                description = stringResource(R.string.notifications_master_desc),
                                checked = notifications.masterEnabled,
                                onCheckedChange = onNotificationsEnabled,
                                modifier = Modifier.testTag("notifications_master_toggle"),
                                toggleTestTag = "notifications_master_switch"
                            )
                            if (!masterEnabled) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.notifications_off_caption),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.accent,
                                    modifier = Modifier.testTag("notifications_off_caption")
                                )
                            }
                        }

                        ItemGap()

                        // 5c. Adhan sound (beta14) + beta17 volume.
                        SettingsCard(
                            title = stringResource(R.string.settings_adhan_sound_title),
                            description = stringResource(R.string.settings_adhan_sound_desc),
                            modifier = Modifier.testTag("settings_adhan_sound")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(NidaaDimens.adhanPickerGap),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    AdhanSoundDropdown(
                                        selected = settings.adhanSound,
                                        onSelect = {
                                            // A stale preview would contradict the new pick.
                                            onStopAdhanPreview()
                                            onSelectAdhanSound(it)
                                        }
                                    )
                                }
                                SettingsSmallButton(
                                    label = stringResource(
                                        if (previewingAdhan) R.string.settings_adhan_test_stop
                                        else R.string.settings_adhan_test
                                    ),
                                    onClick = onToggleAdhanPreview,
                                    modifier = Modifier.testTag("settings_adhan_test_button")
                                )
                            }

                            Spacer(Modifier.height(NidaaDimens.adhanVolumeGap))

                            AdhanVolumeRow(
                                volume = settings.adhanVolume,
                                onChange = onAdhanVolumeChange,
                                onCommit = {
                                    // Restarting the preview is the caller's business;
                                    // stopping it here keeps the audible track in step.
                                    if (previewingAdhan) onStopAdhanPreview()
                                }
                            )
                        }

                        ItemGap()

                        // 6b. Apply one mode to every enabled event.
                        SettingsCard(
                            title = stringResource(R.string.notify_apply_all),
                            description = stringResource(R.string.notify_apply_all_desc),
                            modifier = Modifier.testTag("notify_apply_all")
                        ) {
                            SegmentedPillRow(
                                options = modes.map { mode -> PillOption(modeLabel(mode), mode == applyMode) },
                                onSelect = { index -> applyMode = modes[index] },
                                fillWidth = true
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                SettingsSmallButton(
                                    label = stringResource(R.string.notify_apply),
                                    onClick = { onApplyModeToEnabled(applyMode) }
                                )
                            }
                        }

                        // 7. One card per event, each with its own switch and mode.
                        for (event in NotifyEvent.entries) {
                            ItemGap()
                            val current = notifications.forEvent(event)
                            SettingsCard(
                                enabled = masterEnabled,
                                modifier = Modifier.testTag("event_card_${event.storageKey}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = eventLabel(event),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.text,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = current.enabled,
                                        onCheckedChange = { onEventEnabled(event, it) },
                                        enabled = masterEnabled,
                                        modifier = Modifier.testTag("event_toggle_${event.storageKey}"),
                                        colors = mawaqitiSwitchColors()
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                SegmentedPillRow(
                                    options = modes.map { mode ->
                                        PillOption(
                                            modeLabel(mode),
                                            mode == current.mode,
                                            testTag = "event_mode_${event.storageKey}_${mode.storageKey}"
                                        )
                                    },
                                    onSelect = { index -> onEventMode(event, modes[index]) },
                                    fillWidth = true,
                                    enabled = masterEnabled,
                                    modifier = Modifier.testTag("event_modes_${event.storageKey}")
                                )
                            }
                        }

                        // 8. Pre-prayer alert (beta22).
                        ItemGap()
                        SettingsCard(
                            title = stringResource(R.string.settings_pre_alert_title),
                            description = stringResource(R.string.settings_pre_alert_desc),
                            modifier = Modifier.testTag("settings_pre_alert_card")
                        ) {
                            val preAlertLabels = listOf(
                                R.string.pre_alert_off,
                                R.string.pre_alert_5,
                                R.string.pre_alert_10,
                                R.string.pre_alert_15,
                                R.string.pre_alert_30
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SegmentedPillRow(
                                    options = PreAlertMinutes.OPTIONS.take(3).mapIndexed { index, minutes ->
                                        PillOption(
                                            stringResource(preAlertLabels[index]),
                                            settings.preAlertMinutes == minutes,
                                            testTag = "pre_alert_option_$minutes"
                                        )
                                    },
                                    onSelect = { index -> onSelectPreAlert(PreAlertMinutes.OPTIONS[index]) },
                                    selectedColor = palette.accent,
                                    selectedTextColor = palette.sheet,
                                    fillWidth = true
                                )
                                SegmentedPillRow(
                                    options = PreAlertMinutes.OPTIONS.drop(3).mapIndexed { index, minutes ->
                                        PillOption(
                                            stringResource(preAlertLabels[3 + index]),
                                            settings.preAlertMinutes == minutes,
                                            testTag = "pre_alert_option_$minutes"
                                        )
                                    },
                                    onSelect = { index ->
                                        onSelectPreAlert(PreAlertMinutes.OPTIONS[3 + index])
                                    },
                                    selectedColor = palette.accent,
                                    selectedTextColor = palette.sheet,
                                    fillWidth = true
                                )
                            }
                        }

                        // 9. System status rows, only when they need attention.
                        var anyStatusCard = false
                        if (!canScheduleExact) {
                            ItemGap()
                            StatusCard(
                                title = stringResource(R.string.notify_exact_title),
                                description = stringResource(R.string.notify_exact_desc),
                                onClick = onOpenExactAlarmSettings,
                                testTag = "status_exact_card"
                            )
                            anyStatusCard = true
                        }
                        if (!notificationsGranted) {
                            ItemGap()
                            StatusCard(
                                title = stringResource(R.string.notify_permission_title),
                                description = stringResource(R.string.notify_permission_desc),
                                onClick = onOpenNotificationSettings,
                                testTag = "status_permission_card"
                            )
                            anyStatusCard = true
                        }
                        if (batteryOptimized) {
                            ItemGap()
                            StatusCard(
                                title = stringResource(R.string.notify_battery_title),
                                description = stringResource(R.string.notify_battery_desc),
                                onClick = onOpenBatterySettings,
                                testTag = "status_battery_card"
                            )
                            anyStatusCard = true
                        }
                        if (anyStatusCard) {
                            ItemGap()
                        }
                    }

                    SettingsSection.HOME_CONTENT -> {
                        // beta25 (Task 5): three switches over the existing
                        // DataStore keys; the home observes the same flow, so a
                        // change applies on return without a restart.
                        SettingsCard(
                            description = stringResource(R.string.home_content_desc),
                            modifier = Modifier.testTag("settings_home_content_card")
                        ) {
                            SettingsToggleRow(
                                title = stringResource(R.string.home_widget_dhikr),
                                description = stringResource(R.string.home_widget_dhikr_desc),
                                checked = settings.homeDhikr,
                                onCheckedChange = onSelectHomeDhikr,
                                modifier = Modifier.testTag("home_dhikr_toggle"),
                                toggleTestTag = "home_dhikr_switch"
                            )
                            Spacer(Modifier.height(10.dp))
                            SettingsToggleRow(
                                title = stringResource(R.string.home_widget_countdown),
                                description = stringResource(R.string.home_widget_countdown_desc),
                                checked = settings.homeCountdown,
                                onCheckedChange = onSelectHomeCountdown,
                                modifier = Modifier.testTag("home_countdown_toggle"),
                                toggleTestTag = "home_countdown_switch"
                            )
                            Spacer(Modifier.height(10.dp))
                            SettingsToggleRow(
                                title = stringResource(R.string.home_widget_moon),
                                description = stringResource(R.string.home_widget_moon_desc),
                                checked = settings.homeMoonPhase,
                                onCheckedChange = onSelectHomeMoonPhase,
                                modifier = Modifier.testTag("home_moon_toggle"),
                                toggleTestTag = "home_moon_switch"
                            )
                        }
                    }

                    SettingsSection.APPEARANCE -> {
                        // 2. Language
                        SettingsCard {
                            SettingRowBlock(
                                title = stringResource(R.string.settings_language),
                                description = stringResource(R.string.language_desc)
                            ) {
                                SegmentedPillRow(
                                    options = listOf(
                                        PillOption(stringResource(R.string.language_arabic), isArabic),
                                        PillOption(stringResource(R.string.language_english), !isArabic)
                                    ),
                                    onSelect = { index ->
                                        onSelectLanguage(
                                            if (index == 0) AppLanguage.ARABIC else AppLanguage.ENGLISH
                                        )
                                    }
                                )
                            }
                        }

                        ItemGap()

                        // 3a. Appearance (beta10).
                        SettingsCard {
                            SettingRowBlock(
                                title = stringResource(R.string.settings_theme_title),
                                description = stringResource(R.string.settings_theme_desc)
                            ) {
                                SegmentedPillRow(
                                    options = listOf(
                                        PillOption(
                                            stringResource(R.string.theme_auto),
                                            settings.theme == AppTheme.AUTO
                                        ),
                                        PillOption(
                                            stringResource(R.string.theme_night),
                                            settings.theme == AppTheme.NIGHT
                                        )
                                    ),
                                    onSelect = { index ->
                                        onSelectTheme(if (index == 0) AppTheme.AUTO else AppTheme.NIGHT)
                                    },
                                    selectedColor = palette.accent,
                                    selectedTextColor = palette.sheet
                                )
                            }
                        }

                        ItemGap()

                        // 3a-2. Display layout (beta21).
                        SettingsCard(
                            title = stringResource(R.string.settings_layout_title),
                            description = stringResource(R.string.settings_layout_desc),
                            modifier = Modifier.testTag("settings_layout_card")
                        ) {
                            SegmentedPillRow(
                                options = listOf(
                                    PillOption(
                                        stringResource(R.string.layout_mobile),
                                        settings.layoutMode == LayoutMode.MOBILE,
                                        testTag = "layout_option_mobile"
                                    ),
                                    PillOption(
                                        stringResource(R.string.layout_tv),
                                        settings.layoutMode == LayoutMode.TV,
                                        testTag = "layout_option_tv"
                                    )
                                ),
                                onSelect = { index ->
                                    onSelectLayout(if (index == 0) LayoutMode.MOBILE else LayoutMode.TV)
                                },
                                selectedColor = palette.accent,
                                selectedTextColor = palette.sheet,
                                fillWidth = true
                            )
                        }

                        ItemGap()

                        // 3b. Font size (beta 7.9): five steps, split 3 + 2.
                        SettingsCard {
                            SettingRowBlock(
                                title = stringResource(R.string.settings_font_title),
                                description = stringResource(R.string.settings_font_desc)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SegmentedPillRow(
                                        options = FontScale.STEPS.take(3).map { step ->
                                            PillOption(FontScale.label(step), settings.fontScale == step)
                                        },
                                        onSelect = { index -> onSelectFontScale(FontScale.STEPS[index]) },
                                        fillWidth = true
                                    )
                                    SegmentedPillRow(
                                        options = FontScale.STEPS.drop(3).map { step ->
                                            PillOption(FontScale.label(step), settings.fontScale == step)
                                        },
                                        onSelect = { index ->
                                            onSelectFontScale(FontScale.STEPS[3 + index])
                                        },
                                        fillWidth = true
                                    )
                                }
                            }
                        }

                        ItemGap()

                        // 3c. beta30: Arabic-Indic numerals, home surfaces only.
                        SettingsCard(
                            modifier = Modifier.testTag("settings_numerals_card")
                        ) {
                            SettingsToggleRow(
                                title = stringResource(R.string.settings_numerals_title),
                                description = stringResource(R.string.settings_numerals_desc),
                                checked = settings.arabicIndicNumerals,
                                onCheckedChange = onSelectArabicIndic,
                                modifier = Modifier.testTag("settings_numerals_toggle"),
                                toggleTestTag = "settings_numerals_switch"
                            )
                        }

                        ItemGap()

                        // 4. Widget language: independent of the UI language.
                        SettingsCard {
                            SettingRowBlock(
                                title = stringResource(R.string.settings_widget_title),
                                description = stringResource(R.string.settings_widget_desc)
                            ) {
                                SegmentedPillRow(
                                    options = listOf(
                                        PillOption(
                                            stringResource(R.string.language_arabic),
                                            widgetLanguage == AppLanguage.ARABIC
                                        ),
                                        PillOption(
                                            stringResource(R.string.language_english),
                                            widgetLanguage == AppLanguage.ENGLISH
                                        )
                                    ),
                                    onSelect = { index ->
                                        onSelectWidgetLanguage(
                                            if (index == 0) AppLanguage.ARABIC else AppLanguage.ENGLISH
                                        )
                                    }
                                )
                            }
                        }
                    }

                    SettingsSection.ABOUT -> {
                        // beta30 (Task 3): the only entry point of the adhkar
                        // screen. It is a plain row of this section - no dock
                        // slot, no route - so nothing else in the app moves.
                        SettingsCard(
                            contentPadding = PaddingValues(0.dp),
                            onClick = onOpenAdhkar,
                            modifier = Modifier.testTag("settings_adhkar_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_adhkar_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.text
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.settings_adhkar_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textMuted
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = palette.accent
                                )
                            }
                        }

                        ItemGap()

                        // 4b. Updates (beta 7.95): the only network feature.
                        UpdatesSettingsCard(
                            enabled = updatesEnabled,
                            state = updateState,
                            onEnabledChange = onUpdatesEnabled,
                            onCheckNow = onCheckForUpdates,
                            onDownload = onDownloadUpdate,
                            onInstall = onInstallUpdate,
                            onOpenReleases = onOpenReleasesPage,
                            lastInstall = lastInstallAttempt,
                            onOpenInstallSettings = onOpenInstallSettings
                        )

                        ItemGap()

                        // 10. About - the LAST item of the list.
                        SettingsCard(
                            title = stringResource(R.string.settings_about_title),
                            modifier = Modifier.testTag("settings_about")
                        ) {
                            Text(
                                text = stringResource(R.string.settings_about_brand),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.text
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accent
                            )
                            Spacer(Modifier.height(12.dp))
                            // Single source of truth for the timetable claim.
                            Text(
                                text = stringResource(R.string.settings_about_timetable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.text,
                                modifier = Modifier.testTag("settings_about_claim")
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InfoChip(
                                    text = stringResource(R.string.settings_about_offline),
                                    modifier = Modifier.weight(1f)
                                )
                                InfoChip(
                                    text = stringResource(R.string.settings_about_no_data),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.settings_about_network),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textMuted,
                                modifier = Modifier.testTag("settings_about_network")
                            )
                            Spacer(Modifier.height(14.dp))
                            // Credit is visually separated and always the last line.
                            HorizontalDivider(color = palette.text.copy(alpha = 0.12f))
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.settings_about_credit),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textMuted,
                                modifier = Modifier.testTag("settings_about_credit")
                            )
                        }
                    }
                }
            }
            ItemGap()
        }

        // Framework bottom padding: the About card is the last item.
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * One accordion entry: a tappable header (icon, Arabic title, chevron) and the
 * section body, revealed with the framework's own expand/shrink animation.
 *
 * The header is a [GlassCard] so it shares the list's surface, border and
 * radius tokens; the chevron rotates 180° while open. Headers are always
 * visible; only the body animates, so collapsing never shifts the header of
 * the next section.
 */
@Composable
private fun SettingsSectionBlock(
    section: SettingsSection,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = skyPalette
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "settingsSectionChevron"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        GlassCard(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            onClick = onToggle
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(section.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.text,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_section_title_${section.name.lowercase()}")
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotation)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(180)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(120))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = NidaaDimens.cardGap)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingRowBlock(
    title: String,
    description: String,
    control: @Composable () -> Unit
) {
    SettingRow(
        title = title,
        description = description
    ) { control() }
}

/**
 * beta17: the adhan loudness row, directly under the picker.
 *
 * The slider keeps a local value while the thumb moves (a DataStore write per
 * pixel would be wasteful) and persists exactly once per gesture. The chosen
 * percentage is always visible on the row, so a screen reader and the eye read
 * the same number. [Slider] itself is RTL-aware: 0 % starts at the right edge
 * in Arabic and the golden accent marks the filled side.
 */
@Composable
private fun AdhanVolumeRow(
    volume: Int,
    onChange: (Int) -> Unit,
    onCommit: () -> Unit
) {
    val palette = skyPalette
    var local by remember(volume) { mutableIntStateOf(AdhanVolume.normalize(volume)) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_adhan_volume_title),
                style = MaterialTheme.typography.titleSmall,
                color = palette.text,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(
                    R.string.settings_adhan_volume_value,
                    // Latin digits, like every time and date in the app.
                    remember(local) { String.format(Locale.US, "%d", local) }
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
                modifier = Modifier.testTag("settings_adhan_volume_value")
            )
        }
        Slider(
            value = local.toFloat(),
            onValueChange = { local = it.roundToInt() },
            onValueChangeFinished = {
                onChange(local)
                onCommit()
            },
            valueRange = AdhanVolume.MIN.toFloat()..AdhanVolume.MAX.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.text.copy(alpha = 0.18f),
                activeTickColor = palette.sheet,
                inactiveTickColor = palette.text.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_adhan_volume_slider")
        )
    }
}

/**
 * beta18: a system-status row (exact alarms, notification permission, battery).
 *
 * Sizing is content-driven end to end: [SettingsCard] -> [GlassCard] wrap their
 * content (no fixed height anywhere), the content padding comes from the theme
 * on every side, and the description is allowed unlimited lines with visible
 * overflow. The description can therefore never be clipped at the card's bottom
 * edge, whatever the font scale or text length.
 */
@Composable
private fun StatusCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val palette = skyPalette
    SettingsCard(
        contentPadding = PaddingValues(NidaaDimens.statusCardPadding),
        onClick = onClick,
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = palette.accent
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Visible,
            modifier = if (testTag != null) {
                Modifier.testTag("${testTag}_description")
            } else {
                Modifier
            }
        )
    }
}

/**
 * beta18: the single card-to-card distance for the Settings list, read from the
 * `NidaaDimens.cardGap` theme token so every pair of neighbouring cards shares
 * one value (including the status rows and the About card).
 */
@Composable
private fun ItemGap() {
    Spacer(Modifier.height(NidaaDimens.cardGap))
}

@Composable
private fun eventLabel(event: NotifyEvent): String = stringResource(
    when (event) {
        NotifyEvent.FAJR -> R.string.prayer_fajr
        NotifyEvent.SUNRISE -> R.string.prayer_sunrise
        NotifyEvent.DHUHR -> R.string.prayer_dhuhr
        NotifyEvent.ASR -> R.string.prayer_asr
        NotifyEvent.MAGHRIB -> R.string.prayer_maghrib
        NotifyEvent.ISHA -> R.string.prayer_isha
        NotifyEvent.LAST_THIRD -> R.string.last_third
    }
)

@Composable
private fun modeLabel(mode: NotifyMode): String = stringResource(
    when (mode) {
        NotifyMode.ADHAN -> R.string.notify_mode_adhan
        NotifyMode.SILENT -> R.string.notify_mode_silent
        NotifyMode.VIBRATE -> R.string.notify_mode_vibrate
    }
)

/** The hero's next time, reused for the live clock-format preview. */
private fun nextPreviewMinutes(day: TableDay): Int {
    val nowSeconds = LocalTime.now().toSecondOfDay()
    val events = listOf(day.fajr, day.sunrise, day.dhuhr, day.asr, day.maghrib, day.isha)
    return events.firstOrNull { it * 60 > nowSeconds } ?: day.fajrTomorrow
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun openBatterySettings(context: Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
