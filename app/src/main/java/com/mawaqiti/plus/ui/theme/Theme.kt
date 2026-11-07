package com.mawaqiti.plus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F5132),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3ECDD),
    onPrimaryContainer = Color(0xFF052716),
    secondary = Color(0xFF7A5F12),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E6AE),
    onSecondaryContainer = Color(0xFF2A2100),
    tertiary = Color(0xFF35617A),
    onTertiary = Color.White,
    background = Color(0xFFF7F5EF),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFE1E5DF),
    onSurfaceVariant = Color(0xFF414942),
    outline = Color(0xFF717971),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD5B3),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF0B5132),
    onPrimaryContainer = Color(0xFFB8F0CE),
    secondary = Color(0xFFE2C46A),
    onSecondary = Color(0xFF3B2F00),
    secondaryContainer = Color(0xFF554500),
    onSecondaryContainer = Color(0xFFF6E6AE),
    tertiary = Color(0xFF9DCBE6),
    onTertiary = Color(0xFF00344A),
    background = Color(0xFF101613),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF161D19),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC0C9C0),
    outline = Color(0xFF8B938B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun MawaqitiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
