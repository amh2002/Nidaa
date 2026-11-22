package com.mawaqiti.plus

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.ui.theme.StarTextClearance
import com.mawaqiti.plus.ui.theme.isStarBlocked
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * beta24 (Task 3): the deterministic contract of the star/text exclusion zones.
 *
 * The star field itself is a `DrawScope` extension (no device in a JVM test), so
 * what is proven here is the decision function it calls for every star: a centre
 * inside an inflated text box is rejected, a centre outside it is kept.
 */
class StarExclusionsTest {

    @Test
    fun clearanceIsTwentyFourDp() {
        assertEquals(24.dp, StarTextClearance)
    }

    @Test
    fun starCentreInsideTheInflatedTextBoxIsRejected() {
        // A 100 x 20 box at (50, 100) inflated by 24 dp of margin: 24 px at
        // density 1, so the excluded area is (26..174) x (76..144).
        val text = Rect(left = 50f, top = 100f, right = 150f, bottom = 120f)
        val margin = 24f
        val inflated = Rect(
            text.left - margin,
            text.top - margin,
            text.right + margin,
            text.bottom + margin
        )
        val exclusions = listOf(inflated)

        // On the glyphs, and 23 px away from them: blocked.
        assertTrue(isStarBlocked(Offset(100f, 110f), exclusions))
        assertTrue(isStarBlocked(Offset(100f, 100f - 23f), exclusions))

        // Exactly on the boundary is still inside (Rect.contains is inclusive).
        assertTrue(isStarBlocked(Offset(100f, 76f), exclusions))

        // One pixel past the margin, and far away in the open sky: kept.
        assertFalse(isStarBlocked(Offset(100f, 75f), exclusions))
        assertFalse(isStarBlocked(Offset(20f, 110f), exclusions))
        assertFalse(isStarBlocked(Offset(100f, 600f), exclusions))
    }

    @Test
    fun noExclusionsKeepsEveryStar() {
        assertFalse(isStarBlocked(Offset(0f, 0f), emptyList()))
    }
}
