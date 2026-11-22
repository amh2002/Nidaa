package com.mawaqiti.plus.ui.theme

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * beta24 (Task 3): the clearance a star must keep from any text box.
 *
 * In the night state a small star drawn next to a word reads as a dot or a
 * diacritic of that word - the owner saw it around «نداء» and around the prayer
 * rows. 24 dp is one line of body text: far enough that no star can be mistaken
 * for a mark of the glyphs, close enough that the open sky keeps its density.
 */
val StarTextClearance: Dp = 24.dp

/**
 * Registry of the boxes the star field must avoid.
 *
 * Content registers its own bounds (through [Modifier.starTextExclusion]),
 * already inflated by [StarTextClearance] and expressed in **root**
 * coordinates. [rects] is snapshot state, so a background canvas that reads it
 * simply redraws when a box appears, moves or disappears - no recomposition of
 * the screens is involved.
 *
 * Rejection, not repositioning: a blocked star is dropped. Moving it would just
 * push it next to another word, and dropping keeps the field deterministic
 * (same seed, same sky) for the screenshot evidence.
 */
class StarExclusions {

    private val registered = LinkedHashMap<Any, Rect>()

    /** Inflated text boxes in root coordinates; empty when nothing is laid out. */
    var rects by mutableStateOf(emptyList<Rect>())
        private set

    fun put(key: Any, rect: Rect) {
        if (registered[key] == rect) return
        registered[key] = rect
        publish()
    }

    fun remove(key: Any) {
        if (registered.remove(key) != null) publish()
    }

    private fun publish() {
        rects = registered.values.toList()
    }
}

/**
 * The registry for the current window. Hoisted once at the app root so the one
 * background canvas and every screen share it.
 */
val LocalStarExclusions = staticCompositionLocalOf { StarExclusions() }

/**
 * Registers this element's bounds as a no-star zone for as long as it is
 * composed. Apply it to text nodes (or to the card that holds them) - never to
 * containers that cover the whole screen, which would empty the sky.
 */
fun Modifier.starTextExclusion(key: Any, margin: Dp = StarTextClearance): Modifier = composed {
    val registry = LocalStarExclusions.current
    val marginPx = with(LocalDensity.current) { margin.toPx() }
    DisposableEffect(key) {
        onDispose { registry.remove(key) }
    }
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        registry.put(
            key,
            Rect(
                left = bounds.left - marginPx,
                top = bounds.top - marginPx,
                right = bounds.right + marginPx,
                bottom = bounds.bottom + marginPx
            )
        )
    }
}

/** True when [center] (root coordinates) falls inside any excluded box. */
internal fun isStarBlocked(center: Offset, exclusions: List<Rect>): Boolean =
    exclusions.any { it.contains(center) }
