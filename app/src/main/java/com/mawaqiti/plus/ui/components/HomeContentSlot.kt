package com.mawaqiti.plus.ui.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.R
import com.mawaqiti.plus.format.DailyDhikr
import com.mawaqiti.plus.format.EventCountdown
import com.mawaqiti.plus.format.EventCountdownCalculator
import com.mawaqiti.plus.format.HijriDateFormatter
import com.mawaqiti.plus.format.IslamicEventTarget
import com.mawaqiti.plus.format.MoonIllumination
import com.mawaqiti.plus.format.MoonPhaseKind
import com.mawaqiti.plus.ui.theme.NidaaDimens
import com.mawaqiti.plus.ui.theme.Tajawal
import com.mawaqiti.plus.ui.theme.skyPalette
import com.mawaqiti.plus.ui.theme.starTextExclusion
import java.time.LocalDate
import kotlin.math.abs

/**
 * beta25: the adaptive home content slot.
 *
 * It sits between the «الثلث الأخير» row and the dock, and only ever uses the
 * height the home column has left after its own content. The widgets are
 * candidates in a fixed priority order (dhikr, countdown, moon phase) and
 * [FittingColumn] measures each one before it is shown: a widget that does not
 * fit the remaining height is not composed at all, so the slot can neither
 * clip a widget nor push the home content. When nothing fits, the slot renders
 * nothing.
 */
@Composable
fun HomeContentSlot(
    date: LocalDate,
    dhikrEnabled: Boolean,
    countdownEnabled: Boolean,
    moonEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!dhikrEnabled && !countdownEnabled && !moonEnabled) return

    val hijri = remember(date) { HijriDateFormatter.toHijri(date) }
    val dhikrList = stringArrayResource(R.array.daily_dhikr_list)
    val dhikr = remember(date, dhikrList, dhikrEnabled) {
        if (!dhikrEnabled || dhikrList.isEmpty()) {
            null
        } else {
            val index = DailyDhikr.index(DailyDhikr.hijriEpochDay(date), dhikrList.size)
            dhikrList[index]
        }
    }
    val countdown = remember(hijri, countdownEnabled) {
        if (countdownEnabled) EventCountdownCalculator.next(hijri) else null
    }

    // Verification trail for the countdown: one line per Hijri day, stating the
    // date the app resolved, the chosen target and the whole-day delta.
    LaunchedEffect(date, countdown?.target) {
        val chosen = countdown
        if (chosen != null) {
            Log.d(
                "NidaaHomeContent",
                "hijri=${hijri.year}-${hijri.month}-${hijri.day} " +
                    "target=${chosen.target.name} days=${chosen.daysLeft}"
            )
        }
    }

    val widgets = mutableListOf<@Composable () -> Unit>()
    dhikr?.let { text -> widgets.add({ DhikrWidget(text) }) }
    countdown?.let { value -> widgets.add({ CountdownWidget(value) }) }
    if (moonEnabled) {
        widgets.add({ MoonWidget(hijri.day) })
    }
    if (widgets.isEmpty()) return

    FittingColumn(
        gap = NidaaDimens.homeSlotGap,
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_content_slot")
    ) {
        widgets
    }
}

/**
 * Stacks [widgets] top to bottom in priority order inside the exact height the
 * parent gives it.
 *
 * A candidate is measured with the remaining height as its maximum; if it is
 * taller than what is left it is dropped (never shrunk, never clipped) and the
 * next candidate is still considered, so a short slot can show the first
 * widget that fits. The stack is centred vertically.
 */
@Composable
private fun FittingColumn(
    gap: Dp,
    modifier: Modifier = Modifier,
    widgets: () -> List<@Composable () -> Unit>
) {
    val gapPx = with(LocalDensity.current) { gap.roundToPx() }
    val candidates = widgets()
    if (candidates.isEmpty()) return

    SubcomposeLayout(modifier = modifier) { constraints ->
        val bounded = constraints.maxHeight != Constraints.Infinity
        val limit = if (bounded) constraints.maxHeight else Int.MAX_VALUE / 2

        var used = 0
        val placeables = ArrayList<Placeable>(candidates.size)
        for ((index, content) in candidates.withIndex()) {
            val needed = if (placeables.isEmpty()) 0 else gapPx
            if (used + needed >= limit) break
            val child = subcompose(index, content).first().measure(
                Constraints(
                    maxWidth = constraints.maxWidth,
                    maxHeight = limit - used - needed
                )
            )
            if (used + needed + child.height > limit) break
            used += needed + child.height
            placeables += child
        }

        val height = if (bounded) constraints.maxHeight.coerceAtLeast(used) else used
        layout(constraints.maxWidth, height) {
            var y = ((height - used) / 2).coerceAtLeast(0)
            placeables.forEachIndexed { index, placeable ->
                if (index > 0) y += gapPx
                placeable.place(
                    x = (constraints.maxWidth - placeable.width) / 2,
                    y = y
                )
                y += placeable.height
            }
        }
    }
}

/** «ذكر اليوم»: one centred phrase, at most two lines, no card behind it. */
@Composable
private fun DhikrWidget(text: String) {
    val palette = skyPalette
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = Tajawal,
            lineHeight = 26.sp
        ),
        color = palette.textMuted,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .starTextExclusion("home_slot_dhikr")
            .testTag("home_widget_dhikr")
            .padding(horizontal = 6.dp)
    )
}

/** «عدّاد المناسبات»: the nearest occasion in the gold accent. */
@Composable
private fun CountdownWidget(countdown: EventCountdown) {
    val palette = skyPalette
    val target = stringResource(countdown.target.labelRes())
    val text = if (countdown.isToday) {
        stringResource(R.string.event_today, target)
    } else {
        val days = countdown.daysLeft.toInt()
        pluralStringResource(R.plurals.event_days_left, days, days, target)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontFamily = Tajawal),
        fontWeight = FontWeight.Bold,
        color = palette.accent,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .starTextExclusion("home_slot_countdown")
            .testTag("home_widget_countdown")
            .padding(horizontal = 6.dp)
    )
}

/** «طور القمر»: a drawn disc plus its phase name. */
@Composable
private fun MoonWidget(hijriDay: Int) {
    val palette = skyPalette
    val phase = MoonPhaseKind.forHijriDay(hijriDay)
    val lit = MoonIllumination.illuminatedFraction(hijriDay)
    val waxing = MoonIllumination.waxing(hijriDay)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .starTextExclusion("home_slot_moon")
            .testTag("home_widget_moon")
    ) {
        Canvas(modifier = Modifier.size(NidaaDimens.homeMoonSize)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val disc = Path().apply {
                addOval(
                    Rect(
                        left = center.x - radius,
                        top = center.y - radius,
                        right = center.x + radius,
                        bottom = center.y + radius
                    )
                )
            }

            // Shadow first: the dark surface token, so the unlit limb reads as
            // the card surface rather than a hole in the sky.
            drawPath(disc, palette.sheet)
            clipPath(disc) {
                // The lit half, then the terminator ellipse that turns it into
                // a crescent (lit < half) or a gibbous disc (lit > half).
                drawArc(
                    color = palette.accent,
                    startAngle = if (waxing) -90f else 90f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                val terminator = radius * abs(2f * lit - 1f)
                drawOval(
                    color = if (lit > 0.5f) palette.accent else palette.sheet,
                    topLeft = Offset(center.x - terminator, center.y - radius),
                    size = Size(terminator * 2f, radius * 2f)
                )
            }
            drawCircle(
                color = palette.accent.copy(alpha = 0.45f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        Spacer(Modifier.height(NidaaDimens.homeMoonLabelGap))
        Text(
            text = stringResource(R.string.moon_label, stringResource(phase.labelRes())),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun IslamicEventTarget.labelRes(): Int = when (this) {
    IslamicEventTarget.RAMADAN -> R.string.event_ramadan
    IslamicEventTarget.EID_FITR -> R.string.event_eid_fitr
    IslamicEventTarget.EID_ADHA -> R.string.event_eid_adha
    IslamicEventTarget.NEW_YEAR -> R.string.event_new_year
}

private fun MoonPhaseKind.labelRes(): Int = when (this) {
    MoonPhaseKind.NEW_CRESCENT -> R.string.moon_phase_new
    MoonPhaseKind.WAXING_CRESCENT -> R.string.moon_phase_waxing_crescent
    MoonPhaseKind.FIRST_QUARTER -> R.string.moon_phase_first_quarter
    MoonPhaseKind.WAXING_GIBBOUS -> R.string.moon_phase_waxing_gibbous
    MoonPhaseKind.FULL -> R.string.moon_phase_full
    MoonPhaseKind.WANING_GIBBOUS -> R.string.moon_phase_waning_gibbous
    MoonPhaseKind.LAST_QUARTER -> R.string.moon_phase_last_quarter
    MoonPhaseKind.WANING_CRESCENT -> R.string.moon_phase_waning_crescent
    MoonPhaseKind.OLD_CRESCENT -> R.string.moon_phase_old
}
