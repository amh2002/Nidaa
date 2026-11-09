package com.mawaqiti.plus

import com.mawaqiti.plus.data.City
import com.mawaqiti.plus.data.Country
import com.mawaqiti.plus.engine.table.TableProfile
import com.mawaqiti.plus.ui.CitySearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CitySearchTest {

    private fun city(order: Int, ar: String, en: String) = City(
        order = order,
        nameAr = ar,
        nameEn = en,
        country = Country.PS,
        minDiff = 0,
        profile = TableProfile.DEFAULT,
        latitude = 31.9,
        longitude = 35.2
    )

    private val cities = listOf(
        city(0, "القدس", "Jerusalem"),
        city(1, "نَابُلُس", "Nablus"),
        city(2, "أريحا", "Jericho"),
        city(3, "رام الله", "Ramallah"),
        city(4, "خان يونس", "Khan Younis")
    )

    @Test
    fun emptyQueryKeepsEveryCity() {
        assertEquals(cities, CitySearch.filter(cities, ""))
        assertEquals(cities, CitySearch.filter(cities, "   "))
    }

    @Test
    fun matchesArabicName() {
        assertEquals(listOf(cities[0]), CitySearch.filter(cities, "قدس"))
        assertEquals(listOf(cities[3]), CitySearch.filter(cities, "رام"))
    }

    @Test
    fun matchesEnglishNameCaseInsensitively() {
        assertEquals(listOf(cities[0]), CitySearch.filter(cities, "JERU"))
        assertEquals(listOf(cities[3]), CitySearch.filter(cities, "ramaLLah"))
    }

    @Test
    fun ignoresArabicDiacriticsInBothDirections() {
        // Query without diacritics against a name that has them.
        assertEquals(listOf(cities[1]), CitySearch.filter(cities, "نابلس"))
        // Query with diacritics against a name without them.
        val plain = city(9, "نابلس", "Nablus")
        assertTrue(CitySearch.matches(plain, "نَابُلُس"))
    }

    @Test
    fun foldsAlefAndYaVariants() {
        assertEquals(listOf(cities[2]), CitySearch.filter(cities, "اريحا"))
        assertEquals(listOf(cities[2]), CitySearch.filter(cities, "أريحا"))
        val withYa = city(10, "القدسى", "Qudsi")
        assertTrue(CitySearch.matches(withYa, "القدسي"))
    }

    @Test
    fun noMatchReturnsEmpty() {
        assertTrue(CitySearch.filter(cities, "zzz").isEmpty())
        assertTrue(CitySearch.filter(cities, "غزة").isEmpty())
    }

    @Test
    fun normalizationCollapsesWhitespaceAndTatweel() {
        val extended = city(11, "خـــان   يونس", "Khan Younis")
        assertTrue(CitySearch.matches(extended, "خان يونس"))
        assertEquals("خان يونس", CitySearch.normalize("  خان   يونس "))
    }

    @Test
    fun displayNamesAreTrimmed() {
        val padded = city(12, "القدس ", "Jerusalem ")
        assertEquals("القدس", padded.nameArDisplay)
        assertEquals("Jerusalem", padded.nameEnDisplay)
        assertFalse(padded.nameArDisplay.endsWith(" "))
    }
}
