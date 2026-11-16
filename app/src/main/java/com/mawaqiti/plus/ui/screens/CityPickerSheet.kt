package com.mawaqiti.plus.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.ui.CitySearch
import com.mawaqiti.plus.ui.theme.GoldenPalettes
import com.mawaqiti.plus.ui.theme.GoldenPhase
import com.mawaqiti.plus.ui.theme.SkyPalette
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.delay

/** The city search surface is always the deep-night golden palette. */
private val searchPalette: SkyPalette = GoldenPalettes.of(GoldenPhase.NIGHT)

/**
 * Bottom-sheet city picker: country segmented control, live search and a
 * single-tap list. There is no free-text city entry and no coordinate input;
 * search only filters the verified list.
 *
 * beta10: the sheet is painted with the beta9 golden-night gradient (always the
 * night palette, whatever phase the app is in), the search field carries a soft
 * golden border, and every result is a glass card matching the prayer-time
 * cards. Two explicit states are rendered besides the list: `searching` (a slim
 * gold spinner row while the debounced query is pending) and `no results`.
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
    val palette = searchPalette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var country by remember { mutableStateOf(selectedCity.country) }
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

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

    // Debounced filtering: the query is applied 200 ms after the last
    // keystroke, and the pending interval is what the `searching` state shows.
    LaunchedEffect(query) {
        if (query == appliedQuery) {
            searching = false
            return@LaunchedEffect
        }
        if (query.isBlank()) {
            appliedQuery = query
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(200)
        appliedQuery = query
        searching = false
    }

    val filtered = remember(byCountry, appliedQuery) { CitySearch.filter(byCountry, appliedQuery) }

    // The sheet is dark regardless of phase, so ask the system bars for light
    // icons while it is visible. This is the appearance hook we control for the
    // IME/navigation area; the keyboard surface itself is drawn by the IME
    // process (see BETA_NOTES for the limitation).
    val view = LocalView.current
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window, view) {
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (previousStatus != null) controller.isAppearanceLightStatusBars = previousStatus
            if (previousNavigation != null) {
                controller.isAppearanceLightNavigationBars = previousNavigation
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.gradient.first(),
        contentColor = palette.text,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(palette.gradient))
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.30f))
            )

            Text(
                text = stringResource(R.string.city_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.text,
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
                        appliedQuery = ""
                        searching = false
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.city_search_hint),
                clearDescription = stringResource(R.string.clear_search),
                palette = palette
            )

            Spacer(Modifier.height(10.dp))

            when {
                country == Country.JO -> JordanEmptyState(palette)
                else -> {
                    if (searching) {
                        SearchingRow(palette)
                    }
                    if (filtered.isEmpty()) {
                        if (!searching) EmptyResult(palette)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 430.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filtered, key = { it.order }) { city ->
                                CityResultCard(
                                    city = city,
                                    isArabic = isArabic,
                                    selected = city.order == selectedCity.order,
                                    palette = palette,
                                    onClick = {
                                        onSelect(city)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.cities_count, filtered.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textMuted,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
    palette: SkyPalette
) {
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text),
        cursorBrush = SolidColor(palette.accent),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(palette.cardFill)
                    .border(
                        width = 1.dp,
                        color = palette.accent.copy(alpha = 0.45f),
                        shape = shape
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = palette.accent.copy(alpha = 0.80f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.text.copy(alpha = 0.60f)
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onQueryChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = clearDescription,
                            tint = palette.accent.copy(alpha = 0.80f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    )
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
            .background(Color.White.copy(alpha = 0.06f))
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
                    .background(if (active) palette.accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelected(country) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) palette.accent else palette.text,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** beta10: a city is a card, styled exactly like a prayer-time card. */
@Composable
private fun CityResultCard(
    city: City,
    isArabic: Boolean,
    selected: Boolean,
    palette: SkyPalette,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) palette.heroFill else palette.cardFill)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) palette.accent.copy(alpha = 0.80f) else palette.cardBorder,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = city.displayName(isArabic),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) palette.accent else palette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (city.order == City.DEFAULT_ORDER) {
            Text(
                text = stringResource(R.string.default_city_tag),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = palette.accent
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
            color = palette.text,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.jordan_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SearchingRow(palette: SkyPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = palette.accent,
            strokeWidth = 1.5.dp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.searching),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted
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
            color = palette.textMuted
        )
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
