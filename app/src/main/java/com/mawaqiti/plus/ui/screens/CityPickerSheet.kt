package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.ui.CitySearch
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.mawaqiti.plus.ui.theme.skyPalette
import java.text.Collator
import java.util.Locale

/**
 * Bottom-sheet city picker: country segmented control, live search and a
 * single-tap list. There is no free-text city entry and no coordinate input;
 * search only filters the verified list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPickerSheet(
    cities: List<City>,
    selectedCity: City,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSelect: (City) -> Unit
) {
    val palette = skyPalette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var country by remember { mutableStateOf(selectedCity.country) }
    var query by remember { mutableStateOf("") }

    val accent = if (palette.dark) Color(0xFF8FE3B8) else SkyPalette.BrandGreen
    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH

    val byCountry = remember(country, cities, isArabic) {
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
    val filtered = remember(byCountry, query) { CitySearch.filter(byCountry, query) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.sheet,
        contentColor = palette.sheetText,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(palette.sheetText.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp)
        ) {
            Text(
                text = stringResource(R.string.city_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.sheetText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))

            CountrySegments(
                selected = country,
                palette = palette,
                onSelected = {
                    if (country != it) {
                        country = it
                        query = ""
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = {
                    Text(
                        text = stringResource(R.string.city_search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.sheetMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_desc),
                        tint = palette.sheetMuted
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = palette.sheetMuted
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = palette.sheetText,
                    unfocusedTextColor = palette.sheetText,
                    cursorColor = accent,
                    focusedBorderColor = accent.copy(alpha = 0.75f),
                    unfocusedBorderColor = palette.sheetText.copy(alpha = 0.22f),
                    focusedPlaceholderColor = palette.sheetMuted,
                    unfocusedPlaceholderColor = palette.sheetMuted
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            when {
                country == Country.JO -> JordanEmptyState(palette)
                filtered.isEmpty() -> EmptyResult(palette)
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 430.dp)
                    ) {
                        items(filtered, key = { it.order }) { city ->
                            CityRow(
                                city = city,
                                isArabic = isArabic,
                                selected = city.order == selectedCity.order,
                                accent = accent,
                                palette = palette,
                                onClick = {
                                    onSelect(city)
                                    onDismiss()
                                }
                            )
                            HorizontalDivider(
                                color = palette.sheetText.copy(alpha = 0.08f)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.cities_count, filtered.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.sheetMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun CountrySegments(
    selected: Country,
    palette: SkyPalette,
    onSelected: (Country) -> Unit
) {
    val order = listOf(Country.PS, Country.JO, Country.SA)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(palette.sheetText.copy(alpha = 0.07f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        order.forEach { country ->
            val active = country == selected
            val label = when (country) {
                Country.PS -> stringResource(R.string.country_palestine)
                Country.JO -> stringResource(R.string.country_jordan)
                Country.SA -> stringResource(R.string.country_saudi)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) SkyPalette.BrandGreen else Color.Transparent)
                    .clickable { onSelected(country) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) Color.White else palette.sheetMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CityRow(
    city: City,
    isArabic: Boolean,
    selected: Boolean,
    accent: Color,
    palette: SkyPalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = city.displayName(isArabic),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) accent else palette.sheetText,
            modifier = Modifier.weight(1f)
        )
        if (city.order == City.DEFAULT_ORDER) {
            Text(
                text = stringResource(R.string.default_city_tag),
                style = MaterialTheme.typography.labelSmall,
                color = palette.sheetMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.sheetText.copy(alpha = 0.06f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = accent
            )
        }
    }
}

@Composable
private fun JordanEmptyState(palette: SkyPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.jordan_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.sheetText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.jordan_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.sheetMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyResult(palette: SkyPalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_results),
            style = MaterialTheme.typography.bodyLarge,
            color = palette.sheetMuted
        )
    }
}
