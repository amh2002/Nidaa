package com.mawaqiti.plus

import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.ui.screens.HeroMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

/** Locks the hero spacing values requested in beta4 (6-8 dp and 4 dp). */
class HeroMetricsTest {

    @Test
    fun `label to prayer name is inside the requested 6-8 dp range`() {
        assertEquals(8.dp, HeroMetrics.LABEL_TO_NAME)
    }

    @Test
    fun `prayer name to time is exactly 4 dp`() {
        assertEquals(4.dp, HeroMetrics.NAME_TO_TIME)
    }
}
