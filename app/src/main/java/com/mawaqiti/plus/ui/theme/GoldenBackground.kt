package com.mawaqiti.plus.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Beta 9 "Golden Night" background.
 *
 * Same vertical gradient contract as [LivingSkyBackground] (three palette
 * stops, optional horizon tint, optional bottom glow) plus the ornaments taken
 * from the owner's reference: a deterministic star field, soft clouds, thin
 * botanical corner branches and a mosque silhouette along the bottom edge.
 *
 * Stars/moon are not drawn behind text: the star field stops at 55% of the
 * height, the skyline is a near-black silhouette (darkening the bottom, which
 * only helps the cream text) and the botanicals stay in the bottom corners,
 * clear of the wordmark, chip and cards.
 */
@Composable
fun GoldenSkyBackground(modifier: Modifier = Modifier) {
    val palette = skyPalette
    val phase = LocalGoldenPhase.current
    val starAlpha by animateFloatAsState(
        targetValue = starAlphaFor(phase),
        animationSpec = tween(PHASE_FADE_MS),
        label = "goldenStars"
    )
    val clouds by animateFloatAsState(
        targetValue = cloudAlphaFor(phase),
        animationSpec = tween(PHASE_FADE_MS),
        label = "goldenClouds"
    )
    val stars = remember { starField(count = 46, seed = 20260915L) }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(palette.gradient))

        palette.horizonTint?.let { tint ->
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, tint),
                    startY = size.height * 0.58f,
                    endY = size.height
                )
            )
        }

        palette.glow?.let { glow ->
            val center = Offset(size.width * 0.5f, size.height * 1.04f)
            val radius = size.height * 0.50f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glow, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        if (starAlpha > 0.01f) {
            drawStarField(stars, alpha = starAlpha, color = palette.text, maxY = 0.55f)
        }
        if (clouds > 0.01f) {
            drawClouds(palette.text, clouds)
        }

        val botanical = palette.accent.copy(alpha = if (palette.dark) 0.16f else 0.20f)
        drawBotanical(
            origin = Offset(size.width - size.minDimension * 0.03f, size.height * 0.985f),
            scale = size.minDimension * 0.30f,
            direction = -1f,
            color = botanical
        )
        drawBotanical(
            origin = Offset(size.minDimension * 0.03f, size.height * 0.985f),
            scale = size.minDimension * 0.24f,
            direction = 1f,
            color = botanical
        )

        // Silhouette hugging the bottom edge: darker than the sky, so the note
        // and footer buttons keep their contrast.
        drawSkyline(
            color = Color(0xFF010208).copy(alpha = 0.55f),
            baseY = size.height * 0.955f
        )
    }
}

/**
 * The hero-card illustration: crescent, a few stars, clouds and a small mosque
 * skyline. Painted behind the hero content; the crescent lives in the top-right
 * corner and the skyline in the bottom 12%, so the centred text never sits on
 * top of it.
 */
@Composable
fun GoldenHeroDecor(modifier: Modifier = Modifier) {
    val palette = skyPalette
    val phase = LocalGoldenPhase.current
    val moon by animateFloatAsState(
        targetValue = moonAlphaFor(phase),
        animationSpec = tween(PHASE_FADE_MS),
        label = "goldenMoon"
    )
    val stars by animateFloatAsState(
        targetValue = starAlphaFor(phase),
        animationSpec = tween(PHASE_FADE_MS),
        label = "heroStars"
    )
    val skyline by animateFloatAsState(
        targetValue = skylineAlphaFor(phase),
        animationSpec = tween(PHASE_FADE_MS),
        label = "heroSkyline"
    )
    val field = remember { starField(count = 9, seed = 4242L) }

    Canvas(modifier = modifier) {
        if (stars > 0.01f) {
            // Keep stars off the centred label/name/time column.
            drawStarField(field, alpha = stars * 0.85f, color = palette.text, maxY = 0.34f,
                minX = 0.62f)
        }
        if (moon > 0.01f) {
            val radius = size.minDimension * 0.11f
            val center = Offset(size.width * 0.845f, size.height * 0.20f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.accent.copy(alpha = 0.22f * moon), Color.Transparent),
                    center = center,
                    radius = radius * 2.4f
                ),
                radius = radius * 2.4f,
                center = center
            )
            drawPath(
                path = crescentPath(center, radius),
                color = palette.accent.copy(alpha = 0.95f * moon)
            )
            drawPath(
                path = sparklePath(
                    center = center + Offset(radius * 1.15f, radius * 0.95f),
                    outer = radius * 0.34f,
                    inner = radius * 0.11f
                ),
                color = Color(0xFFFFF6E0).copy(alpha = 0.90f * moon)
            )
        }
        if (skyline > 0.01f) {
            drawSkyline(
                color = Color(0xFF010208).copy(alpha = 0.45f * skyline),
                baseY = size.height * 0.90f,
                heightFraction = 0.55f
            )
        }
    }
}

/** Cross-fade length for decorations; the palette itself uses 500 ms. */
internal const val PHASE_FADE_MS = 500

internal fun starAlphaFor(phase: GoldenPhase): Float = when (phase) {
    GoldenPhase.NIGHT -> 1f
    GoldenPhase.DAWN -> 0.45f
    GoldenPhase.MORNING -> 0.22f
    GoldenPhase.AFTERNOON -> 0.10f
    GoldenPhase.SUNSET -> 0.10f
}

internal fun moonAlphaFor(phase: GoldenPhase): Float = when (phase) {
    GoldenPhase.NIGHT -> 1f
    GoldenPhase.DAWN -> 0.55f
    GoldenPhase.MORNING -> 0.15f
    GoldenPhase.AFTERNOON -> 0.08f
    GoldenPhase.SUNSET -> 0.18f
}

internal fun cloudAlphaFor(phase: GoldenPhase): Float = when (phase) {
    GoldenPhase.NIGHT -> 0.55f
    GoldenPhase.DAWN -> 0.85f
    GoldenPhase.MORNING -> 0.70f
    GoldenPhase.AFTERNOON -> 0.60f
    GoldenPhase.SUNSET -> 0.80f
}

internal fun skylineAlphaFor(phase: GoldenPhase): Float = when (phase) {
    GoldenPhase.NIGHT -> 1f
    GoldenPhase.DAWN -> 0.85f
    GoldenPhase.MORNING -> 0.70f
    GoldenPhase.AFTERNOON -> 0.75f
    GoldenPhase.SUNSET -> 0.90f
}

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/** Deterministic star field: same sky on every launch, no per-frame work. */
private fun starField(count: Int, seed: Long): List<Star> {
    val random = Random(seed)
    return List(count) {
        Star(
            x = random.nextFloat(),
            y = random.nextFloat(),
            radius = 0.35f + random.nextFloat() * 0.9f,
            alpha = 0.25f + random.nextFloat() * 0.55f
        )
    }
}

private fun DrawScope.drawStarField(
    stars: List<Star>,
    alpha: Float,
    color: Color,
    maxY: Float,
    minX: Float = 0f
) {
    val unit = size.minDimension
    for (star in stars) {
        val x = minX + star.x * (1f - minX)
        val center = Offset(size.width * x, size.height * (star.y * maxY + 0.015f))
        val r = unit * 0.0035f * star.radius
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = star.alpha * alpha),
                    Color.Transparent
                ),
                center = center,
                radius = r * 2.6f
            ),
            radius = r * 2.6f,
            center = center
        )
        drawCircle(color = color.copy(alpha = star.alpha * alpha), radius = r, center = center)
    }
}

private fun DrawScope.drawClouds(color: Color, alpha: Float) {
    val bands = listOf(
        Triple(0.06f, 0.46f, 0.30f),
        Triple(0.94f, 0.52f, 0.26f),
        Triple(0.20f, 0.62f, 0.22f),
        Triple(0.80f, 0.66f, 0.20f)
    )
    for ((x, y, width) in bands) {
        val w = size.width * width
        val h = w * 0.34f
        val center = Offset(size.width * x, size.height * y)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.07f * alpha),
                    Color.Transparent
                ),
                center = center,
                radius = w * 0.5f
            ),
            topLeft = Offset(center.x - w * 0.5f, center.y - h * 0.5f),
            size = Size(w, h)
        )
    }
}

/**
 * Thin, stylised branch with round leaves, used in both bottom corners.
 * [direction] is -1 for the right corner (growing leftward) and +1 for the left.
 */
private fun DrawScope.drawBotanical(
    origin: Offset,
    scale: Float,
    direction: Float,
    color: Color
) {
    val stem = Path().apply {
        moveTo(origin.x, origin.y)
        quadraticTo(
            origin.x + direction * scale * 0.42f,
            origin.y - scale * 0.42f,
            origin.x + direction * scale * 0.16f,
            origin.y - scale
        )
    }
    drawPath(
        path = stem,
        color = color,
        style = Stroke(width = scale * 0.014f, cap = StrokeCap.Round)
    )

    val leaves = listOf(
        Triple(0.16f, 0.16f, 0.115f),
        Triple(0.34f, 0.40f, 0.100f),
        Triple(0.17f, 0.52f, 0.090f),
        Triple(0.30f, 0.72f, 0.078f)
    )
    for ((dx, dy, leaf) in leaves) {
        val center = Offset(
            origin.x + direction * scale * dx,
            origin.y - scale * dy
        )
        val w = scale * leaf * 2.1f
        val h = scale * leaf
        drawOval(
            color = color,
            topLeft = Offset(center.x - w * 0.5f, center.y - h * 0.5f),
            size = Size(w, h)
        )
    }
}

/**
 * Mosque silhouette: two domes and two minarets over a solid base. All values
 * are normalised to the canvas, so the same function works for the full-screen
 * background and for the small hero-card strip.
 */
private fun DrawScope.drawSkyline(
    color: Color,
    baseY: Float,
    heightFraction: Float = 1f
) {
    val w = size.width
    val h = size.height
    val unit = h * heightFraction
    val minaret = w * 0.030f

    val path = Path().apply {
        moveTo(0f, h)
        lineTo(0f, baseY)

        // Left minaret
        lineTo(w * 0.085f, baseY)
        lineTo(w * 0.085f, baseY - unit * 0.075f)
        lineTo(w * 0.085f + minaret, baseY - unit * 0.075f)
        lineTo(w * 0.085f + minaret, baseY)

        // Small dome
        lineTo(w * 0.255f, baseY)
        quadraticTo(w * 0.325f, baseY - unit * 0.085f, w * 0.395f, baseY)

        // Large dome with a slim finial
        lineTo(w * 0.470f, baseY)
        lineTo(w * 0.505f, baseY - unit * 0.020f)
        quadraticTo(w * 0.585f, baseY - unit * 0.140f, w * 0.665f, baseY - unit * 0.020f)
        lineTo(w * 0.700f, baseY)

        // Right minaret
        lineTo(w * 0.855f, baseY)
        lineTo(w * 0.855f, baseY - unit * 0.095f)
        lineTo(w * 0.855f + minaret, baseY - unit * 0.095f)
        lineTo(w * 0.855f + minaret, baseY)

        lineTo(w, baseY)
        lineTo(w, h)
        close()
    }
    drawPath(path, color)
}

private fun crescentPath(center: Offset, radius: Float): Path {
    val outer = Path().apply {
        addOval(
            Rect(
                offset = center - Offset(radius, radius),
                size = Size(radius * 2f, radius * 2f)
            )
        )
    }
    val biteCenter = center + Offset(radius * 0.46f, -radius * 0.20f)
    val biteRadius = radius * 0.86f
    val bite = Path().apply {
        addOval(
            Rect(
                offset = biteCenter - Offset(biteRadius, biteRadius),
                size = Size(biteRadius * 2f, biteRadius * 2f)
            )
        )
    }
    return Path().apply { op(outer, bite, PathOperation.Difference) }
}

private fun sparklePath(center: Offset, outer: Float, inner: Float): Path {
    val path = Path()
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
