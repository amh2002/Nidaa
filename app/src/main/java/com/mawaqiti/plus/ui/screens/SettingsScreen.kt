package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.LocationSource
import com.mawaqiti.plus.engine.AsrMadhab
import com.mawaqiti.plus.engine.CalculationMethod
import com.mawaqiti.plus.engine.HighLatitudeRule
import com.mawaqiti.plus.ui.components.SectionCard
import com.mawaqiti.plus.ui.components.cityDisplayName
import com.mawaqiti.plus.ui.components.isArabicLocale
import com.mawaqiti.plus.ui.components.methodLabel
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    cities: List<City>,
    locating: Boolean,
    onSelectMethod: (CalculationMethod) -> Unit,
    onSelectMadhab: (AsrMadhab) -> Unit,
    onSelectHighLatitude: (HighLatitudeRule) -> Unit,
    onSaveCustomAngles: (Double, Double) -> Unit,
    onRefreshLocation: () -> Unit,
    onSelectCity: (City) -> Unit,
    onSaveManualLocation: (Double, Double) -> Unit
) {
    val isArabic = isArabicLocale()
    val methods = listOf(
        CalculationMethod.MWL to stringResource(R.string.method_mwl),
        CalculationMethod.ISNA to stringResource(R.string.method_isna),
        CalculationMethod.EGYPT to stringResource(R.string.method_egypt),
        CalculationMethod.UMM_AL_QURA to stringResource(R.string.method_umm_al_qura),
        CalculationMethod.KARACHI to stringResource(R.string.method_karachi),
        CalculationMethod.CUSTOM to stringResource(R.string.method_custom)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        SectionCard(title = stringResource(R.string.settings_method)) {
            Selector(
                label = stringResource(R.string.settings_method),
                selected = methodLabel(settings.method),
                options = methods,
                onSelect = onSelectMethod
            )
            if (settings.method == CalculationMethod.CUSTOM) {
                Spacer(Modifier.height(10.dp))
                CustomAnglesEditor(settings, onSaveCustomAngles)
            }
        }

        SectionCard(title = stringResource(R.string.settings_madhab)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.asrMadhab == AsrMadhab.SHAFI,
                    onClick = { onSelectMadhab(AsrMadhab.SHAFI) },
                    label = { Text(stringResource(R.string.madhab_shafi)) }
                )
                FilterChip(
                    selected = settings.asrMadhab == AsrMadhab.HANAFI,
                    onClick = { onSelectMadhab(AsrMadhab.HANAFI) },
                    label = { Text(stringResource(R.string.madhab_hanafi)) }
                )
            }
        }

        SectionCard(title = stringResource(R.string.settings_high_lat)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.highLatitudeRule == HighLatitudeRule.NIGHT_MIDDLE,
                    onClick = { onSelectHighLatitude(HighLatitudeRule.NIGHT_MIDDLE) },
                    label = { Text(stringResource(R.string.high_lat_night_middle)) }
                )
                FilterChip(
                    selected = settings.highLatitudeRule == HighLatitudeRule.ANGLE_BASED,
                    onClick = { onSelectHighLatitude(HighLatitudeRule.ANGLE_BASED) },
                    label = { Text(stringResource(R.string.high_lat_angle_based)) }
                )
            }
        }

        SectionCard(title = stringResource(R.string.settings_location)) {
            Text(
                text = cityDisplayName(settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format(Locale.US, "%.4f, %.4f", settings.latitude, settings.longitude),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = sourceLabel(settings.locationSource),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onRefreshLocation,
                enabled = !locating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (locating) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp).height(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.locating))
                } else {
                    Icon(Icons.Filled.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_use_gps))
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.settings_city_search),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            CitySearch(
                cities = cities,
                isArabic = isArabic,
                onSelectCity = onSelectCity
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.settings_manual_coords),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            ManualLocationEditor(settings, onSaveManualLocation)
        }

        SectionCard(title = stringResource(R.string.settings_about_title)) {
            Text(
                text = stringResource(R.string.settings_about_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun <T> Selector(
    label: String,
    selected: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selected,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    maxLines = 2
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            expanded = false
                            onSelect(value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomAnglesEditor(
    settings: AppSettings,
    onSave: (Double, Double) -> Unit
) {
    var fajr by remember(settings.customFajrAngle) {
        mutableStateOf(formatAngle(settings.customFajrAngle))
    }
    var isha by remember(settings.customIshaAngle) {
        mutableStateOf(formatAngle(settings.customIshaAngle))
    }
    var error by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = fajr,
            onValueChange = { fajr = it; error = false },
            label = { Text(stringResource(R.string.settings_custom_fajr)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = isha,
            onValueChange = { isha = it; error = false },
            label = { Text(stringResource(R.string.settings_custom_isha)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }
    if (error) {
        Text(
            text = stringResource(R.string.invalid_angle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = {
            val fajrValue = fajr.toDoubleOrNull()
            val ishaValue = isha.toDoubleOrNull()
            if (fajrValue == null || ishaValue == null ||
                fajrValue !in 0.0..30.0 || ishaValue !in 0.0..30.0
            ) {
                error = true
            } else {
                onSave(fajrValue, ishaValue)
            }
        }
    ) {
        Text(stringResource(R.string.save))
    }
}

@Composable
private fun CitySearch(
    cities: List<City>,
    isArabic: Boolean,
    onSelectCity: (City) -> Unit
) {
    var query by remember { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text(stringResource(R.string.city_search_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    if (query.isNotBlank()) {
        val results = remember(query, cities) {
            cities.filter { it.matches(query) }.take(8)
        }
        if (results.isEmpty()) {
            Text(
                text = stringResource(R.string.no_results),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            results.forEach { city ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectCity(city)
                            query = ""
                        }
                        .padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) city.nameAr else city.nameEn,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (isArabic) city.countryAr else city.countryEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.2f, %.2f", city.latitude, city.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ManualLocationEditor(
    settings: AppSettings,
    onSave: (Double, Double) -> Unit
) {
    var latitude by remember(settings.latitude) {
        mutableStateOf(String.format(Locale.US, "%.4f", settings.latitude))
    }
    var longitude by remember(settings.longitude) {
        mutableStateOf(String.format(Locale.US, "%.4f", settings.longitude))
    }
    var error by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = latitude,
            onValueChange = { latitude = it; error = false },
            label = { Text(stringResource(R.string.latitude)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = longitude,
            onValueChange = { longitude = it; error = false },
            label = { Text(stringResource(R.string.longitude)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }
    if (error) {
        Text(
            text = stringResource(R.string.invalid_coords),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = {
            val lat = latitude.toDoubleOrNull()
            val lon = longitude.toDoubleOrNull()
            if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                error = true
            } else {
                onSave(lat, lon)
            }
        }
    ) {
        Text(stringResource(R.string.save_location))
    }
}

@Composable
private fun sourceLabel(source: LocationSource): String = when (source) {
    LocationSource.GPS -> stringResource(R.string.location_source_gps)
    LocationSource.MANUAL -> stringResource(R.string.location_source_manual)
    LocationSource.DEFAULT -> stringResource(R.string.location_source_default)
}

private fun formatAngle(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString()
    else String.format(Locale.US, "%.1f", value)
