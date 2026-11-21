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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import java.util.Random

/**
 * Beta 9 "Golden Night" background.
 *
 * Same vertical gradient contract as [LivingSkyBackground] (three palette
 * stops, optional horizon tint, optional bottom glow) plus the ornaments taken
 * from the owner's reference: a deterministic star field, soft clouds and - as
 * of beta23 - a symmetric Islamic night scene along the bottom edge.
 *
 * Stars are not drawn behind text: the star field stops at 55% of the height,
 * and the scene is a near-black silhouette that only darkens the very bottom,
 * which raises the contrast of the cream text above it.
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
                    colors = listOf(tint.copy(alpha = 0f), tint),
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
                    colors = listOf(glow, glow.copy(alpha = 0f)),
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

        // beta23: the abstract oval "botanicals" that used to sit in the two
        // bottom corners are gone. In their place is one meaningful, mirror-
        // symmetric scene: a mosque with a dome and two minarets, flanking
        // palms and a low arcade, in the same near-black silhouette value so
        // the cards, the note and the dock keep their contrast.
        drawIslamicScene(
            color = Color(0xFF010208).copy(alpha = 0.55f),
            // The floating dock covers roughly the bottom fifth of the phone
            // screen, so the skyline is based a little higher than the edge:
            // the dome, both minarets and the palms then read ABOVE the dock
            // instead of hiding behind its pill, while the ground slab still
            // runs to the bottom edge.
            baseY = size.height * 0.955f
        )
    }
}

/**
 * The hero-card illustration: a few stars and a small arcade silhouette.
 *
 * beta23 removed the fixed crescent that used to live here. The card now owns a
 * real [CelestialIcon] that follows the computed prayer times, so this decor
 * must never draw a moon of its own.
 */
@Composable
fun GoldenHeroDecor(modifier: Modifier = Modifier) {
    val palette = skyPalette
    val phase = LocalGoldenPhase.current
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
        if (skyline > 0.01f) {
            drawIslamicScene(
                color = Color(0xFF010208).copy(alpha = 0.45f * skyline),
                baseY = size.height * 0.955f,
                compact = true
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
                    color.copy(alpha = 0f)
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
                    color.copy(alpha = 0f)
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
 * beta23: the Islamic night scene that replaces the old abstract shapes.
 *
 * One closed path carries the architecture (base slab, two minarets with their
 * balconies and spires, the drummed central dome with its finial, two flanking
 * half domes and the low arcade wings), then two palms are stroked on top of it.
 * Every x is expressed from the horizontal centre and mirrored, so the scene is
 * exactly symmetric and therefore identical in RTL and LTR layouts.
 *
 * Contrast contract: the caller passes a near-black colour with a low alpha, so
 * the scene can only *darken* the bottom of the screen - the light cream text,
 * the gold accent and every glass card keep or gain contrast over it.
 *
 * [compact] shrinks the vertical proportions for the small arcade strip inside
 * the hero card.
 */
private fun DrawScope.drawIslamicScene(
    color: Color,
    baseY: Float,
    compact: Boolean = false
) {
    val w = size.width
    val h = size.height
    // Every horizontal value is a fraction of the width and every vertical one a
    // fraction of a single "rise" band, so the same numbers mirror exactly and
    // scale from a phone to the small arcade strip in the hero card.
    val rise = h * if (compact) 0.16f else 0.19f
    fun x(f: Float) = w * f
    fun yv(f: Float) = baseY - rise * f

    // Ground slab: from the baseline to the bottom edge of the screen.
    drawRect(color = color, topLeft = Offset(0f, baseY), size = Size(w, h - baseY))

    // ---- arcade wings, mirrored -------------------------------------------
    for (side in listOf(1f, -1f)) {
        fun sx(f: Float) = x(0.5f + side * f)
        val bandTop = yv(0.26f)
        val left = minOf(sx(0.122f), sx(0.238f))
        val right = maxOf(sx(0.122f), sx(0.238f))
        drawRect(
            color = color,
            topLeft = Offset(left, bandTop),
            size = Size(right - left, baseY - bandTop)
        )
        // Three small domes over the arcade openings.
        for (index in 0 until 3) {
            val cx = sx(0.142f + index * 0.038f)
            drawCircle(color = color, radius = rise * 0.075f, center = Offset(cx, bandTop))
        }
    }

    // ---- side halls and their half domes ---------------------------------
    for (side in listOf(1f, -1f)) {
        fun sx(f: Float) = x(0.5f + side * f)
        val hallLeft = minOf(sx(0.238f), sx(0.395f))
        val hallRight = maxOf(sx(0.238f), sx(0.395f))
        drawRect(
            color = color,
            topLeft = Offset(hallLeft, yv(0.30f)),
            size = Size(hallRight - hallLeft, baseY - yv(0.30f))
        )
        val dome = Path().apply {
            moveTo(hallLeft, yv(0.30f))
            quadraticTo(
                (hallLeft + hallRight) / 2f, yv(0.58f),
                hallRight, yv(0.30f)
            )
            lineTo(hallRight, yv(0.30f))
            close()
        }
        drawPath(dome, color)
    }

    // ---- main hall --------------------------------------------------------
    val hallLeft = x(0.395f)
    val hallRight = x(0.605f)
    drawRect(
        color = color,
        topLeft = Offset(hallLeft, yv(0.30f)),
        size = Size(hallRight - hallLeft, baseY - yv(0.30f))
    )
    // Drum under the dome.
    drawRect(
        color = color,
        topLeft = Offset(x(0.410f), yv(0.52f)),
        size = Size(x(0.590f) - x(0.410f), yv(0.30f) - yv(0.52f))
    )
    // Central dome: a bulb that closes on the drum.
    val dome = Path().apply {
        moveTo(x(0.410f), yv(0.52f))
        cubicTo(
            x(0.418f), yv(0.90f),
            x(0.582f), yv(0.90f),
            x(0.590f), yv(0.52f)
        )
        close()
    }
    drawPath(dome, color)
    // Finial: a slim post and its bead.
    drawRect(
        color = color,
        topLeft = Offset(x(0.497f), yv(1.05f)),
        size = Size(x(0.503f) - x(0.497f), yv(0.86f) - yv(1.05f))
    )
    drawCircle(color = color, radius = w * 0.007f, center = Offset(x(0.5f), yv(1.08f)))

    // ---- minarets, mirrored ----------------------------------------------
    for (side in listOf(1f, -1f)) {
        val cx = x(0.5f + side * 0.330f)
        // Shaft.
        drawRect(
            color = color,
            topLeft = Offset(cx - w * 0.011f, yv(0.72f)),
            size = Size(w * 0.022f, baseY - yv(0.72f))
        )
        // Balcony.
        drawRect(
            color = color,
            topLeft = Offset(cx - w * 0.022f, yv(0.72f)),
            size = Size(w * 0.044f, yv(0.66f) - yv(0.72f))
        )
        // Upper shaft.
        drawRect(
            color = color,
            topLeft = Offset(cx - w * 0.008f, yv(0.86f)),
            size = Size(w * 0.016f, yv(0.66f) - yv(0.86f))
        )
        // Cap and spire.
        val cap = Path().apply {
            moveTo(cx - w * 0.015f, yv(0.86f))
            lineTo(cx, yv(0.99f))
            lineTo(cx + w * 0.015f, yv(0.86f))
            close()
        }
        drawPath(cap, color)
        drawCircle(color = color, radius = w * 0.006f, center = Offset(cx, yv(1.02f)))
    }

    // ---- palms, mirrored --------------------------------------------------
    if (!compact) {
        val palmHeight = h * 0.05f
        drawPalm(
            footX = x(0.052f),
            baseY = baseY,
            height = palmHeight,
            lean = -w * 0.010f,
            thickness = w * 0.007f,
            color = color
        )
        drawPalm(
            footX = x(0.948f),
            baseY = baseY,
            height = palmHeight,
            lean = w * 0.010f,
            thickness = w * 0.007f,
            color = color
        )
    }
}

/** One palm: a leaning trunk and five drooping fronds. */
private fun DrawScope.drawPalm(
    footX: Float,
    baseY: Float,
    height: Float,
    lean: Float,
    thickness: Float,
    color: Color
) {
    val crown = Offset(footX + lean, baseY - height)
    val trunk = Path().apply {
        moveTo(footX, baseY)
        quadraticTo(footX + lean * 0.4f, baseY - height * 0.55f, crown.x, crown.y)
    }
    drawPath(
        path = trunk,
        color = color,
        style = Stroke(width = thickness, cap = StrokeCap.Round)
    )

    val spread = height * 0.62f
    val direction = if (lean < 0f) 1f else -1f
    for (index in 0 until 5) {
        val t = (index / 4f) * 2f - 1f          // -1 .. 1, mirrored
        val tip = Offset(
            crown.x + t * spread * direction,
            crown.y - height * (0.46f - kotlin.math.abs(t) * 0.30f)
        )
        val frond = Path().apply {
            moveTo(crown.x, crown.y)
            quadraticTo(
                (crown.x + tip.x) / 2f,
                crown.y - height * 0.12f,
                tip.x,
                tip.y
            )
        }
        drawPath(
            path = frond,
            color = color,
            style = Stroke(width = thickness * 0.72f, cap = StrokeCap.Round)
        )
    }
}
