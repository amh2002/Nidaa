package com.mawaqiti.plus.data

import android.content.Context
import org.json.JSONArray
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class City(
    val nameAr: String,
    val nameEn: String,
    val countryAr: String,
    val countryEn: String,
    val latitude: Double,
    val longitude: Double
) {
    fun matches(query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return false
        return nameAr.contains(needle, ignoreCase = true) ||
            nameEn.contains(needle, ignoreCase = true) ||
            countryAr.contains(needle, ignoreCase = true) ||
            countryEn.contains(needle, ignoreCase = true)
    }
}

data class NearestCity(val city: City, val distanceKm: Double)

class CityRepository(context: Context) {

    private val cities: List<City> = load(context)

    fun all(): List<City> = cities

    fun search(query: String, limit: Int = 12): List<City> =
        cities.filter { it.matches(query) }.take(limit)

    /** Nearest bundled city, useful for labelling a GPS fix. */
    fun nearest(latitude: Double, longitude: Double): NearestCity? {
        val best = cities.minByOrNull { distanceKm(latitude, longitude, it.latitude, it.longitude) }
            ?: return null
        return NearestCity(best, distanceKm(latitude, longitude, best.latitude, best.longitude))
    }

    private fun load(context: Context): List<City> {
        val text = context.assets.open("cities.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(text)
        val result = ArrayList<City>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            result += City(
                nameAr = item.getString("name_ar"),
                nameEn = item.getString("name_en"),
                countryAr = item.optString("country_ar"),
                countryEn = item.optString("country_en"),
                latitude = item.getDouble("lat"),
                longitude = item.getDouble("lon")
            )
        }
        return result
    }

    companion object {
        private const val EARTH_RADIUS_KM = 6371.0088

        fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val phi1 = Math.toRadians(lat1)
            val phi2 = Math.toRadians(lat2)
            val dPhi = Math.toRadians(lat2 - lat1)
            val dLambda = Math.toRadians(lon2 - lon1)
            val a = sin(dPhi / 2) * sin(dPhi / 2) +
                cos(phi1) * cos(phi2) * sin(dLambda / 2) * sin(dLambda / 2)
            return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
