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
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.data.FontScale
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.TimeFormatter
import com.mawaqiti.plus.notify.NotificationScheduler
import com.mawaqiti.plus.notify.NotifyEvent
import com.mawaqiti.plus.notify.NotifyMode
import com.mawaqiti.plus.ui.components.InfoChip
import com.mawaqiti.plus.ui.components.PillOption
import com.mawaqiti.plus.ui.components.SegmentedPillRow
import com.mawaqiti.plus.ui.components.SettingsCard
import com.mawaqiti.plus.ui.components.SettingRow
import com.mawaqiti.plus.ui.components.SettingsSmallButton
import com.mawaqiti.plus.ui.components.SettingsToggleRow
import com.mawaqiti.plus.ui.components.mawaqitiSwitchColors
import com.mawaqiti.plus.ui.theme.skyPalette
import java.time.LocalTime

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
    onEventEnabled: (NotifyEvent, Boolean) -> Unit,
    onEventMode: (NotifyEvent, NotifyMode) -> Unit,
    onApplyModeToEnabled: (NotifyMode) -> Unit,
    onTestEvent: (NotifyEvent, NotifyMode) -> Unit,
    onTestMode: (NotifyMode) -> Unit
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
        onEventEnabled = onEventEnabled,
        onEventMode = onEventMode,
        onApplyModeToEnabled = onApplyModeToEnabled,
        onTestEvent = onTestEvent,
        onTestMode = onTestMode,
        onOpenExactAlarmSettings = { NotificationScheduler.openExactAlarmSettings(context) },
        onOpenNotificationSettings = { openNotificationSettings(context) },
        onOpenBatterySettings = { openBatterySettings(context) }
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
    onEventEnabled: (NotifyEvent, Boolean) -> Unit,
    onEventMode: (NotifyEvent, NotifyMode) -> Unit,
    onApplyModeToEnabled: (NotifyMode) -> Unit,
    onTestEvent: (NotifyEvent, NotifyMode) -> Unit,
    onTestMode: (NotifyMode) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val palette = skyPalette
    val previewMinutes = nextPreviewMinutes(day)
    val preview = TimeFormatter.format(previewMinutes, settings.clockFormat, isArabic)
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

        // 7. One card per event, each with its own switch, mode and test button.
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
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.notify_test_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    SettingsSmallButton(
                        label = stringResource(R.string.notify_test),
                        enabled = masterEnabled,
                        modifier = Modifier.testTag("event_test_${event.storageKey}"),
                        onClick = { onTestEvent(event, current.mode) }
                    )
                }
            }
        }

        ItemGap()

        // 8. Apply one mode to every enabled event.
        SettingsCard(
            title = stringResource(R.string.notify_apply_all),
            description = stringResource(R.string.notify_apply_all_desc)
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

        // 10. Debug-only direct delivery buttons.
        if (BuildConfig.DEBUG) {
            ItemGap()
            SettingsCard(
                title = stringResource(R.string.notify_test_group),
                description = stringResource(R.string.notify_test_group_desc)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsSmallButton(
                        label = stringResource(R.string.notify_test_adhan),
                        onClick = { onTestMode(NotifyMode.ADHAN) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSmallButton(
                        label = stringResource(R.string.notify_test_silent),
                        onClick = { onTestMode(NotifyMode.SILENT) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSmallButton(
                        label = stringResource(R.string.notify_test_vibrate),
                        onClick = { onTestMode(NotifyMode.VIBRATE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        ItemGap()

        // 11. About — the LAST item of the list. The claims live here exactly
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
