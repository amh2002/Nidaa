package com.mawaqiti.plus.ui

import com.mawaqiti.plus.data.City

/**
 * Search helpers for the city picker.
 *
 * Matching is presentation-only: it never changes a city, its profile or its
 * `minDiff`. It is case-insensitive, ignores Arabic diacritics and tatweel,
 * folds the common alef/ya/ta-marbuta variants, and searches both the Arabic
 * and English names at once.
 */
object CitySearch {

    fun normalize(text: String): String {
        val builder = StringBuilder(text.length)
        for (char in text) {
            when {
                char.code in 0x064B..0x065F -> Unit // harakat / tanween / shadda / sukun
                char.code == 0x0670 -> Unit // superscript alef
                char.code == 0x0640 -> Unit // tatweel
                char.code in 0x0300..0x036F -> Unit // Latin combining marks
                char == 'أ' || char == 'إ' || char == 'آ' || char == 'ٱ' -> builder.append('ا')
                char == 'ى' -> builder.append('ي')
                char == 'ئ' -> builder.append('ي')
                char == 'ؤ' -> builder.append('و')
                char == 'ة' -> builder.append('ه')
                else -> builder.append(char.lowercaseChar())
            }
        }
        return builder.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun matches(city: City, query: String): Boolean {
        val needle = normalize(query)
        if (needle.isEmpty()) return true
        return normalize(city.nameArDisplay).contains(needle) ||
            normalize(city.nameEnDisplay).contains(needle)
    }

    fun filter(cities: List<City>, query: String): List<City> =
        if (query.isBlank()) cities else cities.filter { matches(it, query) }
}
