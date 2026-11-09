package com.mawaqiti.plus.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.TimeFormatter
import com.mawaqiti.plus.ui.components.GlassCard
import com.mawaqiti.plus.ui.components.dashedOutline
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.SkyDecorBand
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.mawaqiti.plus.ui.theme.Tajawal
import com.mawaqiti.plus.ui.theme.skyPalette
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
    clockFormat: ClockFormat,
    onChooseCity: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = skyPalette
    val now = rememberClock()
    val hero = remember(day, now) { heroState(day, now) }
    val progress by animateFloatAsState(
        targetValue = hero.progress,
        animationSpec = tween(900, easing = LinearEasing),
        label = "prayerProgress"
    )
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 18.dp)
    ) {
        Header(
            city = city,
            date = date,
            isArabic = isArabic,
            palette = palette,
            onChooseCity = onChooseCity,
            onToggleLanguage = onToggleLanguage
        )

        SkyDecorBand()

        HeroCard(
            hero = hero,
            progress = progress,
            isArabic = isArabic,
            clockFormat = clockFormat,
            palette = palette
        )

        Spacer(Modifier.height(14.dp))

        PrayerGrid(
            day = day,
            next = hero.event,
            isArabic = isArabic,
            clockFormat = clockFormat,
            palette = palette
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FooterButton(
                text = stringResource(R.string.footer_qibla),
                onClick = onOpenQibla,
                modifier = Modifier.weight(1f)
            )
            FooterButton(
                text = stringResource(R.string.footer_settings),
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.official_note),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun Header(
    city: City,
    date: LocalDate,
    isArabic: Boolean,
    palette: SkyPalette,
    onChooseCity: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontFamily = AmiriBold,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 42.sp,
            color = palette.text
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CityChip(city = city, isArabic = isArabic, palette = palette, onClick = onChooseCity)
            Spacer(Modifier.width(8.dp))
            LanguagePill(isArabic = isArabic, palette = palette, onClick = onToggleLanguage)
        }
    }

    Spacer(Modifier.height(8.dp))

    Text(
        text = date.format(dateFormatter(isArabic)),
        style = MaterialTheme.typography.bodyMedium,
        color = palette.textMuted,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End
    )

    Spacer(Modifier.height(6.dp))
}

@Composable
private fun CityChip(
    city: City,
    isArabic: Boolean,
    palette: SkyPalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(palette.chipFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = palette.text,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = city.label(isArabic),
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 190.dp)
        )
        Spacer(Modifier.width(3.dp))
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = stringResource(R.string.city_chip_desc),
            tint = palette.text,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun LanguagePill(
    isArabic: Boolean,
    palette: SkyPalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(palette.chipFill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isArabic) "EN" else "ع",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = palette.text
        )
    }
}

private data class HeroState(
    val event: TableEvent,
    val minutes: Int,
    val secondsLeft: Long,
    val progress: Float
)

private fun heroState(day: TableDay, now: LocalTime): HeroState {
    val nowSeconds = now.toSecondOfDay().toLong()
    val events = listOf(
        TableEvent.FAJR to day.fajr,
        TableEvent.SUNRISE to day.sunrise,
        TableEvent.DHUHR to day.dhuhr,
        TableEvent.ASR to day.asr,
        TableEvent.MAGHRIB to day.maghrib,
        TableEvent.ISHA to day.isha
    )
    val index = events.indexOfFirst { it.second * 60L > nowSeconds }

    if (index >= 0) {
        val (event, minutes) = events[index]
        val startSeconds = if (index == 0) {
            day.ishaYesterday * 60L - 86_400L
        } else {
            events[index - 1].second * 60L
        }
        return HeroState(
            event = event,
            minutes = minutes,
            secondsLeft = minutes * 60L - nowSeconds,
            progress = fraction(nowSeconds, startSeconds, minutes * 60L)
        )
    }

    val nextSeconds = (day.fajrTomorrow + 1_440L) * 60L
    return HeroState(
        event = TableEvent.FAJR,
        minutes = day.fajrTomorrow,
        secondsLeft = nextSeconds - nowSeconds,
        progress = fraction(nowSeconds, day.isha * 60L, nextSeconds)
    )
}

private fun fraction(now: Long, start: Long, end: Long): Float {
    if (end <= start) return 0f
    return ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun HeroCard(
    hero: HeroState,
    progress: Float,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    palette: SkyPalette
) {
    GlassCard(
        emphasized = true,
        corner = 22.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 20.dp,
            vertical = 18.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.next_prayer),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = prayerLabel(hero.event),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = palette.text
            )
            Text(
                text = TimeFormatter.format(hero.minutes, clockFormat, isArabic),
                fontFamily = Tajawal,
                fontSize = 54.sp,
                lineHeight = 62.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (palette.dark) palette.accent.copy(alpha = 0.14f)
                        else Color.White.copy(alpha = 0.30f)
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.remaining_short) + " " +
                        TimeFormatter.formatCountdown(hero.secondsLeft),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (palette.dark) palette.accent else palette.text
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (palette.dark) 0.16f else 0.32f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                )
            }
        }
    }
}

private data class GridItem(
    val labelRes: Int,
    val minutes: Int,
    val dashed: Boolean = false
)

@Composable
private fun PrayerGrid(
    day: TableDay,
    next: TableEvent,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    palette: SkyPalette
) {
    val prayers = listOf(
        TableEvent.FAJR to R.string.prayer_fajr,
        TableEvent.SUNRISE to R.string.prayer_sunrise,
        TableEvent.DHUHR to R.string.prayer_dhuhr,
        TableEvent.ASR to R.string.prayer_asr,
        TableEvent.MAGHRIB to R.string.prayer_maghrib,
        TableEvent.ISHA to R.string.prayer_isha
    )

    val items = ArrayList<GridItem>()
    for ((event, label) in prayers) {
        if (event == next) continue
        items += GridItem(label, day.minutesFor(event))
    }
    items += GridItem(R.string.last_third, day.lastThird, dashed = true)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    GridCard(
                        item = item,
                        isArabic = isArabic,
                        clockFormat = clockFormat,
                        palette = palette,
                        modifier = if (row.size == 1) Modifier.fillMaxWidth() else Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GridCard(
    item: GridItem,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    palette: SkyPalette,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val base = if (item.dashed) {
        modifier
            .clip(shape)
            .background(palette.cardFill)
            .dashedOutline(
                color = if (palette.dark) palette.text.copy(alpha = 0.55f)
                else palette.text.copy(alpha = 0.50f)
            )
    } else {
        modifier
            .clip(shape)
            .background(palette.cardFill)
    }

    Row(
        modifier = base.padding(horizontal = 13.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = TimeFormatter.format(item.minutes, clockFormat, isArabic),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.text,
            maxLines = 1
        )
    }
}

@Composable
private fun FooterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = skyPalette
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.cardFill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = palette.text
        )
    }
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

private fun dateFormatter(isArabic: Boolean): DateTimeFormatter {
    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH
    val pattern = if (isArabic) "EEEE، d MMMM yyyy" else "EEEE, d MMMM yyyy"
    return DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.STANDARD)
}

@Composable
private fun rememberClock(): LocalTime {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1_000)
        }
    }
    return now
}
