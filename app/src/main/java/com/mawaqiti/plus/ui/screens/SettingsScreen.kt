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
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.TimeFormatter
import com.mawaqiti.plus.notify.NotificationScheduler
import com.mawaqiti.plus.notify.NotifyEvent
import com.mawaqiti.plus.notify.NotifyMode
import com.mawaqiti.plus.notify.AdhanSound
import com.mawaqiti.plus.notify.AdhanVolume
import com.mawaqiti.plus.ui.components.AdhanSoundDropdown
import com.mawaqiti.plus.ui.components.InfoChip
import com.mawaqiti.plus.ui.components.PillOption
import com.mawaqiti.plus.ui.components.SegmentedPillRow
import com.mawaqiti.plus.ui.components.SettingsCard
import com.mawaqiti.plus.ui.components.SettingRow
import com.mawaqiti.plus.ui.components.SettingsSmallButton
import com.mawaqiti.plus.ui.components.SettingsToggleRow
import com.mawaqiti.plus.ui.components.mawaqitiSwitchColors
import com.mawaqiti.plus.ui.theme.NidaaDimens
import com.mawaqiti.plus.ui.theme.skyPalette
import com.nidaa.prayer.update.UpdateState
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

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
    /** beta10: persisted appearance choice; AUTO is the default. */
    onSelectTheme: (AppTheme) -> Unit = {}
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
        onSelectTheme = onSelectTheme
    )

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
    /** beta10: appearance choice (see [SettingsScreen]). */
    onSelectTheme: (AppTheme) -> Unit = {}
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .testTag("settings_list")
    ) {
        ScreenTopBar(title = stringResource(R.string.settings_title), palette = palette, onBack = onBack)

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

        // 3. Clock format
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
                        onSelectClockFormat(if (index == 0) ClockFormat.H24 else ClockFormat.H12)
                    }
                )
            }
        }

        ItemGap()

        // 3a. Appearance (beta10). "Auto" follows the five golden phases of the
        //     day; "Night (gold)" pins the deep-night look. Persisted in DataStore
        //     and applied immediately, without restarting the app.
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
                    // Golden selection, styled like the beta 9 cards: gold pill
                    // with the sheet's dark ink on top (contrast > 10:1).
                    selectedColor = palette.accent,
                    selectedTextColor = palette.sheet
                )
            }
        }

        ItemGap()

        // 3b. Font size (beta 7.9): five steps, applied as a multiplier on the
        //     system font scale at the app root. Split into 3 + 2 pills so the
        //     row never overflows, even at 120 % on top of a large system scale.
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

        // 4. Widget language: exactly two choices, independent of the UI language.
        //    Changing it re-renders the widget immediately.
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

        ItemGap()

        // 4b. Updates (beta 7.95): the only network feature, opt-out, with a
        //     manual check that still works when the automatic check is off.
        UpdatesSettingsCard(
            enabled = updatesEnabled,
            state = updateState,
            onEnabledChange = onUpdatesEnabled,
            onCheckNow = onCheckForUpdates,
            onDownload = onDownloadUpdate,
            onInstall = onInstallUpdate,
            onOpenReleases = onOpenReleasesPage
        )

        // 5. Summer time: Palestine only.
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

        ItemGap()

        // 5b. Adhan sound (beta14) + beta17 volume.
        //
        //     ONE persisted sound and ONE persisted loudness, both read at fire
        //     time by AlarmReceiver and by the sound-only preview. The picker
        //     and the preview button share a single horizontally aligned row so
        //     the test always proves the current choice; the volume slider sits
        //     directly under them inside the same card.
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
                    // Restarting the preview is the caller's business; stopping
                    // it here keeps the audible track in step with the slider.
                    if (previewingAdhan) onStopAdhanPreview()
                }
            )
        }

        ItemGap()

        // 6. Notifications master switch.
        SettingsCard(
            title = stringResource(R.string.settings_notifications),
            description = stringResource(R.string.settings_notifications_desc)
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

        // 6b. Apply one mode to every enabled event (beta17: moved to the TOP of
        //     the prayer-notifications section, directly before Fajr). Behaviour,
        //     styling and strings are untouched - only the position changed, so
        //     "set everything at once" no longer requires scrolling past six
        //     event cards.
        ItemGap()
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

        ItemGap()

        // 9. System status rows, only when they need attention.
        if (!canScheduleExact) {
            ItemGap()
            StatusCard(
                title = stringResource(R.string.notify_exact_title),
                description = stringResource(R.string.notify_exact_desc),
                onClick = onOpenExactAlarmSettings
            )
        }
        if (!notificationsGranted) {
            ItemGap()
            StatusCard(
                title = stringResource(R.string.notify_permission_title),
                description = stringResource(R.string.notify_permission_desc),
                onClick = onOpenNotificationSettings
            )
        }
        if (batteryOptimized) {
            ItemGap()
            StatusCard(
                title = stringResource(R.string.notify_battery_title),
                description = stringResource(R.string.notify_battery_desc),
                onClick = onOpenBatterySettings
            )
        }

        // 10. About — the LAST item of the list. The claims live here exactly
        //     once; the old standalone footer card is gone.
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
            // Single source of truth for the timetable claim (both countries).
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
            // Credit is visually separated and always the very last line.
            HorizontalDivider(color = palette.text.copy(alpha = 0.12f))
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.settings_about_credit),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted,
                modifier = Modifier.testTag("settings_about_credit")
            )
        }

        // Framework bottom padding: About is the last item.
        Spacer(Modifier.height(32.dp))
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
                    // Latin digits, like every time and date in the app: the
                    // Arabic default locale would otherwise render ١٠٠٪ here
                    // and read differently from the rest of the screen.
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

@Composable
private fun StatusCard(title: String, description: String, onClick: () -> Unit) {
    val palette = skyPalette
    SettingsCard(onClick = onClick) {
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
            color = palette.textMuted
        )
    }
}

@Composable
private fun ItemGap() {
    Spacer(Modifier.height(12.dp))
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
