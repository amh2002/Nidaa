package com.mawaqiti.plus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mawaqiti.plus.R

/**
 * Bundled Tajawal (SIL OFL 1.1) for all UI text and Amiri Bold (SIL OFL 1.1)
 * for the wordmark only. Latin digits are used in both languages, so the
 * numeral glyphs are the same everywhere.
 */
val Tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold)
)

val AmiriBold = FontFamily(
    Font(R.font.amiri_bold, FontWeight.Bold)
)

private fun tajawal(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal
) = TextStyle(
    fontFamily = Tajawal,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp
)

val MawaqitiTypography = Typography(
    displayLarge = tajawal(52, 60, FontWeight.Bold),
    displayMedium = tajawal(44, 52, FontWeight.Bold),
    displaySmall = tajawal(38, 46, FontWeight.Bold),
    headlineLarge = tajawal(34, 42, FontWeight.Bold),
    headlineMedium = tajawal(30, 38, FontWeight.Bold),
    headlineSmall = tajawal(25, 32, FontWeight.Bold),
    titleLarge = tajawal(22, 29, FontWeight.Bold),
    titleMedium = tajawal(18, 25, FontWeight.Medium),
    titleSmall = tajawal(16, 22, FontWeight.Medium),
    bodyLarge = tajawal(17, 26),
    bodyMedium = tajawal(15, 23),
    bodySmall = tajawal(13, 19),
    labelLarge = tajawal(15, 20, FontWeight.Medium),
    labelMedium = tajawal(13, 18, FontWeight.Medium),
    labelSmall = tajawal(11, 16, FontWeight.Medium)
)
