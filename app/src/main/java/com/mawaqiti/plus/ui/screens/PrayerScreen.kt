package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEngine
import com.mawaqiti.plus.engine.table.TableEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun PrayerScreen(
    city: City,
    day: TableDay,
    date: LocalDate,
    isArabic: Boolean,
    onChooseCity: () -> Unit
) {
    val now = rememberClock()
    val next = remember(day, now) { nextPrayer(day, now) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
    ) {
        Header(city = city, date = date, isArabic = isArabic, onChooseCity = onChooseCity)

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            NextPrayerCard(next = next)
            Spacer(Modifier.height(14.dp))
            PrayerTimesCard(day = day, nextEvent = next.event)
            Spacer(Modifier.height(14.dp))
            LastThirdCard(minutes = day.lastThird)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.official_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Header(
    city: City,
    date: LocalDate,
    isArabic: Boolean,
    onChooseCity: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(colors.primary, colors.primary.copy(alpha = 0.86f))
                )
            )
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
                Spacer(Modifier.width(10.dp))
                Surface(shape = RoundedCornerShape(50), color = colors.secondary) {
                    Text(
                        text = stringResource(R.string.beta_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.onPrimary.copy(alpha = 0.14f),
                modifier = Modifier.clickable(onClick = onChooseCity)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = colors.onPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = city.label(isArabic),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.city_chip_desc),
                        tint = colors.onPrimary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = date.format(dateFormatter(isArabic)),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

private data class NextPrayer(
    val event: TableEvent,
    val minutes: Int,
    val tomorrow: Boolean,
    val secondsLeft: Long
)

private fun nextPrayer(day: TableDay, now: LocalTime): NextPrayer {
    val nowSeconds = now.toSecondOfDay().toLong()
    val today = listOf(
        TableEvent.FAJR to day.fajr,
        TableEvent.SUNRISE to day.sunrise,
        TableEvent.DHUHR to day.dhuhr,
        TableEvent.ASR to day.asr,
        TableEvent.MAGHRIB to day.maghrib,
        TableEvent.ISHA to day.isha
    )
    val event = today.firstOrNull { it.second * 60L > nowSeconds }
    if (event != null) {
        return NextPrayer(
            event = event.first,
            minutes = event.second,
            tomorrow = false,
            secondsLeft = event.second * 60L - nowSeconds
        )
    }
    return NextPrayer(
        event = TableEvent.FAJR,
        minutes = day.fajrTomorrow,
        tomorrow = true,
        secondsLeft = day.fajrTomorrow * 60L + 86_400L - nowSeconds
    )
}

@Composable
private fun prayerLabel(event: TableEvent): String = when (event) {
    TableEvent.FAJR -> stringResource(R.string.prayer_fajr)
    TableEvent.SUNRISE -> stringResource(R.string.prayer_sunrise)
    TableEvent.DHUHR -> stringResource(R.string.prayer_dhuhr)
    TableEvent.ASR -> stringResource(R.string.prayer_asr)
    TableEvent.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    TableEvent.ISHA -> stringResource(R.string.prayer_isha)
}

@Composable
private fun NextPrayerCard(next: NextPrayer) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.next_prayer),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimaryContainer.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prayerLabel(next.event),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer
                )
                Text(
                    text = TableEngine.format(next.minutes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format(
                    Locale.US,
                    "%s  %02d:%02d:%02d",
                    stringResource(R.string.remaining),
                    next.secondsLeft / 3600,
                    (next.secondsLeft % 3600) / 60,
                    next.secondsLeft % 60
                ),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PrayerTimesCard(day: TableDay, nextEvent: TableEvent) {
    val rows = listOf(
        TableEvent.FAJR to day.fajr,
        TableEvent.SUNRISE to day.sunrise,
        TableEvent.DHUHR to day.dhuhr,
        TableEvent.ASR to day.asr,
        TableEvent.MAGHRIB to day.maghrib,
        TableEvent.ISHA to day.isha
    )
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.todays_times),
                style = MaterialTheme.typography.titleMedium,
                color = colors.primary
            )
            Spacer(Modifier.height(6.dp))
            rows.forEachIndexed { index, (event, minutes) ->
                if (index > 0) {
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.12f))
                }
                val highlighted = event == nextEvent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (highlighted) colors.secondaryContainer.copy(alpha = 0.6f)
                            else colors.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prayerLabel(event),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = TableEngine.format(minutes),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (highlighted) colors.primary else colors.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LastThirdCard(minutes: Int) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.last_third),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = TableEngine.format(minutes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant
            )
        }
    }
}

private fun dateFormatter(isArabic: Boolean): DateTimeFormatter {
    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH
    val pattern = if (isArabic) "EEEE، d MMMM yyyy" else "EEEE, d MMMM yyyy"
    // Latin digits in both languages, matching the original app.
    return DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.STANDARD)
}

@Composable
private fun rememberClock(): LocalTime {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000)
        }
    }
    return now
}
