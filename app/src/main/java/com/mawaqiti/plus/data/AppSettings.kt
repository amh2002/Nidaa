package com.mawaqiti.plus.data

import com.mawaqiti.plus.engine.AsrMadhab
import com.mawaqiti.plus.engine.CalculationMethod
import com.mawaqiti.plus.engine.CalculationSettings
import com.mawaqiti.plus.engine.HighLatitudeRule

enum class LocationSource { DEFAULT, GPS, MANUAL }

data class AppSettings(
    val method: CalculationMethod = CalculationMethod.MWL,
    val asrMadhab: AsrMadhab = AsrMadhab.SHAFI,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.NIGHT_MIDDLE,
    val customFajrAngle: Double = 18.0,
    val customIshaAngle: Double = 17.0,
    val latitude: Double = 31.7683,
    val longitude: Double = 35.2137,
    val cityNameAr: String = "القدس",
    val cityNameEn: String = "Al-Quds",
    val locationSource: LocationSource = LocationSource.DEFAULT,
    val onboardingDone: Boolean = false,
    val imsakOffsetMinutes: Int = 10
) {
    fun toCalculationSettings(): CalculationSettings = CalculationSettings(
        method = method,
        asrMadhab = asrMadhab,
        highLatitudeRule = highLatitudeRule,
        customFajrAngle = customFajrAngle,
        customIshaAngle = customIshaAngle,
        imsakOffsetMinutes = imsakOffsetMinutes
    )
}
