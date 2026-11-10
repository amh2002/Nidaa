package com.mawaqiti.plus.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Fixed tokens for the city bottom sheet.
 *
 * The sheet is intentionally dark in every Living Sky phase: it is a modal
 * surface the user opens deliberately, and a dark sheet is easier on the eyes
 * at dawn/night while still matching the app's night-sky identity. It does not
 * read [SkyPalette] on purpose, so the Day and Golden phases can never turn the
 * picker light again.
 *
 * Active segment choice: `white 16%` fill + bold gold text instead of a solid
 * `#1E4D3B` fill. Both pass WCAG, but the translucent token keeps the specified
 * track/active relationship and measures a higher contrast ratio (see
 * `CitySheetPaletteTest`); it also avoids introducing a second green into a
 * palette that is otherwise night-blue + gold.
 */
object CitySheetPalette {
    val GradientTop = Color(0xFF0B102A)
    val GradientBottom = Color(0xFF141B3F)
    val Fallback = Color(0xFF0E1540)

    val Text = Color(0xFFEEF2FF)

    /** Placeholder + magnifier only, as specified (rgba(255,255,255,.45)). */
    val Hint = Color.White.copy(alpha = 0.45f)

    /** Secondary body text; keeps WCAG AA on the dark gradient. */
    val Muted = Color(0xFFEEF2FF).copy(alpha = 0.70f)
    val Divider = Color.White.copy(alpha = 0.08f)
    val Grabber = Color.White.copy(alpha = 0.25f)

    val FieldFill = Color.White.copy(alpha = 0.06f)
    val FieldBorder = Color.White.copy(alpha = 0.22f)

    val Track = Color.White.copy(alpha = 0.08f)
    val ActiveFill = Color.White.copy(alpha = 0.16f)
    val Accent = Color(0xFFFFD98A)

    val DefaultTag = Color.White.copy(alpha = 0.5f)
    val Scrim = Color.Black.copy(alpha = 0.55f)

    val TopRadius = 26.dp
}
