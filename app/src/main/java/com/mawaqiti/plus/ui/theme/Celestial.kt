package com.mawaqiti.plus.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * beta23: the celestial indicator of the next-prayer card.
 *
 * The card used to paint one fixed crescent whatever the hour, so at 10:00 the
 * app showed a moon. The five states below follow the *already computed* prayer
 * times - they never re-derive a sun position:
 *
 *   MOON           Isha     -> Fajr      crescent + stars
 *   RISING_SUN     Fajr     -> Sunrise   half sun on the horizon, rays up
 *   FULL_SUN       Sunrise  -> Asr       full disc, rays
 *   LOW_SUN        Asr      -> Maghrib   disc sitting low, short rays
 *   SETTING_SUN    Maghrib  -> Isha      half disc sinking behind the horizon
 *
 * The moon can therefore never appear in a daytime window, and because the
 * resolver reads the same [PhaseBounds] the countdown uses, the switch happens
 * exactly on the computed minute.
 */
enum class CelestialPhase {
    MOON,
    RISING_SUN,
    FULL_SUN,
    LOW_SUN,
    SETTING_SUN;

    /** True for every state that must not show a moon. */
    val isDaylight: Boolean get() = this != MOON
}

/**
 * Pure resolver: minutes since local midnight plus the day's engine times.
 *
 * The windows are the same half-open ones [resolvePhase] uses, so the card icon
 * and the sky palette can never disagree about which part of the day it is.
 */
fun celestialPhase(minuteOfDay: Int, bounds: PhaseBounds): CelestialPhase {
    val minute = wrapMinute(minuteOfDay)
    return when {
        inPhaseWindow(minute, bounds.isha, bounds.fajr) -> CelestialPhase.MOON
        inPhaseWindow(minute, bounds.fajr, bounds.sunrise) -> CelestialPhase.RISING_SUN
        inPhaseWindow(minute, bounds.sunrise, bounds.asr) -> CelestialPhase.FULL_SUN
        inPhaseWindow(minute, bounds.asr, bounds.maghrib) -> CelestialPhase.LOW_SUN
        else -> CelestialPhase.SETTING_SUN
    }
}

/** The phase that matches a resolved [GoldenPhase]; one mapping, no maths. */
fun celestialOf(phase: GoldenPhase): CelestialPhase = when (phase) {
    GoldenPhase.NIGHT -> CelestialPhase.MOON
    GoldenPhase.DAWN -> CelestialPhase.RISING_SUN
    GoldenPhase.MORNING -> CelestialPhase.FULL_SUN
    GoldenPhase.AFTERNOON -> CelestialPhase.LOW_SUN
    GoldenPhase.SUNSET -> CelestialPhase.SETTING_SUN
}

/**
 * Draws the indicator for [phase] in the current phase's tokens.
 *
 * The canvas is square in both call sites (the phone hero's decor slot and the
 * TV hero row), and every coordinate is a fraction of `minDimension`, so the
 * same drawing scales from a 60 dp TV icon to the 110 px hero decoration.
 */
@Composable
fun CelestialIcon(
    phase: CelestialPhase,
    palette: SkyPalette,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val ink = palette.accent
        when (phase) {
            CelestialPhase.MOON -> drawCrescentAndStars(center, unit, ink)
            CelestialPhase.RISING_SUN -> drawRisingSun(unit, ink)
            CelestialPhase.FULL_SUN -> drawFullSun(center, unit, ink)
            CelestialPhase.LOW_SUN -> drawLowSun(unit, ink)
            CelestialPhase.SETTING_SUN -> drawSettingSun(unit, ink)
        }
    }
}

private fun DrawScope.drawCrescentAndStars(center: Offset, unit: Float, ink: Color) {
    val radius = unit * 0.20f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ink.copy(alpha = 0.20f), ink.copy(alpha = 0f)),
            center = center,
            radius = radius * 2.6f
        ),
        radius = radius * 2.6f,
        center = center
    )
    drawPath(path = crescentShape(center, radius), color = ink.copy(alpha = 0.95f))
    drawPath(
        path = starShape(center + Offset(radius * 1.25f, -radius * 0.95f), radius * 0.36f),
        color = ink.copy(alpha = 0.85f)
    )
    drawCircle(
        color = ink.copy(alpha = 0.55f),
        radius = unit * 0.015f,
        center = center + Offset(-radius * 1.5f, -radius * 1.1f)
    )
    drawCircle(
        color = ink.copy(alpha = 0.45f),
        radius = unit * 0.011f,
        center = center + Offset(-radius * 0.7f, radius * 1.7f)
    )
}

private fun DrawScope.drawRisingSun(unit: Float, ink: Color) {
    val horizonY = size.height * 0.70f
    val r = unit * 0.21f
    val cx = size.width / 2f
    drawArc(
        color = ink.copy(alpha = 0.92f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r, horizonY - r),
        size = Size(r * 2f, r * 2f)
    )
    drawHorizon(horizonY, unit, ink)
    repeat(5) { index ->
        val angle = (-150.0 + index * 30.0) * PI / 180.0
        val inner = r * 1.22f
        val outer = r * 1.62f
        drawLine(
            color = ink.copy(alpha = 0.80f),
            start = Offset(cx + (cos(angle) * inner).toFloat(), horizonY + (sin(angle) * inner).toFloat()),
            end = Offset(cx + (cos(angle) * outer).toFloat(), horizonY + (sin(angle) * outer).toFloat()),
            strokeWidth = unit * 0.022f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawFullSun(center: Offset, unit: Float, ink: Color) {
    val r = unit * 0.20f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ink.copy(alpha = 0.26f), ink.copy(alpha = 0f)),
            center = center,
            radius = r * 2.2f
        ),
        radius = r * 2.2f,
        center = center
    )
    drawCircle(color = ink.copy(alpha = 0.95f), radius = r, center = center)
    repeat(8) { index ->
        val angle = (index * 45.0) * PI / 180.0
        val inner = r * 1.28f
        val outer = r * 1.72f
        drawLine(
            color = ink.copy(alpha = 0.78f),
            start = Offset(center.x + (cos(angle) * inner).toFloat(), center.y + (sin(angle) * inner).toFloat()),
            end = Offset(center.x + (cos(angle) * outer).toFloat(), center.y + (sin(angle) * outer).toFloat()),
            strokeWidth = unit * 0.022f,
            cap = StrokeCap.Round
        )
    }
}

/** Afternoon: the same disc, lower in the frame and with only four short rays. */
private fun DrawScope.drawLowSun(unit: Float, ink: Color) {
    val center = Offset(size.width / 2f, size.height * 0.60f)
    val r = unit * 0.21f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ink.copy(alpha = 0.26f), ink.copy(alpha = 0f)),
            center = center,
            radius = r * 2.3f
        ),
        radius = r * 2.3f,
        center = center
    )
    drawCircle(color = ink.copy(alpha = 0.95f), radius = r, center = center)
    repeat(4) { index ->
        val angle = (20.0 + index * 140.0 / 3.0 + 20.0) * PI / 180.0
        val inner = r * 1.30f
        val outer = r * 1.58f
        drawLine(
            color = ink.copy(alpha = 0.72f),
            start = Offset(center.x + (cos(angle) * inner).toFloat(), center.y + (sin(angle) * inner).toFloat()),
            end = Offset(center.x + (cos(angle) * outer).toFloat(), center.y + (sin(angle) * outer).toFloat()),
            strokeWidth = unit * 0.020f,
            cap = StrokeCap.Round
        )
    }
}

/** Maghrib: the disc is already sinking - only its top part stays above the line. */
private fun DrawScope.drawSettingSun(unit: Float, ink: Color) {
    val horizonY = size.height * 0.68f
    val r = unit * 0.22f
    val cx = size.width / 2f
    drawArc(
        color = ink.copy(alpha = 0.92f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r, horizonY - r * 0.52f),
        size = Size(r * 2f, r * 2f)
    )
    drawHorizon(horizonY, unit, ink)
    drawLine(
        color = ink.copy(alpha = 0.35f),
        start = Offset(cx - r * 1.5f, horizonY + unit * 0.055f),
        end = Offset(cx + r * 1.5f, horizonY + unit * 0.055f),
        strokeWidth = unit * 0.016f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawHorizon(horizonY: Float, unit: Float, ink: Color) {
    drawLine(
        color = ink.copy(alpha = 0.60f),
        start = Offset(size.width * 0.08f, horizonY),
        end = Offset(size.width * 0.92f, horizonY),
        strokeWidth = unit * 0.020f,
        cap = StrokeCap.Round
    )
}

internal fun crescentShape(center: Offset, radius: Float): Path {
    val outer = Path().apply {
        addOval(Rect(center - Offset(radius, radius), Size(radius * 2f, radius * 2f)))
    }
    val biteCenter = center + Offset(radius * 0.46f, -radius * 0.20f)
    val biteRadius = radius * 0.86f
    val bite = Path().apply {
        addOval(Rect(biteCenter - Offset(biteRadius, biteRadius), Size(biteRadius * 2f, biteRadius * 2f)))
    }
    return Path().apply { op(outer, bite, PathOperation.Difference) }
}

internal fun starShape(center: Offset, outer: Float): Path {
    val path = Path()
    val inner = outer * 0.34f
    for (index in 0 until 8) {
        val angle = (-90.0 + index * 45.0) * PI / 180.0
        val radius = if (index % 2 == 0) outer else inner
        val point = Offset(
            center.x + (cos(angle) * radius).toFloat(),
            center.y + (sin(angle) * radius).toFloat()
        )
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}
