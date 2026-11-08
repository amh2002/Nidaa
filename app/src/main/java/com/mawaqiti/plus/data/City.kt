package com.mawaqiti.plus.data

import com.mawaqiti.plus.engine.table.TableProfile

/** Countries exposed by the zero-typing cascade. */
enum class Country(val code: String) {
    PS("PS"),
    SA("SA");

    companion object {
        fun fromCode(code: String): Country =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: PS
    }
}

/**
 * One entry of the original app's embedded city list (`y7.C3444b`).
 *
 * [nameAr] is kept verbatim, including the two names that ship with a trailing
 * space, because the original app derives the table profile from the exact
 * string. [profile] is stored explicitly so the lookup stays byte-identical
 * even if a display name is trimmed.
 */
data class City(
    val order: Int,
    val nameAr: String,
    val nameEn: String,
    val country: Country,
    val minDiff: Int,
    val profile: TableProfile,
    val latitude: Double,
    val longitude: Double,
    val pseudo: Boolean = false
) {
    val nameArDisplay: String get() = nameAr.trim()
    val nameEnDisplay: String get() = nameEn.trim()

    fun displayName(isArabic: Boolean): String =
        if (isArabic) nameArDisplay else nameEnDisplay

    fun countryName(isArabic: Boolean): String = when (country) {
        Country.PS -> if (isArabic) "فلسطين" else "Palestine"
        Country.SA -> if (isArabic) "السعودية" else "Saudi Arabia"
    }

    fun label(isArabic: Boolean): String = "${displayName(isArabic)} – ${countryName(isArabic)}"

    companion object {
        /** The original app's default city: القدس with minDiff = -2. */
        const val DEFAULT_ORDER = 0
    }
}
