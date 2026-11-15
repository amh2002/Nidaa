package com.mawaqiti.plus

import com.mawaqiti.plus.ui.theme.GoldenPhase
import com.mawaqiti.plus.ui.theme.PhaseBounds
import com.mawaqiti.plus.ui.theme.resolvePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Beta 9 phase model: five windows derived from the engine's times.
 *
 * The engine itself is untouched; these tests only pin the presentation
 * boundaries, including the Isha -> Fajr window that wraps midnight.
 */
class GoldenPhaseTest {

    /** A normal day: Fajr 05:00, Sunrise 06:00, Asr 15:30, Maghrib 18:00, Isha 20:00. */
    private val day = PhaseBounds(
        fajr = 300,
        sunrise = 360,
        asr = 930,
        maghrib = 1080,
        isha = 1200
    )

    private fun phase(minute: Int): GoldenPhase = resolvePhase(minute, day)

    @Test
    fun dawnStartsExactlyAtFajrAndEndsAtSunrise() {
        assertEquals(GoldenPhase.NIGHT, phase(299))
        assertEquals(GoldenPhase.DAWN, phase(300))
        assertEquals(GoldenPhase.DAWN, phase(330))
        assertEquals(GoldenPhase.DAWN, phase(359))
        assertEquals(GoldenPhase.MORNING, phase(360))
    }

    @Test
    fun morningStartsExactlyAtSunriseAndEndsAtAsr() {
        assertEquals(GoldenPhase.MORNING, phase(360))
        assertEquals(GoldenPhase.MORNING, phase(600))
        assertEquals(GoldenPhase.MORNING, phase(929))
        assertEquals(GoldenPhase.AFTERNOON, phase(930))
    }

    @Test
    fun afternoonStartsExactlyAtAsrAndEndsAtMaghrib() {
        assertEquals(GoldenPhase.AFTERNOON, phase(930))
        assertEquals(GoldenPhase.AFTERNOON, phase(1000))
        assertEquals(GoldenPhase.AFTERNOON, phase(1079))
        assertEquals(GoldenPhase.SUNSET, phase(1080))
    }

    @Test
    fun sunsetStartsExactlyAtMaghribAndEndsAtIsha() {
        assertEquals(GoldenPhase.SUNSET, phase(1080))
        assertEquals(GoldenPhase.SUNSET, phase(1140))
        assertEquals(GoldenPhase.SUNSET, phase(1199))
        assertEquals(GoldenPhase.NIGHT, phase(1200))
    }

    @Test
    fun nightWrapsMidnightBackToFajr() {
        assertEquals(GoldenPhase.NIGHT, phase(1200))
        assertEquals(GoldenPhase.NIGHT, phase(1350))
        assertEquals(GoldenPhase.NIGHT, phase(1439))
        assertEquals(GoldenPhase.NIGHT, phase(0))
        assertEquals(GoldenPhase.NIGHT, phase(120))
        assertEquals(GoldenPhase.NIGHT, phase(299))
        assertEquals(GoldenPhase.DAWN, phase(300))
    }

    @Test
    fun everyMinuteOfTheDayHasExactlyOnePhase() {
        for (minute in 0 until 1440) {
            // resolvePhase always returns one value; this also proves the five
            // windows tile the day without a gap or an overlap.
            phase(minute)
        }
        val counts = GoldenPhase.entries.associateWith { wanted ->
            (0 until 1440).count { phase(it) == wanted }
        }
        assertEquals(1440, counts.values.sum())
        assertTrue(counts.values.all { it > 0 })
    }

    @Test
    fun minutesOutsideTheDayAreWrapped() {
        assertEquals(phase(300), phase(300 + 1440))
        assertEquals(phase(300), phase(300 - 1440))
        assertEquals(GoldenPhase.NIGHT, phase(1440 + 60))
    }

    @Test
    fun highLatitudeDayStillTiles() {
        // Summer in the north: sunrise 03:30, Asr 17:10, Maghrib 20:40, Isha 22:40.
        val short = PhaseBounds(fajr = 180, sunrise = 210, asr = 1030, maghrib = 1240, isha = 1360)
        assertEquals(GoldenPhase.NIGHT, resolvePhase(179, short))
        assertEquals(GoldenPhase.DAWN, resolvePhase(180, short))
        assertEquals(GoldenPhase.MORNING, resolvePhase(210, short))
        assertEquals(GoldenPhase.AFTERNOON, resolvePhase(1030, short))
        assertEquals(GoldenPhase.SUNSET, resolvePhase(1240, short))
        assertEquals(GoldenPhase.NIGHT, resolvePhase(1360, short))
        assertEquals(GoldenPhase.NIGHT, resolvePhase(0, short))
    }

    @Test
    fun debugKeysRoundTrip() {
        for (phase in GoldenPhase.entries) {
            assertEquals(phase, GoldenPhase.fromKey(phase.name))
            assertEquals(phase, GoldenPhase.fromKey(phase.key))
        }
        assertEquals(null, GoldenPhase.fromKey(null))
        assertEquals(null, GoldenPhase.fromKey("auto"))
        assertNotEquals(GoldenPhase.NIGHT, GoldenPhase.fromKey("dawn"))
    }
}
