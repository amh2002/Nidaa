package com.mawaqiti.plus.data

import android.content.Context
import com.mawaqiti.plus.engine.table.TableProfile
import java.text.Collator
import java.util.Locale
import org.json.JSONArray

/**
 * Reads the 90-city list extracted from the original APK.
 *
 * The list is bundled as an asset and never refreshed from the network: the
 * beta is fully offline and the server-side city API could not be retrieved
 * during the RE (it is authenticated).
 */
class CityRepository(context: Context) {

    private val cities: List<City> = load(context)

    fun all(): List<City> = cities

    fun byOrder(order: Int): City =
        cities.firstOrNull { it.order == order } ?: cities.first { it.order == City.DEFAULT_ORDER }

    fun defaultCity(): City = byOrder(City.DEFAULT_ORDER)

    fun forCountry(country: Country, isArabic: Boolean): List<City> {
        val collator = Collator.getInstance(if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH)
        return cities
            .filter { it.country == country }
            .sortedWith { left, right ->
                collator.compare(
                    if (isArabic) left.nameArDisplay else left.nameEnDisplay,
                    if (isArabic) right.nameArDisplay else right.nameEnDisplay
                )
            }
    }

    fun countFor(country: Country): Int = cities.count { it.country == country }

    private fun load(context: Context): List<City> {
        val text = context.assets.open("cities.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val array = JSONArray(text)
        val result = ArrayList<City>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            result += City(
                order = item.getInt("order"),
                nameAr = item.getString("nameAr"),
                nameEn = item.getString("nameEn"),
                country = Country.fromCode(item.getString("country")),
                minDiff = item.getInt("minDiff"),
                profile = profileFrom(item.getString("profile")),
                latitude = item.getDouble("lat"),
                longitude = item.getDouble("lon"),
                pseudo = item.optBoolean("pseudo", false)
            )
        }
        return result.sortedBy { it.order }
    }

    private fun profileFrom(value: String): TableProfile = when (value.lowercase(Locale.US)) {
        "makkah" -> TableProfile.MAKKAH
        "madinah" -> TableProfile.MADINAH
        else -> TableProfile.DEFAULT
    }
}
