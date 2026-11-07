package com.mawaqiti.plus.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.engine.PrayerDay
import com.mawaqiti.plus.engine.PrayerMoment
import com.mawaqiti.plus.engine.PrayerName
import com.mawaqiti.plus.ui.components.cityDisplayName
import com.mawaqiti.plus.ui.components.isArabicLocale
import com.mawaqiti.plus.ui.components.methodLabel
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun PrayerScreen(
    settings: AppSettings,
    day: PrayerDay?,
    hasError: Boolean,
    onRefreshLocation: () -> Unit
) {
    val isArabic = isArabicLocale()
    val now = rememberNow()
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
    ) {
        Header(settings = settings, day = day, isArabic = isArabic)

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            when {
                hasError -> ErrorCard()
                day == null -> Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                else -> {
                    val next = findNextPrayer(day, now)
                    NextPrayerCard(day = day, next = next, isArabic = isArabic)
                    Spacer(Modifier.height(14.dp))
                    PrayerTimesCard(day = day, nextName = next.name)
                    Spacer(Modifier.height(14.dp))
                    ExtraTimesRow(day = day)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = methodLabel(settings.method),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(settings: AppSettings, day: PrayerDay?, isArabic: Boolean) {
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
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.secondary
                ) {
                    Text(
                        text = stringResource(R.string.beta_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = colors.onPrimary.copy(alpha = 0.85f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = cityDisplayName(settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimary
                )
            }
            if (day != null) {
                val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
                val pattern = if (isArabic) "EEEE، d MMMM yyyy" else "EEEE, d MMMM yyyy"
                Text(
                    text = day.date.format(DateTimeFormatter.ofPattern(pattern, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimary.copy(alpha = 0.85f)
                )
            }
        }
    }
}

private data class NextPrayer(val name: PrayerName, val moment: PrayerMoment, val secondsLeft: Long)

private fun findNextPrayer(day: PrayerDay, now: Instant): NextPrayer {
    val candidates = day.prayers + listOf(PrayerName.FAJR to day.tomorrowFajr)
    val next = candidates.firstOrNull { it.second.dateTime.toInstant().isAfter(now) }
        ?: (PrayerName.FAJR to day.tomorrowFajr)
    val seconds = Duration.between(now, next.second.dateTime.toInstant()).seconds
    return NextPrayer(next.first, next.second, seconds.coerceAtLeast(0))
}

@Composable
private fun prayerLabel(name: PrayerName): String = when (name) {
    PrayerName.FAJR -> stringResource(R.string.prayer_fajr)
    PrayerName.SUNRISE -> stringResource(R.string.prayer_sunrise)
    PrayerName.DHUHR -> stringResource(R.string.prayer_dhuhr)
    PrayerName.ASR -> stringResource(R.string.prayer_asr)
    PrayerName.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    PrayerName.ISHA -> stringResource(R.string.prayer_isha)
}

@Composable
private fun NextPrayerCard(day: PrayerDay, next: NextPrayer, isArabic: Boolean) {
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
                    text = prayerLabel(next.name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer
                )
                Text(
                    text = next.moment.display(),
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
private fun PrayerTimesCard(day: PrayerDay, nextName: PrayerName) {
    val rows = listOf(
        PrayerName.FAJR to day.fajr,
        PrayerName.SUNRISE to day.sunrise,
        PrayerName.DHUHR to day.dhuhr,
        PrayerName.ASR to day.asr,
        PrayerName.MAGHRIB to day.maghrib,
        PrayerName.ISHA to day.isha
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
            rows.forEachIndexed { index, (name, moment) ->
                if (index > 0) {
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.12f))
                }
                val highlighted = name == nextName
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
                        text = prayerLabel(name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = moment.display(),
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
private fun ExtraTimesRow(day: PrayerDay) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ExtraCard(
            title = stringResource(R.string.last_third),
            value = day.lastThird.display(),
            modifier = Modifier.weight(1f)
        )
        ExtraCard(
            title = stringResource(R.string.imsak),
            value = day.imsak.display(),
            modifier = Modifier.weight(1f)
        )
        ExtraCard(
            title = stringResource(R.string.midnight),
            value = day.midnight.display(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ExtraCard(title: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorCard() {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.calc_error_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.calc_error_message),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onErrorContainer
            )
        }
    }
}

@Composable
private fun rememberNow(): Instant {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1000)
        }
    }
    return now
}
