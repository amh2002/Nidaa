package com.mawaqiti.plus.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.HijriDateFormatter
import com.mawaqiti.plus.format.TimeFormatter
import com.mawaqiti.plus.ui.components.GlassCard
import com.mawaqiti.plus.ui.components.PrayerIcons
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.CelestialIcon
import com.mawaqiti.plus.ui.theme.CelestialPhase
import com.mawaqiti.plus.ui.theme.NidaaDimens
import com.mawaqiti.plus.ui.theme.PhaseBounds
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.mawaqiti.plus.ui.theme.celestialPhase
import com.mawaqiti.plus.ui.theme.Tajawal
import com.mawaqiti.plus.ui.theme.TvCompactDimens
import com.mawaqiti.plus.ui.theme.TvDimens
import com.mawaqiti.plus.ui.theme.TvMetrics
import com.mawaqiti.plus.ui.theme.skyPalette
import com.nidaa.prayer.update.ReleaseInfo
import java.time.LocalDate

/**
 * beta21: the landscape TV dashboard, selected by «وضع العرض ← تلفاز».
 *
 * Composition, top to bottom, all on one screen with no scrolling:
 *
 * 1. Header: the نداء wordmark (About entry), the Gregorian + Hijri date lines,
 *    and the city chip (city picker) - the same entry points as the phone home.
 * 2. Hero panel: the next prayer + its time, a large countdown, the live
 *    current time and the between-prayers progress bar.
 * 3. Five prayer cards in a single full-width RTL row. In RTL the first child
 *    (الفجر) lands on the right edge, so the day reads right to left; the card
 *    matching the next prayer is emphasized and tagged «الصلاة القادمة».
 * 4. Footer: sunrise, last third and the official-timetable note.
 *
 * Every size comes from [TvDimens] / [TvCompactDimens] (chosen from the panel
 * height), and every colour from the current golden palette - no literals.
 * The dock is the unchanged beta12/13 golden pill supplied by the Scaffold.
 */
@Composable
fun TvHomeScreen(
    city: City,
    day: TableDay,
    date: LocalDate,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    onChooseCity: () -> Unit,
    onOpenAbout: () -> Unit = {},
    updateRelease: ReleaseInfo? = null,
    onOpenUpdate: () -> Unit = {}
) {
    val palette = skyPalette
    // The same foreground-only ticker as the phone home: the countdown and the
    // current-time line update once a second while the screen is resumed.
    val now = rememberForegroundClock()
    val hero = remember(day, now) { heroState(day, now) }
    val nowMinute = now.hour * 60 + now.minute
    // beta23: the same phase resolver the phone hero uses, so the TV dashboard
    // and the phone can never disagree about sun vs moon.
    val celestial = remember(day, nowMinute) {
        celestialPhase(nowMinute, PhaseBounds.fromDay(day))
    }
    val nowText = remember(nowMinute, clockFormat, isArabic) {
        TimeFormatter.format(nowMinute, clockFormat, isArabic)
    }
    val progress by animateFloatAsState(
        targetValue = hero.progress,
        animationSpec = tween(900, easing = LinearEasing),
        label = "tvPrayerProgress"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tv_home")
    ) {
        // Short panels (960 x 540 class) use tighter tokens so font scale 1.3
        // still fits without clipping; 1280 x 720 panels use the full sizes.
        val metrics = if (maxHeight < NidaaDimens.tvCompactHeight) {
            TvCompactDimens
        } else {
            TvDimens
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = metrics.screenPaddingH,
                    vertical = metrics.screenPaddingV
                )
        ) {
            TvHeader(
                city = city,
                date = date,
                isArabic = isArabic,
                updateRelease = updateRelease,
                onChooseCity = onChooseCity,
                onOpenAbout = onOpenAbout,
                onOpenUpdate = onOpenUpdate,
                metrics = metrics,
                palette = palette
            )

            Spacer(Modifier.height(metrics.rowGap))

            TvHeroPanel(
                hero = hero,
                progress = progress,
                nowText = nowText,
                celestial = celestial,
                isArabic = isArabic,
                clockFormat = clockFormat,
                metrics = metrics,
                palette = palette,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(Modifier.height(metrics.rowGap))

            TvPrayerRow(
                day = day,
                next = hero.event,
                isArabic = isArabic,
                clockFormat = clockFormat,
                metrics = metrics,
                palette = palette,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(Modifier.height(metrics.rowGap))

            TvFooter(
                day = day,
                isArabic = isArabic,
                clockFormat = clockFormat,
                metrics = metrics,
                palette = palette
            )
        }
    }
}

@Composable
private fun TvHeader(
    city: City,
    date: LocalDate,
    isArabic: Boolean,
    updateRelease: ReleaseInfo?,
    onChooseCity: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenUpdate: () -> Unit,
    metrics: TvMetrics,
    palette: SkyPalette
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // RTL: the wordmark is the first child, so it sits at the right edge.
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.brand_name),
                fontFamily = AmiriBold,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.brandSize,
                lineHeight = metrics.brandSize * 1.25f,
                color = palette.text,
                modifier = Modifier
                    .clickable(onClick = onOpenAbout)
                    .testTag("tv_brand")
            )
        }

        Column(
            modifier = Modifier.weight(1.3f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.format(dateFormatter(isArabic)),
                fontFamily = Tajawal,
                fontSize = metrics.dateSize,
                lineHeight = metrics.dateSize * 1.4f,
                color = palette.textMuted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = HijriDateFormatter.format(date, isArabic),
                fontFamily = Tajawal,
                fontSize = metrics.dateSize,
                lineHeight = metrics.dateSize * 1.4f,
                color = palette.textMuted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            CityChip(
                city = city,
                isArabic = isArabic,
                palette = palette,
                onClick = onChooseCity
            )
            if (updateRelease != null) {
                Spacer(Modifier.height(metrics.headerGap))
                Text(
                    text = stringResource(
                        R.string.settings_updates_banner,
                        updateRelease.tag.removePrefix("v")
                    ),
                    fontFamily = Tajawal,
                    fontSize = metrics.footerSize,
                    lineHeight = metrics.footerSize * 1.4f,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.accent.copy(alpha = 0.14f))
                        .clickable(onClick = onOpenUpdate)
                        .padding(
                            horizontal = metrics.pillPaddingH,
                            vertical = metrics.pillPaddingV
                        )
                        .testTag("tv_update_banner")
                )
            }
        }
    }
}

@Composable
private fun TvHeroPanel(
    hero: HeroState,
    progress: Float,
    nowText: String,
    celestial: CelestialPhase,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    metrics: TvMetrics,
    palette: SkyPalette,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.testTag("tv_hero_card"),
        corner = metrics.heroCorner,
        contentPadding = PaddingValues(
            horizontal = metrics.heroPaddingH,
            vertical = metrics.heroPaddingV
        ),
        emphasized = true
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RTL: next-prayer caption + name on the right.
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CelestialIcon(
                        phase = celestial,
                        palette = palette,
                        modifier = Modifier
                            .size(metrics.icon * 2f)
                            .testTag("tv_celestial")
                    )
                    Spacer(Modifier.height(metrics.cardTextGap))
                    Text(
                        text = stringResource(R.string.next_prayer),
                        fontFamily = Tajawal,
                        fontSize = metrics.heroLabelSize,
                        lineHeight = metrics.heroLabelSize * 1.4f,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("tv_next_label")
                    )
                    Spacer(Modifier.height(NidaaDimens.nextPrayerLabelGap))
                    Text(
                        text = prayerLabel(hero.event),
                        fontFamily = Tajawal,
                        fontSize = metrics.heroNameSize,
                        lineHeight = metrics.heroNameSize * 1.2f,
                        fontWeight = FontWeight.Bold,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("tv_next_name")
                    )
                }

                // The loudest element: the next prayer time, readable at distance.
                Text(
                    text = TimeFormatter.format(hero.minutes, clockFormat, isArabic),
                    fontFamily = Tajawal,
                    fontSize = metrics.heroTimeSize,
                    lineHeight = metrics.heroTimeSize * 1.15f,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_next_time")
                )

                // Countdown + live current time on the left.
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.accent.copy(alpha = 0.14f))
                            .border(
                                width = metrics.pillBorder,
                                color = palette.accent.copy(alpha = 0.40f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(
                                horizontal = metrics.pillPaddingH,
                                vertical = metrics.pillPaddingV
                            )
                            .testTag("tv_countdown_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // beta23: caption and digits now share one baseline via
                        // `alignByBaseline()` instead of relying on the row's
                        // vertical centring. Centring aligns the *boxes*, which
                        // leaves the two ink baselines apart whenever the font
                        // metrics differ (always, between Arabic letters and
                        // tabular digits). Same contract as the phone pill.
                        Text(
                            text = stringResource(R.string.remaining_short),
                            fontFamily = Tajawal,
                            fontSize = metrics.countdownSize,
                            fontWeight = FontWeight.Medium,
                            color = palette.accent,
                            maxLines = 1,
                            modifier = Modifier
                                .alignByBaseline()
                                .testTag("tv_countdown_caption")
                        )
                        Spacer(Modifier.width(NidaaDimens.countdownWordGap))
                        Text(
                            text = TimeFormatter.formatCountdown(hero.secondsLeft),
                            fontFamily = Tajawal,
                            fontSize = metrics.countdownSize,
                            fontWeight = FontWeight.Medium,
                            color = palette.accent,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .alignByBaseline()
                                .testTag("tv_countdown_value")
                        )
                    }
                    Spacer(Modifier.height(metrics.cardTextGap))
                    Text(
                        text = stringResource(R.string.current_time_prefix) + " " + nowText,
                        fontFamily = Tajawal,
                        fontSize = metrics.nowSize,
                        lineHeight = metrics.nowSize * 1.3f,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accent,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("tv_now_time")
                    )
                }
            }

            Spacer(Modifier.height(metrics.progressGap))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.progressHeight)
                    .clip(CircleShape)
                    .background(palette.text.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(metrics.progressHeight)
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

private data class TvPrayer(
    val event: TableEvent,
    @StringRes val labelRes: Int,
    val minutes: Int,
    val icon: ImageVector
)

@Composable
private fun TvPrayerRow(
    day: TableDay,
    next: TableEvent,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    metrics: TvMetrics,
    palette: SkyPalette,
    modifier: Modifier = Modifier
) {
    // Fajr first: in RTL the Row lays the first child at the right edge.
    val prayers = listOf(
        TvPrayer(TableEvent.FAJR, R.string.prayer_fajr, day.fajr, PrayerIcons.Sunrise),
        TvPrayer(TableEvent.DHUHR, R.string.prayer_dhuhr, day.dhuhr, Icons.Filled.WbSunny),
        TvPrayer(TableEvent.ASR, R.string.prayer_asr, day.asr, Icons.Filled.LightMode),
        TvPrayer(TableEvent.MAGHRIB, R.string.prayer_maghrib, day.maghrib, PrayerIcons.Sunset),
        TvPrayer(TableEvent.ISHA, R.string.prayer_isha, day.isha, Icons.Filled.NightsStay)
    )

    Row(
        modifier = modifier.testTag("tv_prayer_row"),
        horizontalArrangement = Arrangement.spacedBy(metrics.cardGap)
    ) {
        prayers.forEach { prayer ->
            TvPrayerCard(
                prayer = prayer,
                highlighted = prayer.event == next,
                isArabic = isArabic,
                clockFormat = clockFormat,
                metrics = metrics,
                palette = palette,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("tv_card_${prayer.event.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun TvPrayerCard(
    prayer: TvPrayer,
    highlighted: Boolean,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    metrics: TvMetrics,
    palette: SkyPalette,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        corner = metrics.cardCorner,
        contentPadding = PaddingValues(
            horizontal = metrics.cardPaddingH,
            vertical = metrics.cardPaddingV
        ),
        emphasized = highlighted
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = prayer.icon,
                contentDescription = null,
                tint = if (highlighted) palette.accent else palette.text.copy(alpha = 0.72f),
                modifier = Modifier.size(metrics.icon)
            )
            Spacer(Modifier.height(metrics.sectionGap))
            Text(
                text = stringResource(prayer.labelRes),
                fontFamily = Tajawal,
                fontSize = metrics.prayerNameSize,
                lineHeight = metrics.prayerNameSize * 1.3f,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (highlighted) palette.text else palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(metrics.cardTextGap))
            Text(
                text = TimeFormatter.format(prayer.minutes, clockFormat, isArabic),
                fontFamily = Tajawal,
                fontSize = metrics.prayerTimeSize,
                lineHeight = metrics.prayerTimeSize * 1.2f,
                fontWeight = FontWeight.Bold,
                color = if (highlighted) palette.accent else palette.text,
                maxLines = 1,
                softWrap = false
            )
            if (highlighted) {
                Spacer(Modifier.height(metrics.cardTextGap))
                Text(
                    text = stringResource(R.string.next_prayer),
                    fontFamily = Tajawal,
                    fontSize = metrics.footerSize,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.accent.copy(alpha = 0.16f))
                        .padding(
                            horizontal = metrics.pillPaddingH,
                            vertical = metrics.pillPaddingV
                        )
                )
            }
        }
    }
}

@Composable
private fun TvFooter(
    day: TableDay,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    metrics: TvMetrics,
    palette: SkyPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tv_footer"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(metrics.columnGap)
    ) {
        // RTL: sunrise first (right), then last third, then the official note.
        TvFooterItem(
            icon = Icons.Filled.WbTwilight,
            label = stringResource(R.string.prayer_sunrise),
            minutes = day.sunrise,
            isArabic = isArabic,
            clockFormat = clockFormat,
            metrics = metrics,
            palette = palette,
            modifier = Modifier.weight(1f)
        )
        TvFooterItem(
            icon = Icons.Filled.Stars,
            label = stringResource(R.string.last_third),
            minutes = day.lastThird,
            isArabic = isArabic,
            clockFormat = clockFormat,
            metrics = metrics,
            palette = palette,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.official_note),
            fontFamily = Tajawal,
            fontSize = metrics.footerSize,
            lineHeight = metrics.footerSize * 1.3f,
            color = palette.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Composable
private fun TvFooterItem(
    icon: ImageVector,
    label: String,
    minutes: Int,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    metrics: TvMetrics,
    palette: SkyPalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(metrics.footerIcon)
        )
        Spacer(Modifier.width(metrics.cardTextGap))
        Text(
            text = label,
            fontFamily = Tajawal,
            fontSize = metrics.footerSize,
            color = palette.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(metrics.cardTextGap))
        Text(
            text = TimeFormatter.format(minutes, clockFormat, isArabic),
            fontFamily = Tajawal,
            fontSize = metrics.footerTimeSize,
            fontWeight = FontWeight.Bold,
            color = palette.text,
            maxLines = 1,
            softWrap = false
        )
    }
}
