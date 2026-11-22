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
import com.mawaqiti.plus.ui.theme.LocalGoldenPhase
import com.mawaqiti.plus.ui.theme.LocalStarExclusions
import com.mawaqiti.plus.ui.theme.NidaaDimens
import com.mawaqiti.plus.ui.theme.SKY_STAR_COUNT
import com.mawaqiti.plus.ui.theme.Star
import com.mawaqiti.plus.ui.theme.SKY_STAR_MAX_Y
import com.mawaqiti.plus.ui.theme.SKY_STAR_SEED
import com.mawaqiti.plus.ui.theme.SKY_STAR_TOP_PAD
import com.mawaqiti.plus.ui.theme.STAR_GLOW_FACTOR
import com.mawaqiti.plus.ui.theme.STAR_RADIUS_FACTOR
import com.mawaqiti.plus.ui.theme.isStarBlocked
import com.mawaqiti.plus.ui.theme.skyPalette
import com.mawaqiti.plus.ui.theme.skylineAlphaFor
import com.mawaqiti.plus.ui.theme.starField
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * beta26: the two calm effects of the Settings screen.
 *
 * 1. **Adhan waves** - three hairlines leaving the tip of the scene's right-hand
 *    minaret, staggered a third of a period apart, each growing to
 *    [NidaaDimens.adhanWaveMaxRadius] over four seconds while its alpha falls
 *    from half to zero on an ease-out curve. Only the upper arc of the circle is
 *    drawn (±70° around the vertical), so a wave rises into the sky and never
 *    cuts across the silhouette it leaves from.
 * 2. **Twinkling stars** - every third star of the *same* deterministic field the
 *    background paints (same count, same seed, same geometry), so each twinkle
 *    sits exactly on an existing star instead of being a new one. Alpha breathes
 *    between 0.4 and 1.0 and the radius between 0.85x and 1.15x, each star on its
 *    own 2.5-5 s period with a golden-ratio phase offset, so no two pulse
 *    together.
 *
 * Everything is drawn in the **draw phase**: the only animated input is one
 * linear 60 s clock, read inside the [Canvas] lambda, so a frame invalidates the
 * draw and never recomposes a screen. The whole layer is composed only while the
 * Settings screen is on screen with every section collapsed, so expanding a
 * section removes it from the composition and both animations are disposed -
 * there is no offscreen work and nothing left to fade.
 *
 * Both effects take their colour from the current palette ([skyPalette]) and
 * their weight from [skylineAlphaFor], the same per-phase weight the silhouette
 * uses, so the night state is full strength and the four daylight states are far
 * weaker - the arcs stay legible without competing with the cards.
 */
@Composable
fun SettingsSkyAmbient(modifier: Modifier = Modifier) {
    val palette = skyPalette
    val phase = LocalGoldenPhase.current
    val weight = skylineAlphaFor(phase)
    val exclusions = LocalStarExclusions.current.rects
    // The scene is painted by the window-filling background canvas, so the only
    // way to anchor to its minaret is to work in the same root-pixel space:
    // `view` is that canvas's own root, and `origin` is where this layer starts
    // inside it (below the status bar).
    val view = LocalView.current
    val density = LocalDensity.current
    val waveRadiusPx = with(density) { NidaaDimens.adhanWaveMaxRadius.toPx() }
    val waveStrokePx = with(density) { NidaaDimens.adhanWaveStroke.toPx() }
    val field = remember { starField(count = SKY_STAR_COUNT, seed = SKY_STAR_SEED) }
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
                x = rootWidth * (0.5f + MINARET_X_FROM_CENTRE) + dx,
                y = rootHeight * (SCENE_BASE_Y - SCENE_RISE * MINARET_TIP_RISE) + dy
            ),
            color = palette.accent,
            weight = weight,
            maxRadius = waveRadiusPx,
            stroke = waveStrokePx
        )

        drawTwinklingStars(
            clockSeconds = clock / 1000f,
            field = field,
            color = palette.text,
            weight = weight,
            rootWidth = rootWidth,
            rootHeight = rootHeight,
            origin = Offset(dx, dy),
            exclusions = exclusions
        )
    }
}

/** One 60 s linear clock: every star period divides it evenly. */
private const val AMBIENT_CLOCK_MS = 60_000f

// ---- adhan waves ----------------------------------------------------------

/** Arcs in flight at any moment; 4 s period / 3 arcs = the 1.33 s stagger. */
private const val WAVE_COUNT = 3
private const val WAVE_PERIOD_MS = 4_000f

/** Peak alpha of a fresh wave, before the phase weight is applied. */
private const val WAVE_MAX_ALPHA = 0.5f

/**
 * ±70° around straight up. The arc stays above the minaret tip it leaves from,
 * which is what makes it read as a call rising instead of a circle drawn over
 * the building.
 */
private const val WAVE_ARC_START_DEG = 200f
private const val WAVE_ARC_SWEEP_DEG = 140f

// ---- scene geometry (mirrors `drawIslamicScene` in GoldenBackground.kt) ----

private const val SCENE_BASE_Y = 0.955f
private const val SCENE_RISE = 0.19f

/** Horizontal distance of a minaret from the centre, as a fraction of width. */
private const val MINARET_X_FROM_CENTRE = 0.330f

/** The minaret's bead, i.e. the highest point of the scene: `yv(1.02)`. */
private const val MINARET_TIP_RISE = 1.02f

private fun DrawScope.drawAdhanWaves(
    clockMs: Float,
    center: Offset,
    color: Color,
    weight: Float,
    maxRadius: Float,
    stroke: Float
) {
    if (weight <= 0.01f) return
    val base = clockMs / WAVE_PERIOD_MS
    for (index in 0 until WAVE_COUNT) {
        // Raw progress through this wave's own 4 s life.
        val progress = (base + index / WAVE_COUNT.toFloat()) % 1f
        // Ease-out: the ring springs away from the minaret, then slows as it
        // fades, so the three arcs drift apart instead of marching.
        val easeOut = 1f - (1f - progress) * (1f - progress)
        val radius = maxRadius * easeOut
        val alpha = WAVE_MAX_ALPHA * (1f - progress).pow(1.15f) * weight
        if (radius < 1f || alpha <= 0.004f) continue
        drawArc(
            color = color.copy(alpha = alpha),
            startAngle = WAVE_ARC_START_DEG,
            sweepAngle = WAVE_ARC_SWEEP_DEG,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

// ---- twinkling stars ------------------------------------------------------

/** 1 star in 3 (16 of 46) is animated; the rest keep the steady field. */
private const val TWINKLE_EVERY = 3

/**
 * Periods of 2.5-5 s. Each divides the 60 s clock exactly, so the sine of a star
 * is continuous across the clock's wrap - a period that did not divide it would
 * snap once a minute, which is precisely the flash this effect must not have.
 */
private val TWINKLE_PERIODS_SECONDS = floatArrayOf(2.5f, 3f, 3.75f, 4f, 5f)

/** Golden-ratio stride: successive stars never share a phase. */
private const val TWINKLE_PHASE_STRIDE = 0.6180339887f

/** alpha = CENTRE + SWING * sin(...), i.e. 0.4 .. 1.0. */
private const val TWINKLE_ALPHA_CENTRE = 0.7f
private const val TWINKLE_ALPHA_SWING = 0.3f

/** scale = 1 + SWING * sin(...), i.e. 0.85x .. 1.15x. */
private const val TWINKLE_SCALE_SWING = 0.15f

private fun DrawScope.drawTwinklingStars(
    clockSeconds: Float,
    field: List<Star>,
    color: Color,
    weight: Float,
    rootWidth: Float,
    rootHeight: Float,
    origin: Offset,
    exclusions: List<Rect>
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
        val alphaWave = sin(turns * TWO_PI)
        val scaleWave = sin((turns + QUARTER_TURN) * TWO_PI)

        val alpha = star.alpha * (TWINKLE_ALPHA_CENTRE + TWINKLE_ALPHA_SWING * alphaWave) * weight
        if (alpha <= 0.004f) continue
        val radius = unit * STAR_RADIUS_FACTOR * star.radius * (1f + TWINKLE_SCALE_SWING * scaleWave)
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
    }
}

private const val TWO_PI = 6.2831855f

/** A quarter period of lead, so size and brightness do not breathe in lockstep. */
private const val QUARTER_TURN = 0.25f
