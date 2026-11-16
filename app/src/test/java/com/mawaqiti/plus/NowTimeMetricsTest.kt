package com.mawaqiti.plus

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.ui.screens.NowTimeMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * beta12: the current time moved from the card header to its own bottom row,
 * but it must keep the beta11 visibility the owner asked for. These values are
 * the contract: at least 18 sp, clearly secondary to the 44 sp prayer time.
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
    fun `clock icon scales with the text`() {
        assertEquals(20.dp, NowTimeMetrics.ICON_SIZE)
    }

    @Test
    fun `now row stays secondary to the prayer time`() {
        assertTrue(NowTimeMetrics.FONT_SIZE.value < 44f)
        assertTrue(NowTimeMetrics.LINE_HEIGHT.value >= NowTimeMetrics.FONT_SIZE.value)
    }
}
