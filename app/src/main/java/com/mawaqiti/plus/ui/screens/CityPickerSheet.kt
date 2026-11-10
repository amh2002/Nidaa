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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.ui.CitySearch
import com.mawaqiti.plus.ui.theme.CitySheetPalette
import java.text.Collator
import java.util.Locale

/**
 * Bottom-sheet city picker: country segmented control, live search and a
 * single-tap list. There is no free-text city entry and no coordinate input;
 * search only filters the verified list.
 *
 * The sheet surface is always dark (see [CitySheetPalette]) so the same picker
 * appears in Day, Golden, Dawn and Night without ever flashing white.
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var country by remember { mutableStateOf(selectedCity.country) }
    var query by remember { mutableStateOf("") }

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
        containerColor = CitySheetPalette.Fallback,
        contentColor = CitySheetPalette.Text,
        scrimColor = CitySheetPalette.Scrim,
        shape = RoundedCornerShape(
            topStart = CitySheetPalette.TopRadius,
            topEnd = CitySheetPalette.TopRadius
        ),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(CitySheetPalette.GradientTop, CitySheetPalette.GradientBottom)
                    )
                )
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
                    .background(CitySheetPalette.Grabber)
            )

            Text(
                text = stringResource(R.string.city_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CitySheetPalette.Text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))

            CountrySegments(
                selected = country,
                onSelected = {
                    if (country != it) {
                        country = it
                        query = ""
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.city_search_hint),
                clearDescription = stringResource(R.string.clear_search)
            )

            Spacer(Modifier.height(6.dp))

            when {
                country == Country.JO -> JordanEmptyState()
                filtered.isEmpty() -> EmptyResult()
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
                                onClick = {
                                    onSelect(city)
                                    onDismiss()
                                }
                            )
                            HorizontalDivider(color = CitySheetPalette.Divider)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.cities_count, filtered.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = CitySheetPalette.Muted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
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
    clearDescription: String
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = CitySheetPalette.Text),
        cursorBrush = SolidColor(CitySheetPalette.Accent),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(CitySheetPalette.FieldFill)
                    .border(
                        width = 1.5.dp,
                        color = CitySheetPalette.FieldBorder,
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = CitySheetPalette.Hint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CitySheetPalette.Hint
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
                            tint = CitySheetPalette.Hint,
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
    onSelected: (Country) -> Unit
) {
    val order = listOf(Country.PS, Country.JO, Country.SA)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(CitySheetPalette.Track)
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
                    .background(if (active) CitySheetPalette.ActiveFill else Color.Transparent)
                    .clickable { onSelected(country) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) CitySheetPalette.Accent else CitySheetPalette.Text,
                    textAlign = TextAlign.Center
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
            color = if (selected) CitySheetPalette.Accent else CitySheetPalette.Text,
            modifier = Modifier.weight(1f)
        )
        if (city.order == City.DEFAULT_ORDER) {
            Text(
                text = stringResource(R.string.default_city_tag),
                style = MaterialTheme.typography.labelSmall,
                color = CitySheetPalette.DefaultTag,
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
                tint = CitySheetPalette.Accent
            )
        }
    }
}

@Composable
private fun JordanEmptyState() {
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
            color = CitySheetPalette.Text,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.jordan_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = CitySheetPalette.Muted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyResult() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_results),
            style = MaterialTheme.typography.bodyLarge,
            color = CitySheetPalette.Muted
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
