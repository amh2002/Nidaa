package com.mawaqiti.plus

import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.ui.screens.NowTimeMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * beta14: the current-time row is text-only.
 *
 * The clock icon was removed completely (no icon box, no optical shift), so the
 * guard changed shape: it now pins the typography (still 20 sp on a 26 sp line,
 * never below the 18 sp floor) and the prefix, which must come from
 * `strings.xml` rather than from a Kotlin literal.
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
    fun `prefix is the dedicated string resource`() {
        assertEquals(R.string.current_time_prefix, NowTimeMetrics.PREFIX_RES)
        // The beta10 "الآن" label stays for other call sites; the row uses the
        // longer "الوقت الآن" prefix the owner asked for.
        assertNotEquals(R.string.now_label, NowTimeMetrics.PREFIX_RES)
    }

    @Test
    fun `line height always exceeds the font size`() {
        assertTrue(NowTimeMetrics.LINE_HEIGHT.value >= NowTimeMetrics.FONT_SIZE.value)
    }

    @Test
    fun `now row stays secondary to the prayer time`() {
        assertTrue(NowTimeMetrics.FONT_SIZE.value < 44f)
    }
}
