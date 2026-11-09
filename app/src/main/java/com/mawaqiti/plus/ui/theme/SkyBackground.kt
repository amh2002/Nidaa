package com.mawaqiti.plus.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-bleed Living Sky background. Gradients are palette-driven; the warm
 * amber wash and horizon tint belong to Dawn/Golden only.
 */
@Composable
fun LivingSkyBackground(modifier: Modifier = Modifier) {
    val palette = skyPalette
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(palette.gradient))

        palette.warmOverlay?.let { overlay ->
            drawRect(overlay)
        }

        palette.horizonTint?.let { tint ->
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, tint),
                    startY = size.height * 0.55f,
                    endY = size.height
                )
            )
        }

        palette.glow?.let { glow ->
            val center = Offset(size.width * 0.5f, size.height * 1.02f)
            val radius = size.height * 0.55f
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

        // Pale top haze for the two light phases. It keeps #0D2438 text at
        // WCAG AA over the Day gradient's darkest stop without touching the
        // authoritative gradient colours.
        palette.topScrim?.let { scrim ->
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(scrim, scrim.copy(alpha = 0f)),
                    startY = 0f,
                    endY = size.height * SkyPalette.SCRIM_FALLOFF
                )
            )
        }

        // A faint warm corner glow is part of the Living Sky identity in every
        // phase; it is kept very low so text contrast never depends on it.
        val corner = Offset(size.width * 0.92f, size.height * 0.02f)
        val cornerRadius = size.minDimension * 0.45f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFC77B4A).copy(alpha = if (palette.dark) 0.10f else 0.07f),
                    Color.Transparent
                ),
                center = corner,
                radius = cornerRadius
            ),
            radius = cornerRadius,
            center = corner
        )
    }
}

/**
 * Reserved decoration band. Crescent and stars are drawn only inside this
 * fixed-height band, so they can never overlap the wordmark, chips, hero card
 * or any text in any phase.
 */
@Composable
fun SkyDecorBand(modifier: Modifier = Modifier) {
    val palette = skyPalette
    val decorated = palette.phase == SkyPhase.NIGHT
    val height = if (decorated) 60.dp else 8.dp

    if (!decorated) {
        Spacer(modifier.fillMaxWidth().height(height))
        return
    }

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val unit = size.minDimension
        val crescentCenter = Offset(size.width * 0.18f, size.height * 0.52f)
        val crescentRadius = unit * 0.24f

        // Soft halo behind the crescent.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.accent.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = crescentCenter,
                radius = crescentRadius * 1.9f
            ),
            radius = crescentRadius * 1.9f,
            center = crescentCenter
        )

        drawCrescent(
            center = crescentCenter,
            radius = crescentRadius,
            color = palette.accent.copy(alpha = 0.92f)
        )
        drawPath(
            path = sparkle(
                center = crescentCenter + Offset(crescentRadius * 1.05f, crescentRadius * 0.62f),
                outer = crescentRadius * 0.34f,
                inner = crescentRadius * 0.11f
            ),
            color = Color(0xFFFFF6E0).copy(alpha = 0.92f)
        )

        // Star dots, low opacity, strictly inside the band.
        val stars = listOf(
            Triple(0.38f, 0.22f, 0.9f),
            Triple(0.47f, 0.68f, 0.7f),
            Triple(0.58f, 0.34f, 1.1f),
            Triple(0.70f, 0.62f, 0.8f),
            Triple(0.82f, 0.26f, 1.3f),
            Triple(0.92f, 0.55f, 0.7f)
        )
        for ((x, y, radius) in stars) {
            val center = Offset(size.width * x, size.height * (y.coerceIn(0.08f, 0.92f)))
            val r = unit * 0.018f * radius
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF6E0).copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = r * 2.4f
                ),
                radius = r * 2.4f,
                center = center
            )
        }
    }
}

private fun DrawScope.drawCrescent(center: Offset, radius: Float, color: Color) {
    val outer = Path().apply {
        addOval(
            Rect(
                offset = center - Offset(radius, radius),
                size = Size(radius * 2f, radius * 2f)
            )
        )
    }
    val biteCenter = center + Offset(radius * 0.44f, -radius * 0.18f)
    val biteRadius = radius * 0.88f
    val bite = Path().apply {
        addOval(
            Rect(
                offset = biteCenter - Offset(biteRadius, biteRadius),
                size = Size(biteRadius * 2f, biteRadius * 2f)
            )
        )
    }
    val crescent = Path().apply {
        op(outer, bite, PathOperation.Difference)
    }
    drawPath(crescent, color)
}

private fun sparkle(center: Offset, outer: Float, inner: Float): Path {
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
