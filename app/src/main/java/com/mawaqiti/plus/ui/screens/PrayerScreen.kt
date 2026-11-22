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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.mawaqiti.plus.R
import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.engine.table.TableEvent
import com.mawaqiti.plus.format.ClockFormat
import com.mawaqiti.plus.format.HijriDateFormatter
import com.mawaqiti.plus.format.TimeFormatter
import com.mawaqiti.plus.ui.components.GlassCard
import com.mawaqiti.plus.ui.components.HomeContentSlot
import com.mawaqiti.plus.ui.components.PrayerIcons
import com.mawaqiti.plus.ui.components.dashedOutline
import com.mawaqiti.plus.ui.theme.AmiriBold
import com.mawaqiti.plus.ui.theme.CelestialIcon
import com.mawaqiti.plus.ui.theme.CelestialPhase
import com.mawaqiti.plus.ui.theme.GoldenHeroDecor
import com.mawaqiti.plus.ui.theme.NidaaDimens
import com.mawaqiti.plus.ui.theme.PhaseBounds
import com.mawaqiti.plus.ui.theme.starTextExclusion
import com.mawaqiti.plus.ui.theme.SkyPalette
import com.mawaqiti.plus.ui.theme.celestialPhase
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
    onOpenAbout: () -> Unit = {},
    updateRelease: ReleaseInfo? = null,
    onOpenUpdate: () -> Unit = {},
    /** beta25: home content slot visibility, from Settings → «محتوى الشاشة الرئيسية». */
    homeDhikr: Boolean = true,
    homeCountdown: Boolean = false,
    homeMoonPhase: Boolean = false
) {
    val palette = skyPalette
    val now = rememberForegroundClock()
    val hero = remember(day, now) { heroState(day, now) }
    // beta10: "now" line in the hero card. It is derived from the same ticker,
    // but keyed on the minute, so the text is rebuilt once per minute at most.
    val nowMinute = now.hour * 60 + now.minute
    val nowText = remember(nowMinute, clockFormat, isArabic) {
        TimeFormatter.format(nowMinute, clockFormat, isArabic)
    }
    // beta23: the card's celestial indicator. It reads only the engine's own
    // times, and it is keyed on the same minute tick the countdown uses, so the
    // sun/moon swaps exactly on the computed prayer minute.
    val celestial = remember(day, nowMinute) {
        celestialPhase(nowMinute, PhaseBounds.fromDay(day))
    }
    val progress by animateFloatAsState(
        targetValue = hero.progress,
        animationSpec = tween(900, easing = LinearEasing),
        label = "prayerProgress"
    )
    val scroll = rememberScrollState()

    // beta25: the home is one region of two children - the unchanged scrollable
    // list and the adaptive content slot under it.
    //
    // A weighted split cannot express this: `weight` divides the height before
    // measuring, so two weighted children would each take half and the list
    // would be clipped at the halfway line. This layout measures the list FIRST
    // with the full window height; `verticalScroll` then reports
    // min(contentHeight, windowHeight), which is exactly what is needed:
    //   - content shorter than the window -> it keeps its natural height and the
    //     slot is handed the remainder;
    //   - content as tall as the window   -> the remainder is 0, the slot is
    //     measured at 0 dp and composes nothing, and the list scrolls as before.
    // The slot can therefore never push, clip or overlap the home content.
    Layout(
        content = {
            Column(modifier = Modifier.verticalScroll(scroll)) {
                Header(
                    city = city,
                    date = date,
                    isArabic = isArabic,
                    palette = palette,
                    onChooseCity = onChooseCity,
                    onOpenAbout = onOpenAbout
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
                    palette = palette,
                    nowText = nowText,
                    celestial = celestial
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

                Spacer(Modifier.height(10.dp))
            }

            HomeContentSlot(
                date = date,
                dhikrEnabled = homeDhikr,
                countdownEnabled = homeCountdown,
                moonEnabled = homeMoonPhase,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val content = measurables.first().measure(
            Constraints(maxWidth = width, maxHeight = constraints.maxHeight)
        )
        val leftover = (constraints.maxHeight - content.height).coerceAtLeast(0)
        val slot = measurables.getOrNull(1)?.measure(
            Constraints(
                minWidth = width,
                maxWidth = width,
                minHeight = leftover,
                maxHeight = leftover
            )
        )
        layout(width, constraints.maxHeight) {
            content.place(0, 0)
            slot?.place(0, content.height)
        }
    }
}

@Composable
private fun Header(
    city: City,
    date: LocalDate,
    isArabic: Boolean,
    palette: SkyPalette,
    onChooseCity: () -> Unit,
    onOpenAbout: () -> Unit
) {
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // beta10: the wordmark is the About entry point.
        Text(
            text = stringResource(R.string.brand_name),
            fontFamily = AmiriBold,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 42.sp,
            color = palette.text,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenAbout)
                .starTextExclusion("home_brand")
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        CityChip(city = city, isArabic = isArabic, palette = palette, onClick = onChooseCity)
    }

    Spacer(Modifier.height(6.dp))

    // beta10: one row, two halves. In RTL the Hijri date (first child) sits at
    // the right and the Gregorian date at the left; each is centred inside its
    // half. `autoSize` shrinks the text on narrow screens instead of wrapping
    // or ellipsizing, and `maxLines = 1` guarantees a single line.
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // `date` is the ViewModel's local "today", so both lines roll over
        // together at local midnight. Hijri is computational (Umm al-Qura).
        Text(
            text = HijriDateFormatter.format(date, isArabic),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = MaterialTheme.typography.bodySmall.fontSize
            ),
            modifier = Modifier
                .weight(1f)
                .starTextExclusion("home_hijri_date")
        )
        Text(
            text = date.format(dateFormatter(isArabic)),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = MaterialTheme.typography.bodyMedium.fontSize
            ),
            modifier = Modifier
                .weight(1f)
                .starTextExclusion("home_gregorian_date")
        )
    }

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
internal fun CityChip(
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

internal data class HeroState(
    val event: TableEvent,
    val minutes: Int,
    val secondsLeft: Long,
    val progress: Float
)

internal fun heroState(day: TableDay, now: LocalTime): HeroState {
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
    /**
     * beta15: the label -> prayer-name separation is a theme dimen now
     * ([com.mawaqiti.plus.ui.theme.NidaaDimens.nextPrayerLabelGap]) instead of a
     * literal in this file, so the anti-overlap rule has one home. Still 8 dp.
     */
    val LABEL_TO_NAME = NidaaDimens.nextPrayerLabelGap
    val NAME_TO_TIME = 4.dp
}

/**
 * Beta14 typography of the current time, kept as named values (like
 * [HeroMetrics]) so the "readable at arm's length" size survives future layout
 * changes and is asserted by [NowTimeMetricsTest].
 *
 * beta14 removes the clock icon entirely: the row is a single text line
 * ("الوقت الآن" + the app's formatted time), so there is no icon box to
 * misalign and no optical correction to maintain. The type keeps beta12's
 * 20 sp on a 26 sp line - still above the 18 sp floor - in the golden accent at
 * full opacity, with tabular digits so the minute never jitters.
 */
object NowTimeMetrics {
    val FONT_SIZE = 20.sp
    val LINE_HEIGHT = 26.sp

    /** Prefix shown before the time; the Arabic text lives in `strings.xml`. */
    val PREFIX_RES = R.string.current_time_prefix
}

/**
 * beta16: the "الصلاة القادمة" caption, now its own standalone row at the top of
 * the hero card and the second-loudest element in the card after the prayer
 * time.
 *
 * beta15 kept it as a small, muted, start-aligned line because its task 2 was
 * only the anti-overlap guard; the owner's screenshot showed the result: still a
 * corner label. beta16 promotes it: centred on the card's visual axis, 17 sp
 * (was 15 sp), Bold (was the body font), in the theme's gold accent (was
 * `textMuted` cream). No badge, no pill, nothing behind it.
 *
 * The colour comes from [SkyPalette.accent] - the same `ReferenceGold` token the
 * divider, the prayer time and the progress bar already use - never a literal,
 * so every phase keeps its own accent.
 */
object NextPrayerLabelMetrics {
    /** 17 sp: clearly above the 15 sp body size, inside the 16-18 sp target. */
    val FONT_SIZE = 17.sp
    val LINE_HEIGHT = 24.sp

    /** Bold is the heaviest real Tajawal face bundled with the app. */
    val FONT_WEIGHT = FontWeight.Bold

    /** The deeper gold of the active phase (never a hardcoded colour). */
    fun color(palette: SkyPalette) = palette.accent
}

/**
 * beta14: the current time row at the bottom of the next-prayer card.
 *
 * Text only - [NowTimeMetrics.PREFIX_RES] + " HH:MM" - in the golden accent at
 * full opacity, 20 sp SemiBold, tabular digits, one line.
 *
 * beta15: the row is horizontally CENTRED as one unit. It is wrap-content (so
 * its box is the rendered pair, not a full-width container) and the parent
 * Column centres it, which keeps the pair exactly on the card's vertical axis
 * without reintroducing the beta14 start-edge pin. `textAlign = Center` keeps
 * the ink centred even if a future caller gives the node extra width.
 *
 * `maxLines = 1` + `softWrap = false` + ellipsis guarantee that no screen width
 * and no font scale can wrap, overlap or droop the line; the beta13 full-bleed
 * `sheet` scrim behind the divider keeps the mosque silhouette from crossing
 * the glyphs.
 */
@Composable
private fun NowTimeRow(text: String, palette: SkyPalette) {
    Text(
        text = stringResource(NowTimeMetrics.PREFIX_RES) + " " + text,
        style = MaterialTheme.typography.titleSmall.copy(
            fontFamily = Tajawal,
            fontSize = NowTimeMetrics.FONT_SIZE,
            lineHeight = NowTimeMetrics.LINE_HEIGHT,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum"
        ),
        color = palette.accent,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .starTextExclusion("hero_now_row")
            .testTag("now_time_row")
    )
}

@Composable
internal fun HeroCard(
    hero: HeroState,
    progress: Float,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    palette: SkyPalette,
    nowText: String,
    celestial: CelestialPhase
) {
    GlassCard(
        modifier = Modifier.testTag("hero_card"),
        emphasized = true,
        corner = 24.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Stars and the arcade silhouette, drawn behind the content but only in
        // the top strip and along the bottom edge.
        GoldenHeroDecor(Modifier.matchParentSize())

        // beta23: the celestial indicator. It replaces the single fixed
        // crescent the card used to paint, and follows the computed prayer
        // times (moon at night, rising/full/low/setting sun by day).
        CelestialIcon(
            phase = celestial,
            palette = palette,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 10.dp)
                .size(84.dp)
                .testTag("hero_celestial")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_content"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // beta16: the caption is its own row at the top of the card, centred
            // on the card's visual axis and set in the deeper gold accent at
            // 17 sp Bold - a prominent standalone line, not a corner label. It
            // still owns the full card width and is a single ellipsized line, so
            // it can never wrap, clip or collide with the prayer name below it;
            // the clear space stays [NidaaDimens.nextPrayerLabelGap].
            Text(
                text = stringResource(R.string.next_prayer),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = Tajawal,
                    fontSize = NextPrayerLabelMetrics.FONT_SIZE,
                    lineHeight = NextPrayerLabelMetrics.LINE_HEIGHT,
                    fontWeight = NextPrayerLabelMetrics.FONT_WEIGHT,
                    letterSpacing = 0.sp
                ),
                color = NextPrayerLabelMetrics.color(palette),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .starTextExclusion("hero_next_label")
                    .testTag("next_prayer_label")
            )
            Spacer(Modifier.height(HeroMetrics.LABEL_TO_NAME))
            // beta15: the flexible element. It may only ever shrink into an
            // ellipsis, never wrap onto a second line and never grow past the
            // card width, so it cannot collide with the caption above it. It is
            // deliberately wrap-content: the parent Column centres it, and
            // keeping its box on the ink makes the regression test measure the
            // real rendered width instead of a full-width container.
            Text(
                text = prayerLabel(hero.event),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .starTextExclusion("hero_prayer_name")
                    .testTag("hero_prayer_name")
            )
            Spacer(Modifier.height(HeroMetrics.NAME_TO_TIME))
            Text(
                text = TimeFormatter.format(hero.minutes, clockFormat, isArabic),
                fontFamily = Tajawal,
                fontSize = 44.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
                modifier = Modifier.starTextExclusion("hero_prayer_time")
            )
            Spacer(Modifier.height(6.dp))
            CountdownPill(secondsLeft = hero.secondsLeft, palette = palette)
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

            // beta12: the current time owns a dedicated row at the bottom of the
            // card, separated by a thin golden hairline (faded at both ends so
            // it reads as a divider, not a border).
            Spacer(Modifier.height(9.dp))
            // beta13: the in-card mosque silhouette reaches this strip, so its
            // dome and minaret edges could cross the gold time text. A soft
            // `sheet` scrim behind the divider + row gives the text a uniform
            // dark band to sit on - the row stays exactly where it was and the
            // silhouette is still visible above it. Theme token only.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Full-bleed scrim: the GlassCard clips to its rounded
                        // shape, so bleeding 16 dp sideways and 12 dp down makes
                        // the band reach the card edges instead of looking like
                        // a rectangle inset inside the card.
                        val bleedX = 16.dp.toPx()
                        val bleedBottom = 12.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to palette.sheet.copy(alpha = 0f),
                                0.55f to palette.sheet.copy(alpha = 0.74f),
                                1f to palette.sheet.copy(alpha = 0.90f),
                                startY = 0f,
                                endY = size.height + bleedBottom
                            ),
                            topLeft = Offset(-bleedX, 0f),
                            size = Size(size.width + bleedX * 2f, size.height + bleedBottom)
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("now_divider")
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        palette.accent.copy(alpha = 0f),
                                        palette.accent.copy(alpha = 0.28f),
                                        palette.accent.copy(alpha = 0f)
                                    )
                                )
                            )
                    )
                    Spacer(Modifier.height(7.dp))
                    NowTimeRow(text = nowText, palette = palette)
                }
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

/**
 * beta23.1: the "متبقي 00:15:09" pill under the hero time, as ONE [Text].
 *
 * beta17 shipped one concatenated [Text], beta18 split it into two [Text]s in a
 * [Row] and beta23 gave both children `Modifier.alignByBaseline()`. A native
 * screenshot of the beta23 build (411 dp, font scale 1.0) shows the two runs
 * already ended on the same pixel row - ink top 777 and ink bottom 803 for the
 * label and for the digits - so none of those attempts had a baseline offset
 * left to remove, which is why the pill never looked different. What the eye
 * reports is a font-metric fact, not a layout one: Tajawal's lining digits are
 * full cap height while the Arabic body sits far lower, so the digits read as
 * *taller*, never as *higher*.
 *
 * The pill is one [Text] again, but not beta17's flat concatenation: the label
 * and the digits are two runs of one [AnnotatedString], each carrying its own
 * [SpanStyle]. One run pair in one layout means one baseline for both runs by
 * construction - no `alignByBaseline`, no vertical padding compensation and no
 * offsets can make them drift - and the digits keep their tabular figures via
 * `fontFeatureSettings = "tnum"` on the shared [androidx.compose.ui.text.TextStyle].
 *
 * RTL order needs no handling: the label is the leading run, so it is laid out
 * on the right and the LTR digits follow it. Shape, colour, padding, type size
 * and type family are unchanged from beta23.
 */
@Composable
private fun CountdownPill(secondsLeft: Long, palette: SkyPalette) {
    val label = stringResource(R.string.remaining_short)
    val digits = TimeFormatter.formatCountdown(secondsLeft)
    val text = remember(label, digits, palette.accent) {
        buildAnnotatedString {
            // Leading run: the Arabic label (right-most in RTL).
            withStyle(
                SpanStyle(
                    fontFamily = Tajawal,
                    fontWeight = FontWeight.Medium,
                    color = palette.accent
                )
            ) { append(label) }
            // The word gap is the shared run's space glyph; it belongs to the
            // paragraph, so bidi resolves it between the two directions.
            append(' ')
            // Trailing run: the countdown digits (LTR, one numeric run).
            withStyle(
                SpanStyle(
                    fontFamily = Tajawal,
                    fontWeight = FontWeight.Medium,
                    color = palette.accent
                )
            ) { append(digits) }
        }
    }
    Text(
        text = text,
        fontFamily = Tajawal,
        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.Medium,
        color = palette.accent,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.20f))
            .border(1.dp, palette.accent.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .starTextExclusion("hero_countdown_pill")
            .testTag("hero_countdown_pill")
    )
}

@Composable
private fun PrayerGrid(
    day: TableDay,
    next: TableEvent,
    isArabic: Boolean,
    clockFormat: ClockFormat,
    palette: SkyPalette
) {
    val prayers = listOf(
        // beta10: Fajr is a rising sun (rays above the horizon) and Maghrib a
        // setting sun (touching the horizon, no rays). Both are hand-made
        // vectors in the icon library's absence; the other four keep beta9.
        Triple(TableEvent.FAJR, R.string.prayer_fajr, PrayerIcons.Sunrise),
        Triple(TableEvent.SUNRISE, R.string.prayer_sunrise, Icons.Filled.WbTwilight),
        Triple(TableEvent.DHUHR, R.string.prayer_dhuhr, Icons.Filled.WbSunny),
        Triple(TableEvent.ASR, R.string.prayer_asr, Icons.Filled.LightMode),
        Triple(TableEvent.MAGHRIB, R.string.prayer_maghrib, PrayerIcons.Sunset),
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
            .starTextExclusion("last_third_row")
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
        // beta24 (Task 3): the whole card, not just its two text lines - no
        // star behind a prayer card, and therefore none inside the 24 dp
        // margin of its label or time.
        .starTextExclusion("prayer_card_${item.labelRes}")
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
internal fun prayerLabel(event: TableEvent): String = when (event) {
    TableEvent.FAJR -> stringResource(R.string.prayer_fajr)
    TableEvent.SUNRISE -> stringResource(R.string.prayer_sunrise)
    TableEvent.DHUHR -> stringResource(R.string.prayer_dhuhr)
    TableEvent.ASR -> stringResource(R.string.prayer_asr)
    TableEvent.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    TableEvent.ISHA -> stringResource(R.string.prayer_isha)
}

internal fun dateFormatter(isArabic: Boolean): DateTimeFormatter {
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
internal fun rememberForegroundClock(): LocalTime {
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
