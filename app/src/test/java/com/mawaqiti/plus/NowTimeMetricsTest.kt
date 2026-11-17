package com.mawaqiti.plus

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.ui.screens.NowTimeMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * beta12 kept the current time readable after moving it to its own bottom row;
 * beta13 fixes the clock icon's optical alignment without shrinking it.
 *
 * The lift is em-based, so these tests pin both the 1.0 and 1.3 values the owner
 * asked to verify (within a small tolerance) and keep the text at its beta12
 * size.
 */
class NowTimeMetricsTest {

    @Test
    fun `now row is 20 sp on a 26 sp line`() {
        assertEquals(20.sp, NowTimeMetrics.FONT_SIZE)
        assertEquals(26.sp, NowTimeMetrics.LINE_HEIGHT)
    }

    @Test
    fun `now row never drops below the 18 sp floor`() {
        assertTrue(NowTimeMetrics.FONT_SIZE.value >= 18f)
    }

    @Test
    fun `clock icon is a fixed 18 dp vector`() {
        assertEquals(18.dp, NowTimeMetrics.ICON_SIZE)
    }

    @Test
    fun `optical shift is 3_0 dp at font scale 1_0`() {
        val shift = NowTimeMetrics.iconShift(1f)
        assertEquals(3.0f, shift.value, 0.01f)
    }

    @Test
    fun `optical shift is 3_9 dp at font scale 1_3`() {
        val shift = NowTimeMetrics.iconShift(1.3f)
        assertEquals(3.9f, shift.value, 0.01f)
    }

    @Test
    fun `optical shift grows with the font scale`() {
        assertTrue(NowTimeMetrics.iconShift(1.3f) > NowTimeMetrics.iconShift(1f))
        assertTrue(NowTimeMetrics.iconShift(1f).value > 0f)
        assertTrue(NowTimeMetrics.iconShift(1.3f).value <= 4f)
    }

    @Test
    fun `now row stays secondary to the prayer time`() {
        assertTrue(NowTimeMetrics.FONT_SIZE.value < 44f)
        assertTrue(NowTimeMetrics.LINE_HEIGHT.value >= NowTimeMetrics.FONT_SIZE.value)
    }
}
