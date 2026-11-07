package com.mawaqiti.plus.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Great-circle helpers for the qibla compass.
 */
object Qibla {

    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    private const val DEG = Math.PI / 180.0

    /** Initial great-circle bearing from (lat, lon) to the Kaaba, degrees clockwise from north. */
    fun bearing(latitude: Double, longitude: Double): Double {
        val phi1 = latitude * DEG
        val phi2 = KAABA_LATITUDE * DEG
        val deltaLambda = (KAABA_LONGITUDE - longitude) * DEG

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val degrees = atan2(y, x) / DEG
        return (degrees + 360.0) % 360.0
    }

    /** Great-circle distance to the Kaaba in kilometres. */
    fun distanceKm(latitude: Double, longitude: Double): Double {
        val phi1 = latitude * DEG
        val phi2 = KAABA_LATITUDE * DEG
        val dPhi = (KAABA_LATITUDE - latitude) * DEG
        val dLambda = (KAABA_LONGITUDE - longitude) * DEG
        val a = sin(dPhi / 2) * sin(dPhi / 2) +
            cos(phi1) * cos(phi2) * sin(dLambda / 2) * sin(dLambda / 2)
        return 6371.0088 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Signed shortest angular difference target - current, in (-180, 180]. */
    fun signedDelta(target: Double, current: Double): Double {
        var delta = (target - current) % 360.0
        if (delta > 180.0) delta -= 360.0
        if (delta <= -180.0) delta += 360.0
        return delta
    }

    /** Normalises an angle into [0, 360). */
    fun normalize(degrees: Double): Double {
        var value = degrees % 360.0
        if (value < 0.0) value += 360.0
        return value
    }
}
