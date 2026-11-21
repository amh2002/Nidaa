package com.mawaqiti.plus

import com.mawaqiti.plus.engine.table.TableDay
import com.mawaqiti.plus.ui.theme.CelestialPhase
import com.mawaqiti.plus.ui.theme.GoldenPhase
import com.mawaqiti.plus.ui.theme.PhaseBounds
import com.mawaqiti.plus.ui.theme.celestialOf
import com.mawaqiti.plus.ui.theme.celestialPhase
import com.mawaqiti.plus.ui.theme.resolvePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * beta23: the celestial indicator switches exactly on the computed prayer
 * minutes, and the moon never appears while the sun is up.
 *
 * Bounds are a realistic June day: Fajr 04:05, Sunrise 05:38, Asr 16:20,
 * Maghrib 19:45, Isha 21:10.
 */
class CelestialPhaseTest {

    private val bounds = PhaseBounds(
        fajr = 4 * 60 + 5,
        sunrise = 5 * 60 + 38,
        asr = 16 * 60 + 20,
        maghrib = 19 * 60 + 45,
        isha = 21 * 60 + 10
    )

    @Test
    fun `each phase starts on its own computed minute`() {
        // Half-open windows: the boundary minute belongs to the NEW phase.
        assertEquals(CelestialPhase.MOON, celestialPhase(bounds.isha, bounds))
        assertEquals(CelestialPhase.MOON, celestialPhase(23 * 60 + 59, bounds))
        assertEquals(CelestialPhase.MOON, celestialPhase(0, bounds))
        assertEquals(CelestialPhase.MOON, celestialPhase(bounds.fajr - 1, bounds))

        assertEquals(CelestialPhase.RISING_SUN, celestialPhase(bounds.fajr, bounds))
        assertEquals(CelestialPhase.RISING_SUN, celestialPhase(bounds.sunrise - 1, bounds))

        assertEquals(CelestialPhase.FULL_SUN, celestialPhase(bounds.sunrise, bounds))
        assertEquals(CelestialPhase.FULL_SUN, celestialPhase(bounds.asr - 1, bounds))

        assertEquals(CelestialPhase.LOW_SUN, celestialPhase(bounds.asr, bounds))
        assertEquals(CelestialPhase.LOW_SUN, celestialPhase(bounds.maghrib - 1, bounds))

        assertEquals(CelestialPhase.SETTING_SUN, celestialPhase(bounds.maghrib, bounds))
        assertEquals(CelestialPhase.SETTING_SUN, celestialPhase(bounds.isha - 1, bounds))
    }

    @Test
    fun `the moon never appears during a daylight phase`() {
        val daylightPhases = listOf(
            CelestialPhase.RISING_SUN,
            CelestialPhase.FULL_SUN,
            CelestialPhase.LOW_SUN,
            CelestialPhase.SETTING_SUN
        )
        for (phase in daylightPhases) {
            assertTrue("$phase must be daylight", phase.isDaylight)
        }
        assertFalse("moon is not daylight", CelestialPhase.MOON.isDaylight)

        // Walk a whole day minute by minute and assert the invariant: between
        // sunrise and isha the answer is never MOON.
        for (minute in 0 until 1440) {
            val phase = celestialPhase(minute, bounds)
            if (minute in bounds.sunrise until bounds.isha) {
                assertTrue(
                    "moon shown at ${minute / 60}:${minute % 60} (daylight)",
                    phase != CelestialPhase.MOON
                )
            }
        }
    }

    @Test
    fun `night wraps midnight`() {
        // After Isha the moon runs until Fajr of the next day.
        var moonMinutes = 0
        for (minute in 0 until 1440) {
            if (celestialPhase(minute, bounds) == CelestialPhase.MOON) moonMinutes++
        }
        val expected = (60 * 24) - (bounds.isha - bounds.fajr)
        assertEquals("moon window length", expected, moonMinutes)
    }

    @Test
    fun `the five golden phases map one to one`() {
        val pairs = mapOf(
            GoldenPhase.NIGHT to CelestialPhase.MOON,
            GoldenPhase.DAWN to CelestialPhase.RISING_SUN,
            GoldenPhase.MORNING to CelestialPhase.FULL_SUN,
            GoldenPhase.AFTERNOON to CelestialPhase.LOW_SUN,
            GoldenPhase.SUNSET to CelestialPhase.SETTING_SUN
        )
        for ((golden, celestial) in pairs) {
            assertEquals(celestial, celestialOf(golden))
        }

        // The two resolvers must always agree, so the card icon and the sky
        // palette can never contradict each other.
        for (minute in 0 until 1440) {
            assertEquals(
                "resolvers disagree at $minute",
                celestialOf(resolvePhase(minute, bounds)),
                celestialPhase(minute, bounds)
            )
        }
    }

    @Test
    fun `phase bounds come from the engine day unchanged`() {
        val day = TableDay(
            fajr = 245, sunrise = 338, dhuhr = 760, asr = 980,
            maghrib = 1185, isha = 1270, lastThird = 1300,
            fajrTomorrow = 244, ishaYesterday = 1268, row = 0
        )
        val fromDay = PhaseBounds.fromDay(day)
        assertEquals(day.fajr, fromDay.fajr)
        assertEquals(day.sunrise, fromDay.sunrise)
        assertEquals(day.asr, fromDay.asr)
        assertEquals(day.maghrib, fromDay.maghrib)
        assertEquals(day.isha, fromDay.isha)

        // Nothing re-derives the times: the same inputs give the same phase.
        assertEquals(celestialPhase(980, fromDay), celestialPhase(980, bounds.copy(
            fajr = 245, sunrise = 338, asr = 980, maghrib = 1185, isha = 1270
        )))
    }
}
