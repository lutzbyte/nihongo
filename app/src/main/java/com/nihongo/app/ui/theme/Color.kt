package com.nihongo.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E4FF),
    onPrimaryContainer = Color(0xFF1E1B6B),
    secondary = Color(0xFF6B73C9),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E8FF),
    onSecondaryContainer = Color(0xFF1F2354),
    tertiary = Color(0xFFE08A4A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE8D6),
    onTertiaryContainer = Color(0xFF4A2A10),
    background = Color(0xFFEEF0FF),
    onBackground = Color(0xFF141629),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141629),
    surfaceVariant = Color(0xFFF1F2FB),
    onSurfaceVariant = Color(0xFF6B6F85),
    outline = Color(0xFFD9DCEF),
    outlineVariant = Color(0xFFE6E8F6),
    error = Color(0xFFD9534F),
    onError = Color(0xFFFFFFFF),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C83FF),
    onPrimary = Color(0xFF10123A),
    primaryContainer = Color(0xFF2D3170),
    onPrimaryContainer = Color(0xFFE0E4FF),
    secondary = Color(0xFF9CA3F0),
    onSecondary = Color(0xFF1B1F4A),
    secondaryContainer = Color(0xFF2B2F5C),
    onSecondaryContainer = Color(0xFFE6E8FF),
    tertiary = Color(0xFFF0A36B),
    onTertiary = Color(0xFF3A1D08),
    tertiaryContainer = Color(0xFF4A3322),
    onTertiaryContainer = Color(0xFFFFE8D6),
    background = Color(0xFF0B0C10),
    onBackground = Color(0xFFECEDF5),
    surface = Color(0xFF1B1D26),
    onSurface = Color(0xFFECEDF5),
    surfaceVariant = Color(0xFF262A38),
    onSurfaceVariant = Color(0xFF9A9FB5),
    outline = Color(0xFF3A3E52),
    outlineVariant = Color(0xFF2C3042),
    error = Color(0xFFFF7A74),
    onError = Color(0xFF3A0A08),
)

@Immutable
data class NihongoExtraColors(
    val tileBlue: Color,
    val tileOrange: Color,
    val tileGreen: Color,
    val tilePurple: Color,
    val streak: Color,
    val success: Color,
    val chartBar: Color,
)

val LightExtraColors = NihongoExtraColors(
    tileBlue = Color(0xFFDDE6FF),
    tileOrange = Color(0xFFFFE6D3),
    tileGreen = Color(0xFFDDF3EE),
    tilePurple = Color(0xFFE4E3FF),
    streak = Color(0xFFF97316),
    success = Color(0xFF16A34A),
    chartBar = Color(0xFF4C8DF6),
)

val DarkExtraColors = NihongoExtraColors(
    tileBlue = Color(0xFF2A3350),
    tileOrange = Color(0xFF3B2E2A),
    tileGreen = Color(0xFF253A38),
    tilePurple = Color(0xFF2B2B4D),
    streak = Color(0xFFFB923C),
    success = Color(0xFF4ADE80),
    chartBar = Color(0xFF6BA3FF),
)

val LocalNihongoExtraColors = staticCompositionLocalOf { LightExtraColors }