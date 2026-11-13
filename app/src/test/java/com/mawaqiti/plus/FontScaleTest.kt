package com.mawaqiti.plus

import com.mawaqiti.plus.data.FontScale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * beta 7.9 gate for the in-app font multiplier: five fixed steps, a 1.0 default,
 * locale-neutral percentage labels and a normalizer that cannot leave a corrupt
 * stored value in the UI.
 */
class FontScaleTest {

    @Test
    fun `five steps from 80 to 120 percent with a 1_0 default`() {
        assertEquals(listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f), FontScale.STEPS)
        assertEquals(1.0f, FontScale.DEFAULT)
        assertEquals(5, FontScale.STEPS.size)
    }

    @Test
    fun `labels are plain percentages in both locales`() {
        assertEquals("80%", FontScale.label(0.8f))
        assertEquals("90%", FontScale.label(0.9f))
        assertEquals("100%", FontScale.label(1.0f))
        assertEquals("110%", FontScale.label(1.1f))
        assertEquals("120%", FontScale.label(1.2f))
    }

    @Test
    fun `normalize snaps to the nearest allowed step`() {
        assertEquals(0.8f, FontScale.normalize(0.0f))
        assertEquals(0.8f, FontScale.normalize(0.81f))
        assertEquals(0.9f, FontScale.normalize(0.87f))
        assertEquals(1.0f, FontScale.normalize(1.03f))
        assertEquals(1.2f, FontScale.normalize(3.7f))
        assertEquals(1.0f, FontScale.normalize(Float.NaN))
    }

    @Test
    fun `index round-trips through the pill rows`() {
        FontScale.STEPS.forEachIndexed { index, step ->
            assertEquals(index, FontScale.indexOf(step))
        }
    }
}
