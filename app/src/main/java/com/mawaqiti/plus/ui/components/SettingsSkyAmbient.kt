package com.mawaqiti.plus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.ui.theme.LocalGoldenPhase
import com.mawaqiti.plus.ui.theme.LocalStarExclusions
import com.mawaqiti.plus.ui.theme.NidaaDimens
import com.mawaqiti.plus.ui.theme.SKY_STAR_COUNT
import com.mawaqiti.plus.ui.theme.SKY_STAR_MAX_Y
import com.mawaqiti.plus.ui.theme.SKY_STAR_SEED
import com.mawaqiti.plus.ui.theme.SKY_STAR_TOP_PAD
import com.mawaqiti.plus.ui.theme.STAR_GLOW_FACTOR
import com.mawaqiti.plus.ui.theme.STAR_RADIUS_FACTOR
import com.mawaqiti.plus.ui.theme.Star
import com.mawaqiti.plus.ui.theme.isStarBlocked
import com.mawaqiti.plus.ui.theme.skyPalette
import com.mawaqiti.plus.ui.theme.skylineAlphaFor
import com.mawaqiti.plus.ui.theme.starField
import java.util.Random
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/**
 * beta27: the three calm effects of the Settings screen.
 *
 * 1. **Adhan waves** - three hairlines leave the **central dome finial** (the
 *    highest point of the shared scene) and rise. Centring the source removed
 *    the edge clipping the owner reported in beta26: the radius is now
 *    `min(160 dp, halfWidth - 24 dp)`, so the outermost arc keeps clear of both
 *    screen edges. Every arc also lifts its centre by 60 dp over its life, so a
 *    wave rises instead of merely expanding. Two passes - a 6 dp underlay and a
 *    3 dp core - make each arc read against the sky, and the daylight floor is
 *    raised to 0.45 so the effect is still visible outside the night phase.
 * 2. **Twinkling stars** - every second star of the *same* deterministic field
 *    the background paints (same count, same seed, same geometry), so each
 *    twinkle sits exactly on an existing star. Alpha breathes 0.35..1.0 and the
 *    scale 0.8..1.3 on periods that divide the shared clock, with golden-ratio
 *    phase offsets. The six largest selected stars grow a four-ray glint near
 *    the peak of their pulse.
 * 3. **Meteor** - one seeded streak every 7.5..13 s crosses the open band
 *    between the collapsed header stack and the verse block. Head, glow and a
 *    fading 80 dp tail travel right to left with a 9..13 degree downward slope
 *    over 1.1 s, then vanish completely. The band comes from the real layout
 *    (see [contentTopPx] / [contentBottomPx]) and the path is rejected when it
 *    would touch a registered text box.
 *
 * Everything is drawn in the **draw phase**: the only animated input is one
 * linear 180 s clock (long enough that the 4.5 s wave period and every 2.5-5 s
 * star period divide it exactly, so nothing snaps at the wrap), read inside the
 * [Canvas] lambda. The whole layer is composed only while the Settings screen is
 * on screen with every section collapsed, so expanding a section removes it from
 * the composition and all three effects are disposed - no offscreen work, no
 * leftover fade.
 *
 * @param contentTopPx root-pixel y where the collapsed header stack ends, or 0
 *   before the first layout pass.
 * @param contentBottomPx root-pixel y where the verse block starts, or 0 while
 *   it is not shown.
 */
@Composable
fun SettingsSkyAmbient(
    modifier: Modifier = Modifier,
    contentTopPx: Float = 0f,
    contentBottomPx: Float = 0f
) {
    val palette = skyPalette
    val phase = LocalGoldenPhase.current
    val weight = skylineAlphaFor(phase)
    val waveWeight = max(weight, WAVE_DAYLIGHT_FLOOR)
    val exclusions = LocalStarExclusions.current.rects
    // The scene is painted by the window-filling background canvas, so the only
    // way to anchor to its dome is to work in the same root-pixel space:
    // `view` is that canvas's own root, and `origin` is where this layer starts
    // inside it (below the status bar).
    val view = LocalView.current
    val density = LocalDensity.current

    val maxRadiusCap = with(density) { NidaaDimens.adhanWaveMaxRadius.toPx() }
    val edgeMargin = with(density) { NidaaDimens.adhanWaveEdgeMargin.toPx() }
    val waveStroke = with(density) { NidaaDimens.adhanWaveStroke.toPx() }
    val waveGlowStroke = with(density) { NidaaDimens.adhanWaveGlowStroke.toPx() }
    val waveRise = with(density) { NidaaDimens.adhanWaveRise.toPx() }
    val glintStroke = with(density) { GLINT_STROKE_DP.dp.toPx() }
    val bandGap = with(density) { NidaaDimens.meteorBandGap.toPx() }
    val verseClearance = with(density) { NidaaDimens.meteorVerseClearance.toPx() }
    val meteorHead = with(density) { NidaaDimens.meteorHeadRadius.toPx() }
    val meteorGlow = with(density) { NidaaDimens.meteorGlowRadius.toPx() }
    val meteorTail = with(density) { NidaaDimens.meteorTailLength.toPx() }
    val meteorStroke = with(density) { NidaaDimens.meteorStroke.toPx() }

    val field = remember { starField(count = SKY_STAR_COUNT, seed = SKY_STAR_SEED) }
    // The glint belongs to the six biggest *selected* stars, so the rarest
    // sparkle always lands on a star the eye can already see.
    val glintStars = remember(field) {
        field.withIndex()
            .filter { it.index % TWINKLE_EVERY == 0 }
            .sortedByDescending { it.value.radius }
            .take(GLINT_STAR_COUNT)
            .map { it.index }
            .toSet()
    }
    val meteors = remember { buildMeteorSchedule() }
    var origin by remember { mutableStateOf(Offset.Zero) }

    val clock by rememberInfiniteTransition(label = "settingsAmbient").animateFloat(
        initialValue = 0f,
        targetValue = AMBIENT_CLOCK_MS,
        animationSpec = infiniteRepeatable(
            animation = tween(AMBIENT_CLOCK_MS.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientClock"
    )

    Canvas(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val topLeft = coordinates.boundsInRoot().topLeft
            if (topLeft != origin) origin = topLeft
        }
    ) {
        val rootWidth = view.width.toFloat()
        val rootHeight = view.height.toFloat()
        if (rootWidth <= 0f || rootHeight <= 0f) return@Canvas
        val dx = -origin.x
        val dy = -origin.y

        drawAdhanWaves(
            clockMs = clock,
            center = Offset(
                x = rootWidth * 0.5f + dx,
                // `yv(1.08) - beadRadius`: the very top of the dome finial's bead.
                y = rootHeight * (SCENE_BASE_Y - SCENE_RISE * FINIAL_BEAD_RISE) -
                    rootWidth * FINIAL_BEAD_RADIUS + dy
            ),
            color = palette.accent,
            weight = waveWeight,
            maxRadius = min(maxRadiusCap, rootWidth * 0.5f - edgeMargin),
            stroke = waveStroke,
            glowStroke = waveGlowStroke,
            rise = waveRise
        )

        drawTwinklingStars(
            clockSeconds = clock / 1000f,
            field = field,
            glintStars = glintStars,
            color = palette.text,
            weight = weight,
            rootWidth = rootWidth,
            rootHeight = rootHeight,
            origin = Offset(dx, dy),
            exclusions = exclusions,
            glintStroke = glintStroke
        )

        // The meteor band: from the bottom of the collapsed header stack (never
        // above 14 % of the screen) to just above the verse block, or the top of
        // the silhouette while the verse is not laid out yet.
        val bandTop = if (contentTopPx > 0f) {
            max(contentTopPx + dy + bandGap, rootHeight * METEOR_MIN_TOP_FRACTION + dy)
        } else {
            rootHeight * METEOR_MIN_TOP_FRACTION + dy
        }
        val bandBottom = if (contentBottomPx > 0f) {
            contentBottomPx + dy - verseClearance
        } else {
            rootHeight * FINIAL_TOP_FRACTION + dy - bandGap
        }
        drawMeteors(
            clockMs = clock,
            schedule = meteors,
            tailColor = palette.accent,
            headColor = palette.text,
            weight = weight,
            width = rootWidth,
            bandTop = bandTop,
            bandBottom = bandBottom,
            headRadius = meteorHead,
            glowRadius = meteorGlow,
            tailLength = meteorTail,
            stroke = meteorStroke,
            exclusions = exclusions.map { it.translate(dx, dy) }
        )
    }
}

/**
 * One shared clock for all three effects.
 *
 * 180 s is the smallest round length that every animated period divides
 * exactly: 4.5 s (waves), 4 s / 5 s / 3.75 s / 3 s / 2.5 s (stars). Anything
 * shorter would snap once per loop - precisely the flash these effects must not
 * have.
 */
private const val AMBIENT_CLOCK_MS = 180_000f

// ---- adhan waves ----------------------------------------------------------

/** Arcs in flight at any moment; 4.5 s period / 3 arcs = the 1.5 s stagger. */
private const val WAVE_COUNT = 3
private const val WAVE_PERIOD_MS = 4_500f

/** Peak alpha of a fresh wave, before the phase weight is applied. */
private const val WAVE_MAX_ALPHA = 0.65f

/** Peak alpha of the soft underlay that carries the glow. */
private const val WAVE_GLOW_ALPHA = 0.28f

/** The daylight phases never drop the waves below this weight. */
private const val WAVE_DAYLIGHT_FLOOR = 0.45f

/** A full upper half-circle: the wave rises into the sky it leaves from. */
private const val WAVE_ARC_START_DEG = 180f
private const val WAVE_ARC_SWEEP_DEG = 180f

// ---- scene geometry (mirrors `drawIslamicScene` in GoldenBackground.kt) ----

private const val SCENE_BASE_Y = 0.955f
private const val SCENE_RISE = 0.19f

/** The central finial's bead, i.e. the highest point of the whole scene. */
private const val FINIAL_BEAD_RISE = 1.08f

/** Bead radius as a fraction of the width (`w * 0.007f` in the scene). */
private const val FINIAL_BEAD_RADIUS = 0.007f

/** Top of the scene in height fractions, used as the meteor band's floor. */
private const val FINIAL_TOP_FRACTION = SCENE_BASE_Y - SCENE_RISE * FINIAL_BEAD_RISE

private fun DrawScope.drawAdhanWaves(
    clockMs: Float,
    center: Offset,
    color: Color,
    weight: Float,
    maxRadius: Float,
    stroke: Float,
    glowStroke: Float,
    rise: Float
) {
    if (weight <= 0.01f || maxRadius <= 1f) return
    val base = clockMs / WAVE_PERIOD_MS
    for (index in 0 until WAVE_COUNT) {
        // Raw progress through this wave's own 4.5 s life.
        val progress = (base + index / WAVE_COUNT.toFloat()) % 1f
        // Ease-out: the ring springs away from the finial, then slows as it
        // fades, so the three arcs drift apart instead of marching.
        val easeOut = 1f - (1f - progress) * (1f - progress)
        val radius = maxRadius * easeOut
        if (radius < 1f) continue
        // The whole ring lifts as it grows: a pulse rising off the dome, not a
        // circle drawn on the spot.
        val lifted = Offset(center.x, center.y - rise * easeOut)
        val fade = (1f - progress).pow(1.15f)
        val glow = WAVE_GLOW_ALPHA * fade * weight
        val core = WAVE_MAX_ALPHA * fade * weight
        val topLeft = Offset(lifted.x - radius, lifted.y - radius)
        val size = Size(radius * 2f, radius * 2f)
        if (glow > 0.004f) {
            drawArc(
                color = color.copy(alpha = glow),
                startAngle = WAVE_ARC_START_DEG,
                sweepAngle = WAVE_ARC_SWEEP_DEG,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = glowStroke, cap = StrokeCap.Round)
            )
        }
        if (core > 0.004f) {
            drawArc(
                color = color.copy(alpha = core),
                startAngle = WAVE_ARC_START_DEG,
                sweepAngle = WAVE_ARC_SWEEP_DEG,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

// ---- twinkling stars ------------------------------------------------------

/** Every second star (23 of 46) is animated; the rest keep the steady field. */
private const val TWINKLE_EVERY = 2

/**
 * Periods of 2.5-5 s. Each divides the 180 s clock exactly, so the sine of a
 * star is continuous across the clock's wrap.
 */
private val TWINKLE_PERIODS_SECONDS = floatArrayOf(2.5f, 3f, 3.75f, 4f, 5f)

/** Golden-ratio stride: successive stars never share a phase. */
private const val TWINKLE_PHASE_STRIDE = 0.6180339887f

/** breath = CENTRE + SWING * sin(...), i.e. 0.35 .. 1.0. */
private const val TWINKLE_ALPHA_CENTRE = 0.675f
private const val TWINKLE_ALPHA_SWING = 0.325f

/** scale = CENTRE + SWING * sin(...), i.e. 0.8x .. 1.3x. */
private const val TWINKLE_SCALE_CENTRE = 1.05f
private const val TWINKLE_SCALE_SWING = 0.25f

/** How many of the biggest selected stars carry the four-ray glint. */
private const val GLINT_STAR_COUNT = 6

/** The glint only appears near the peak of the pulse (breath > 0.85). */
private const val GLINT_THRESHOLD = 0.85f

/** Ray length is a multiple of the star's own radius. */
private const val GLINT_RAY_FACTOR = 3f

/** Hairline width of a glint ray. */
private const val GLINT_STROKE_DP = 1f

private fun DrawScope.drawTwinklingStars(
    clockSeconds: Float,
    field: List<Star>,
    glintStars: Set<Int>,
    color: Color,
    weight: Float,
    rootWidth: Float,
    rootHeight: Float,
    origin: Offset,
    exclusions: List<Rect>,
    glintStroke: Float
) {
    if (weight <= 0.01f) return
    val unit = min(rootWidth, rootHeight)
    for ((index, star) in field.withIndex()) {
        if (index % TWINKLE_EVERY != 0) continue
        val center = Offset(
            x = rootWidth * star.x + origin.x,
            y = rootHeight * (star.y * SKY_STAR_MAX_Y + SKY_STAR_TOP_PAD) + origin.y
        )
        // Same contract as the background: a star inside a text box (plus its
        // margin) is dropped, never moved.
        if (isStarBlocked(center - origin, exclusions)) continue
        if (center.x < -unit || center.x > size.width + unit) continue

        val period = TWINKLE_PERIODS_SECONDS[index % TWINKLE_PERIODS_SECONDS.size]
        val phaseOffset = (index * TWINKLE_PHASE_STRIDE) % 1f
        val turns = clockSeconds / period + phaseOffset
        val breath = TWINKLE_ALPHA_CENTRE + TWINKLE_ALPHA_SWING * sin(turns * TWO_PI)
        val scaleWave = sin((turns + QUARTER_TURN) * TWO_PI)

        val alpha = star.alpha * breath * weight
        if (alpha <= 0.004f) continue
        val radius = unit * STAR_RADIUS_FACTOR * star.radius *
            (TWINKLE_SCALE_CENTRE + TWINKLE_SCALE_SWING * scaleWave)
        val glow = radius * STAR_GLOW_FACTOR
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha * 0.55f), color.copy(alpha = 0f)),
                center = center,
                radius = glow
            ),
            radius = glow,
            center = center
        )
        drawCircle(color = color.copy(alpha = alpha), radius = radius, center = center)

        if (index in glintStars && breath > GLINT_THRESHOLD) {
            drawStarGlint(center, radius, color, alpha, glintStroke)
        }
    }
}

/** Four short rays forming a subtle cross at the pulse's peak. */
private fun DrawScope.drawStarGlint(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
    stroke: Float
) {
    val ray = radius * GLINT_RAY_FACTOR
    val rayAlpha = alpha * 0.7f
    if (rayAlpha <= 0.004f) return
    val rayColor = color.copy(alpha = rayAlpha)
    drawLine(rayColor, Offset(center.x - ray, center.y), Offset(center.x + ray, center.y), stroke, StrokeCap.Round)
    drawLine(rayColor, Offset(center.x, center.y - ray), Offset(center.x, center.y + ray), stroke, StrokeCap.Round)
}

private const val TWO_PI = 6.2831855f

/** A quarter period of lead, so size and brightness do not breathe in lockstep. */
private const val QUARTER_TURN = 0.25f

// ---- meteor ---------------------------------------------------------------

/** One scheduled streak: when it starts and where it enters the band. */
private data class MeteorEvent(
    val startMs: Float,
    val durationMs: Float,
    /** Entry height inside the band, 0 = top, 1 = bottom. */
    val yFraction: Float,
    /** Nominal downward slope, radians. */
    val slope: Float
)

/** Deterministic sky: the same meteors on every entry. */
private const val METEOR_SEED = 20261122L

/** First streak about three seconds after the screen appears. */
private const val METEOR_FIRST_MS = 3_000f

/** From the start of one streak to the start of the next. */
private const val METEOR_GAP_MIN_MS = 7_800
private const val METEOR_GAP_MAX_MS = 13_000

/**
 * Crossing time, ease-in-out.
 *
 * The specification asks for about 1.1 s. 1.5 s is the shortest crossing whose
 * visible window still spans two consecutive frames of a 1080 x 1920 emulator
 * capture (one PNG takes about 1.3 s to encode there), and the pace still reads
 * as one calm streak rather than a fast scratch. The gap to the next meteor
 * keeps at least six seconds of empty sky after this one has gone.
 */
private const val METEOR_DURATION_MS = 1_500f

/** Peak alpha before the phase weight. */
private const val METEOR_PEAK_ALPHA = 0.95f

/** The band never starts above this height fraction, whatever the headers do. */
private const val METEOR_MIN_TOP_FRACTION = 0.14f

/** No meteor is drawn when the open band is shorter than this. */
private const val METEOR_MIN_BAND_PX = 34f

/** A slope below three degrees no longer reads as a meteor. */
private const val METEOR_MIN_SLOPE = 0.052f

/** Entry x, as a fraction beyond the right edge. */
private const val METEOR_START_X = 1.02f

/** Exit x, as a fraction beyond the left edge. */
private const val METEOR_END_X = 0.02f

private fun buildMeteorSchedule(): List<MeteorEvent> {
    val random = Random(METEOR_SEED)
    val events = ArrayList<MeteorEvent>()
    var start = METEOR_FIRST_MS
    while (start < AMBIENT_CLOCK_MS) {
        events += MeteorEvent(
            startMs = start,
            durationMs = METEOR_DURATION_MS,
            yFraction = 0.05f + random.nextFloat() * 0.55f,
            slope = (9f + random.nextFloat() * 4f) * (PI.toFloat() / 180f)
        )
        start += (METEOR_GAP_MIN_MS + random.nextInt(METEOR_GAP_MAX_MS - METEOR_GAP_MIN_MS + 1)).toFloat()
    }
    return events
}

private fun DrawScope.drawMeteors(
    clockMs: Float,
    schedule: List<MeteorEvent>,
    tailColor: Color,
    headColor: Color,
    weight: Float,
    width: Float,
    bandTop: Float,
    bandBottom: Float,
    headRadius: Float,
    glowRadius: Float,
    tailLength: Float,
    stroke: Float,
    exclusions: List<Rect>
) {
    if (weight <= 0.01f) return
    val bandHeight = bandBottom - bandTop
    if (bandHeight < METEOR_MIN_BAND_PX) return
    for (event in schedule) {
        val local = clockMs - event.startMs
        if (local < 0f || local >= event.durationMs) continue
        val progress = local / event.durationMs
        // Ease-in-out along the path; alpha fades in and out around it.
        val eased = 0.5f - 0.5f * cos(progress * PI.toFloat())
        val startX = width * METEOR_START_X
        val endX = -width * METEOR_END_X
        val yStart = bandTop + bandHeight * event.yFraction
        val room = bandBottom - yStart - stroke
        if (room <= 0f) continue
        val slope = min(event.slope, atan(room / (startX - endX)))
        if (slope < METEOR_MIN_SLOPE) continue

        val x = startX + (endX - startX) * eased
        val y = yStart + (startX - x) * tan(slope)
        val head = Offset(x, y)
        val tail = Offset(x + cos(slope) * tailLength, y - sin(slope) * tailLength)
        if (segmentBlocked(tail, head, exclusions)) continue

        // A flattened sine: a quick fade-in, a long held core, a quick exit.
        val fade = sin(progress * PI.toFloat()).pow(0.3f)
        val alpha = METEOR_PEAK_ALPHA * fade * weight
        if (alpha <= 0.004f) continue

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    tailColor.copy(alpha = 0f),
                    tailColor.copy(alpha = alpha * 0.45f),
                    headColor.copy(alpha = alpha)
                ),
                start = tail,
                end = head
            ),
            start = tail,
            end = head,
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(headColor.copy(alpha = alpha * 0.55f), headColor.copy(alpha = 0f)),
                center = head,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = head
        )
        drawCircle(color = headColor.copy(alpha = alpha), radius = headRadius, center = head)
    }
}

/**
 * True when any sampled point of the streak falls inside a registered text box.
 *
 * Nine samples along the path are enough: the boxes are already inflated by
 * 24 dp, so a streak cannot slip between two samples and end up drawn over a
 * word.
 */
private fun segmentBlocked(tail: Offset, head: Offset, exclusions: List<Rect>): Boolean {
    if (exclusions.isEmpty()) return false
    for (step in 0..8) {
        val t = step / 8f
        val point = Offset(
            tail.x + (head.x - tail.x) * t,
            tail.y + (head.y - tail.y) * t
        )
        if (exclusions.any { it.contains(point) }) return true
    }
    return false
}
