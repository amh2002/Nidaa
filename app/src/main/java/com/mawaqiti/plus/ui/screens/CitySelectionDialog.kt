package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import java.text.Collator
import java.util.Locale

/**
 * Two-step, zero-typing city picker: country card first, then a read-only
 * dropdown with that country's cities sorted by the current locale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionDialog(
    cities: List<City>,
    selectedCity: City,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (City) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var country by remember { mutableStateOf(selectedCity.country) }
    var pendingOrder by remember { mutableIntStateOf(selectedCity.order) }

    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH
    val sorted = remember(country, isArabic, cities) {
        val collator = Collator.getInstance(locale)
        cities
            .filter { it.country == country }
            .sortedWith { left, right ->
                collator.compare(
                    if (isArabic) left.nameArDisplay else left.nameEnDisplay,
                    if (isArabic) right.nameArDisplay else right.nameEnDisplay
                )
            }
    }
    val pending = sorted.firstOrNull { it.order == pendingOrder }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.select_city_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        if (step == 1) R.string.select_country_hint else R.string.select_city_step
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(16.dp))

                if (step == 1) {
                    Text(
                        text = stringResource(R.string.select_country_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    CountryCard(
                        name = stringResource(R.string.country_palestine),
                        count = cities.count { it.country == Country.PS },
                        selected = country == Country.PS,
                        onClick = {
                            country = Country.PS
                            pendingOrder = if (selectedCity.country == Country.PS) {
                                selectedCity.order
                            } else {
                                cities.first { it.country == Country.PS }.order
                            }
                            step = 2
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    CountryCard(
                        name = stringResource(R.string.country_saudi),
                        count = cities.count { it.country == Country.SA },
                        selected = country == Country.SA,
                        onClick = {
                            country = Country.SA
                            pendingOrder = if (selectedCity.country == Country.SA) {
                                selectedCity.order
                            } else {
                                cities.first { it.country == Country.SA }.order
                            }
                            step = 2
                        }
                    )
                } else {
                    Text(
                        text = stringResource(R.string.select_country_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = pending?.displayName(isArabic)
                                ?: stringResource(R.string.select_city_hint),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text(stringResource(R.string.select_city_hint)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 360.dp)
                        ) {
                            sorted.forEach { city ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = city.displayName(isArabic),
                                            fontWeight = if (city.order == pendingOrder) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        if (city.order == pendingOrder) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        pendingOrder = city.order
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cities_count, sorted.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step == 2) {
                        TextButton(onClick = { step = 1 }) {
                            Text(stringResource(R.string.back))
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        enabled = step == 2 && pending != null,
                        onClick = {
                            pending?.let {
                                onConfirm(it)
                                onDismiss()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryCard(
    name: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val border = if (selected) colors.primary else colors.outline.copy(alpha = 0.3f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer else colors.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .background(if (selected) colors.primaryContainer else colors.surface)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = colors.primary
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface
                )
                Text(
                    text = stringResource(R.string.cities_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.primary
                )
            }
        }
    }
}
