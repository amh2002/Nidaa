package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.BuildConfig
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.TimeFormatter
import com.mawaqiti.plus.ui.components.GlassCard
import com.mawaqiti.plus.ui.components.SettingRow
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.mawaqiti.plus.ui.theme.skyPalette
import java.time.LocalTime

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
    onSummerTimeChange: (Boolean) -> Unit
) {
    val palette = skyPalette
    val previewMinutes = nextPreviewMinutes(day)
    val preview = TimeFormatter.format(previewMinutes, settings.clockFormat, isArabic)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        ScreenTopBar(title = stringResource(R.string.settings_title), palette = palette, onBack = onBack)

        // City
        GlassCard(contentPadding = PaddingValues(0.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenCityPicker)
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
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = null,
                    tint = palette.accent
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Language
        GlassCard {
            SettingRow(
                title = stringResource(R.string.settings_language),
                description = stringResource(R.string.language_desc)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill(
                        label = stringResource(R.string.language_arabic),
                        selected = isArabic,
                        palette = palette,
                        onClick = { onSelectLanguage(AppLanguage.ARABIC) }
                    )
                    ChoicePill(
                        label = stringResource(R.string.language_english),
                        selected = !isArabic,
                        palette = palette,
                        onClick = { onSelectLanguage(AppLanguage.ENGLISH) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Clock format
        GlassCard {
            SettingRow(
                title = stringResource(R.string.settings_clock_title),
                description = stringResource(R.string.clock_preview) + ": " + preview
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill(
                        label = stringResource(R.string.clock_24),
                        selected = settings.clockFormat == ClockFormat.H24,
                        palette = palette,
                        onClick = { onSelectClockFormat(ClockFormat.H24) }
                    )
                    ChoicePill(
                        label = stringResource(R.string.clock_12),
                        selected = settings.clockFormat == ClockFormat.H12,
                        palette = palette,
                        onClick = { onSelectClockFormat(ClockFormat.H12) }
                    )
                }
            }
        }

        // Summer time: Palestine only.
        if (city.country == Country.PS) {
            Spacer(Modifier.height(12.dp))
            GlassCard {
                SettingRow(
                    title = stringResource(R.string.settings_summer_time),
                    description = stringResource(R.string.summer_time_desc)
                ) {
                    Switch(
                        checked = settings.summerTime,
                        onCheckedChange = onSummerTimeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SkyPalette.BrandGreen,
                            checkedBorderColor = SkyPalette.BrandGreen,
                            uncheckedThumbColor = palette.textMuted,
                            uncheckedTrackColor = palette.text.copy(alpha = 0.16f),
                            uncheckedBorderColor = palette.text.copy(alpha = 0.30f)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // About: name + version, timetable source, creator, offline/privacy.
        GlassCard {
            Text(
                text = stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.text
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = palette.accent
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_about_timetable),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_about_creator),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = palette.text
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_about_offline),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ChoicePill(
    label: String,
    selected: Boolean,
    palette: SkyPalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) SkyPalette.BrandGreen else palette.text.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else palette.text
        )
    }
}

/** The hero's next time, reused for the live clock-format preview. */
private fun nextPreviewMinutes(day: TableDay): Int {
    val nowSeconds = LocalTime.now().toSecondOfDay()
    val events = listOf(day.fajr, day.sunrise, day.dhuhr, day.asr, day.maghrib, day.isha)
    return events.firstOrNull { it * 60 > nowSeconds } ?: day.fajrTomorrow
}
