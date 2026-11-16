package com.mawaqiti.plus.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * beta10: the two hand-made prayer glyphs the Material icon set does not ship.
 *
 * `material-icons-extended` has no plain sunrise/sunset pair (only
 * `WbTwilight`, which reads as a sunrise-with-rays for every horizon event), so
 * Fajr and Maghrib get their own 24x24 vectors:
 *
 *  - [Sunrise]  half sun **above** the horizon with five rays fanning upward.
 *  - [Sunset]   half sun **touching** the horizon, no rising rays.
 *
 * Both are single-colour vectors: the path fill/stroke is black and the caller
 * tints them with `Icon(tint = ...)`, which in beta10 is the golden accent for
 * the highlighted card and `palette.text` at 72 % for the others, exactly like
 * the beta9 icons they replace.
 */
object PrayerIcons {

    /** Fajr: rising sun, rays above the horizon. */
    val Sunrise: ImageVector by lazy {
        ImageVector.Builder(
            name = "PrayerSunrise",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Sun disc (half circle standing on the horizon).
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.6f, 18f)
                curveTo(6.6f, 12f, 17.4f, 12f, 17.4f, 18f)
                close()
            }
            // Horizon line.
            strokeLine(2.5f, 18.4f, 21.5f, 18.4f)
            // Five rising rays.
            strokeLine(12f, 10.4f, 12f, 7.2f)
            strokeLine(16.36f, 11.78f, 18.2f, 9.15f)
            strokeLine(7.64f, 11.78f, 5.8f, 9.15f)
            strokeLine(19.05f, 15.15f, 22f, 13.95f)
            strokeLine(4.95f, 15.15f, 2f, 13.95f)
        }.build()
    }

    /** Maghrib: sun resting on the horizon, no rays. */
    val Sunset: ImageVector by lazy {
        ImageVector.Builder(
            name = "PrayerSunset",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // A slightly wider disc so the two glyphs never read as clones.
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.2f, 18f)
                curveTo(6.2f, 11.2f, 17.8f, 11.2f, 17.8f, 18f)
                close()
            }
            // Horizon line only: the sun rests on it and nothing rises.
            strokeLine(2.5f, 18.4f, 21.5f, 18.4f)
        }.build()
    }

    private fun ImageVector.Builder.strokeLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
    }
}
