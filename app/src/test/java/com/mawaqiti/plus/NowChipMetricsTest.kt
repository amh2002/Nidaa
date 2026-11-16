package com.mawaqiti.plus

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.ui.screens.NowChipMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * beta11: the "now" line was promoted from 11 sp / 75 % gold to a readable
 * header. These values are the contract, and the last test keeps the line
 * secondary to the 44 sp prayer time it sits above.
 */
class NowChipMetricsTest {

    @Test
    fun `now line is 16 sp semibold on a 20 sp line`() {
        assertEquals(16.sp, NowChipMetrics.FONT_SIZE)
        assertEquals(20.sp, NowChipMetrics.LINE_HEIGHT)
    }

    @Test
    fun `clock icon scales with the text`() {
        assertEquals(16.dp, NowChipMetrics.ICON_SIZE)
    }

    @Test
    fun `now line stays smaller than the prayer time`() {
        assertTrue(NowChipMetrics.FONT_SIZE.value < 44f)
        assertTrue(NowChipMetrics.LINE_HEIGHT.value >= NowChipMetrics.FONT_SIZE.value)
    }
}
