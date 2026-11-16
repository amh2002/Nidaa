package com.mawaqiti.plus.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.mawaqiti.plus.BuildConfig
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.HijriDateFormatter
import com.mawaqiti.plus.format.TimeFormatter
import com.mawaqiti.plus.ui.components.GlassCard
import com.mawaqiti.plus.ui.components.dashedOutline
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.GoldenHeroDecor
import com.mawaqiti.plus.ui.theme.GoldenPhase
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.mawaqiti.plus.ui.theme.Tajawal
import com.mawaqiti.plus.ui.theme.skyPalette
import com.nidaa.prayer.update.ReleaseInfo
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
    onOpenQibla: () -> Unit,
    onOpenSettings: () -> Unit,
    updateRelease: ReleaseInfo? = null,
    onOpenUpdate: () -> Unit = {},
    debugPhase: GoldenPhase? = null,
    onDebugPhase: (GoldenPhase?) -> Unit = {}
) {
    val palette = skyPalette
    val now = rememberForegroundClock()
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
            onChooseCity = onChooseCity
        )

        if (updateRelease != null) {
            Spacer(Modifier.height(8.dp))
            UpdateBanner(release = updateRelease, onOpen = onOpenUpdate)
        }

        Spacer(Modifier.height(12.dp))

        HeroCard(
            hero = hero,
            progress = progress,
            isArabic = isArabic,
            clockFormat = clockFormat,
            palette = palette
        )

        Spacer(Modifier.height(12.dp))

        PrayerGrid(
            day = day,
            next = hero.event,
            isArabic = isArabic,
            clockFormat = clockFormat,
            palette = palette
        )

        Spacer(Modifier.height(10.dp))

        LastThirdRow(
            minutes = day.lastThird,
            isArabic = isArabic,
            clockFormat = clockFormat,
            palette = palette
        )

        Spacer(Modifier.height(12.dp))

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

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.official_note),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(6.dp))
            DebugPhaseBar(current = debugPhase, onSelect = onDebugPhase)
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun Header(
    city: City,
    date: LocalDate,
    isArabic: Boolean,
    palette: SkyPalette,
    onChooseCity: () -> Unit
) {
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.brand_name),
            fontFamily = AmiriBold,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 42.sp,
            color = palette.text
        )
        CityChip(city = city, isArabic = isArabic, palette = palette, onClick = onChooseCity)
    }

    Spacer(Modifier.height(6.dp))

    Text(
        text = date.format(dateFormatter(isArabic)),
        style = MaterialTheme.typography.bodyMedium,
        color = palette.textMuted,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End
    )

    Spacer(Modifier.height(2.dp))

    // Computational Umm al-Qura date directly under the Gregorian line. It is
    // deliberately a little smaller and dimmer than the date above; `date` is
    // the ViewModel's local "today", so the line rolls over at local midnight.
    Text(
        text = HijriDateFormatter.format(date, isArabic),
        style = MaterialTheme.typography.bodySmall,
        color = palette.textMuted,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End
    )

    Spacer(Modifier.height(10.dp))

    // Thin gold rule, the reference's separator between header and hero.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        palette.accent.copy(alpha = 0.45f),
                        Color.Transparent
                    )
                )
            )
    )

    Spacer(Modifier.height(14.dp))
}

@Composable
private fun CityChip(
    city: City,
    isArabic: Boolean,
    palette: SkyPalette,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(palette.chipFill)
            .border(1.dp, palette.chipBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = city.label(isArabic),
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // beta 7.9: keeps the whole city visible at the largest in-app scale.
            autoSize = TextAutoSize.StepBased(
                minFontSize = 9.sp,
                maxFontSize = MaterialTheme.typography.titleSmall.fontSize
            ),
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

/**
 * Vertical rhythm of the hero card. These are explicit, unit-tested values:
 * 8 dp between the "next prayer" label and the prayer name, 4 dp between the
 * name and its time. No fixed card height is involved, so a large font scale
 * grows the card instead of colliding the lines.
 */
object HeroMetrics {
    val LABEL_TO_NAME = 8.dp
    val NAME_TO_TIME = 4.dp
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
        corner = 24.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Crescent, stars, clouds and skyline, drawn behind the content but
        // only in the top-right and bottom strips.
        GoldenHeroDecor(Modifier.matchParentSize())

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.next_prayer),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted
            )
            Spacer(Modifier.height(HeroMetrics.LABEL_TO_NAME))
            Text(
                text = prayerLabel(hero.event),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = palette.text
            )
            Spacer(Modifier.height(HeroMetrics.NAME_TO_TIME))
            Text(
                text = TimeFormatter.format(hero.minutes, clockFormat, isArabic),
                fontFamily = Tajawal,
                fontSize = 44.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.20f))
                    .border(1.dp, palette.accent.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.remaining_short) + " " +
                        TimeFormatter.formatCountdown(hero.secondsLeft),
                    fontFamily = Tajawal,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = palette.accent
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    palette.accent.copy(alpha = 0.75f),
                                    palette.accent
                                )
                            )
                        )
                )
            }
        }
    }
}

private data class GridItem(
    val labelRes: Int,
    val minutes: Int,
    val icon: ImageVector,
    val highlighted: Boolean = false
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
        Triple(TableEvent.FAJR, R.string.prayer_fajr, Icons.Filled.Nightlight),
        Triple(TableEvent.SUNRISE, R.string.prayer_sunrise, Icons.Filled.WbTwilight),
        Triple(TableEvent.DHUHR, R.string.prayer_dhuhr, Icons.Filled.WbSunny),
        Triple(TableEvent.ASR, R.string.prayer_asr, Icons.Filled.LightMode),
        Triple(TableEvent.MAGHRIB, R.string.prayer_maghrib, Icons.Filled.Bedtime),
        Triple(TableEvent.ISHA, R.string.prayer_isha, Icons.Filled.NightsStay)
    )

    val items = prayers.map { (event, label, icon) ->
        GridItem(
            labelRes = label,
            minutes = day.minutesFor(event),
            icon = icon,
            highlighted = event == next
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Adaptive 3 / 2 columns. `maxWidth` is in dp, so the choice does not
        // change with the user's font scale, only with the actual width.
        val columns = if (maxWidth >= 300.dp) 3 else 2

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { item ->
                        GridCard(
                            item = item,
                            isArabic = isArabic,
                            clockFormat = clockFormat,
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Slim dashed row for the last third, so the grid stays a clean 3 x 2. */
@Composable
private fun LastThirdRow(
    minutes: Int,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    palette: SkyPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.cardFill)
            .dashedOutline(color = palette.accent.copy(alpha = 0.50f), corner = 14.dp)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Stars,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.last_third),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = TimeFormatter.format(minutes, clockFormat, isArabic),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = palette.text
        )
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
    val shape = RoundedCornerShape(16.dp)
    val base = modifier
        .clip(shape)
        .background(if (item.highlighted) palette.heroFill else palette.cardFill)
        .border(
            width = if (item.highlighted) 1.5.dp else 1.dp,
            color = if (item.highlighted) palette.accent.copy(alpha = 0.80f)
            else palette.cardBorder,
            shape = shape
        )
        .padding(horizontal = 8.dp, vertical = 8.dp)

    Column(
        modifier = base,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (item.highlighted) palette.accent else palette.text.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = if (item.highlighted) palette.text else palette.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            // beta 7.9: at the largest in-app x system scale the longest label
            // ("Last third") must still be readable instead of ellipsized.
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = MaterialTheme.typography.bodySmall.fontSize
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = TimeFormatter.format(item.minutes, clockFormat, isArabic),
            style = MaterialTheme.typography.titleSmall,
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
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(palette.cardFill)
            .border(1.dp, palette.accent.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = palette.accent
        )
    }
}

/**
 * DEBUG-only phase switch. Stripped from release builds because the caller
 * checks BuildConfig.DEBUG (a compile-time constant) before composing it.
 */
@Composable
private fun DebugPhaseBar(
    current: GoldenPhase?,
    onSelect: (GoldenPhase?) -> Unit
) {
    val palette = skyPalette
    val phases = listOf<GoldenPhase?>(
        null,
        GoldenPhase.NIGHT,
        GoldenPhase.DAWN,
        GoldenPhase.MORNING,
        GoldenPhase.AFTERNOON,
        GoldenPhase.SUNSET
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        phases.forEach { phase ->
            val selected = phase == current
            val shape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(
                        if (selected) palette.accent.copy(alpha = 0.22f)
                        else Color.White.copy(alpha = 0.06f)
                    )
                    .border(
                        1.dp,
                        if (selected) palette.accent.copy(alpha = 0.8f)
                        else Color.White.copy(alpha = 0.16f),
                        shape
                    )
                    .clickable { onSelect(phase) }
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = phase?.key ?: "auto",
                    fontFamily = Tajawal,
                    fontSize = 8.sp,
                    color = if (selected) palette.accent else palette.textMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
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

/**
 * Clock that ticks once a second **only while the screen is resumed**.
 *
 * The observer is registered on the hosting activity (found by walking the
 * context wrappers, so no lifecycle-compose dependency is needed) and is
 * removed on dispose. ON_PAUSE cancels the ticking coroutine, ON_RESUME
 * restarts it and refreshes immediately, so the countdown never runs in the
 * background and never shows a stale value after returning.
 */
@Composable
private fun rememberForegroundClock(): LocalTime {
    val owner = LocalContext.current.lifecycleOwnerOrNull()
    var now by remember { mutableStateOf(LocalTime.now()) }
    var foreground by remember {
        mutableStateOf(
            owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
        )
    }

    if (owner != null) {
        DisposableEffect(owner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> foreground = true
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> foreground = false
                    else -> Unit
                }
            }
            owner.lifecycle.addObserver(observer)
            onDispose { owner.lifecycle.removeObserver(observer) }
        }
    }

    LaunchedEffect(foreground) {
        if (!foreground) return@LaunchedEffect
        now = LocalTime.now()
        while (true) {
            delay(1_000)
            now = LocalTime.now()
        }
    }
    return now
}

private fun Context.lifecycleOwnerOrNull(): LifecycleOwner? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is LifecycleOwner) return current
        current = current.baseContext
    }
    return current as? LifecycleOwner
}
