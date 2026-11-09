package com.mawaqiti.plus

import com.mawaqiti.plus.ui.theme.SkyPhase
import org.junit.Assert.assertEquals
import org.junit.Test

class SkyPhaseTest {

    // 05:00 Fajr, 06:00 sunrise, 15:30 Asr, 18:00 Maghrib.
    private val fajr = 300
    private val sunrise = 360
    private val asr = 930
    private val maghrib = 1080

    private fun phase(minute: Int) =
        SkyPhase.forMinutes(minute, fajr, sunrise, asr, maghrib)

    @Test
    fun dawnStartsNinetyMinutesBeforeFajr() {
        assertEquals(SkyPhase.NIGHT, phase(209))
        assertEquals(SkyPhase.DAWN, phase(210))
        assertEquals(SkyPhase.DAWN, phase(300))
        assertEquals(SkyPhase.DAWN, phase(359))
    }

    @Test
    fun dayRunsFromSunriseToAsr() {
        assertEquals(SkyPhase.DAY, phase(360))
        assertEquals(SkyPhase.DAY, phase(600))
        assertEquals(SkyPhase.DAY, phase(929))
    }

    @Test
    fun goldenRunsFromAsrToMaghrib() {
        assertEquals(SkyPhase.GOLDEN, phase(930))
        assertEquals(SkyPhase.GOLDEN, phase(1000))
        assertEquals(SkyPhase.GOLDEN, phase(1079))
    }

    @Test
    fun nightRunsFromMaghribToDawn() {
        assertEquals(SkyPhase.NIGHT, phase(1080))
        assertEquals(SkyPhase.NIGHT, phase(1200))
        assertEquals(SkyPhase.NIGHT, phase(1439))
    }

    @Test
    fun dawnWindowWrapsAcrossMidnight() {
        // High-latitude winter case: Fajr 00:10, sunrise 00:50, so dawn starts
        // at 22:40 on the previous evening.
        val wrappingFajr = 10
        val wrappingSunrise = 50
        val wrappingAsr = 780
        val wrappingMaghrib = 990

        fun wrapped(minute: Int) = SkyPhase.forMinutes(
            minute, wrappingFajr, wrappingSunrise, wrappingAsr, wrappingMaghrib
        )

        assertEquals(SkyPhase.NIGHT, wrapped(1359))
        assertEquals(SkyPhase.DAWN, wrapped(1360))
        assertEquals(SkyPhase.DAWN, wrapped(1439))
        assertEquals(SkyPhase.DAWN, wrapped(0))
        assertEquals(SkyPhase.DAWN, wrapped(49))
        assertEquals(SkyPhase.DAY, wrapped(50))
        assertEquals(SkyPhase.GOLDEN, wrapped(780))
        assertEquals(SkyPhase.NIGHT, wrapped(990))
        assertEquals(SkyPhase.NIGHT, wrapped(1200))
        assertEquals(SkyPhase.DAY, wrapped(600))
    }

    @Test
    fun everyPhaseHasDistinctPalette() {
        val palettes = SkyPhase.entries.map { com.mawaqiti.plus.ui.theme.SkyPalette.of(it) }
        assertEquals(SkyPhase.entries.size, palettes.size)
        assertEquals(SkyPhase.entries.toSet(), palettes.map { it.phase }.toSet())
    }
}
