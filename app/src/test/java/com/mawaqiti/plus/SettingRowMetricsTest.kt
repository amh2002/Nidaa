package com.mawaqiti.plus

import com.mawaqiti.plus.ui.components.SettingRowMetrics
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The settings row keeps the control on the trailing side only while the label
 * column is still at least 132dp wide. These cases mirror the widths that broke
 * beta2 (narrow RTL phones and fontScale 1.3).
 */
class SettingRowMetricsTest {

    private val gap = 36
    private val minLabel = 396

    @Test
    fun wideRowKeepsControlTrailing() {
        assertFalse(
            SettingRowMetrics.shouldStack(
                contentWidthPx = 1080,
                controlWidthPx = 420,
                gapPx = gap,
                minLabelWidthPx = minLabel
            )
        )
    }

    @Test
    fun narrowRowStacksControlUnderText() {
        assertTrue(
            SettingRowMetrics.shouldStack(
                contentWidthPx = 720,
                controlWidthPx = 420,
                gapPx = gap,
                minLabelWidthPx = minLabel
            )
        )
    }

    @Test
    fun largeFontGrowsControlAndTriggersStacking() {
        assertFalse(
            SettingRowMetrics.shouldStack(1080, 520, gap, minLabel)
        )
        assertTrue(
            SettingRowMetrics.shouldStack(1080, 680, gap, minLabel)
        )
    }

    @Test
    fun exactMinimumStaysTrailingAndOnePixelLessStacks() {
        assertFalse(SettingRowMetrics.shouldStack(1080, 648, gap, minLabel))
        assertTrue(SettingRowMetrics.shouldStack(1080, 649, gap, minLabel))
    }
}
