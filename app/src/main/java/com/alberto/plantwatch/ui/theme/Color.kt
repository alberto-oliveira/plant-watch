package com.alberto.plantwatch.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * The app's fixed colour palette: leaf green (primary), sage (secondary) and a
 * warm amber (tertiary, used for "due today" and for sunlight). Error stays
 * Material's standard red, so it isn't listed.
 *
 * Material 3 colours come in roles, each with an "on" partner for text and icons
 * drawn on top of it (primary / onPrimary, primaryContainer / onPrimaryContainer...).
 * The values follow Material's tonal-palette approach: light and dark schemes use
 * the same hues at different lightness, so both stay readable.
 *
 * `Color(0xFF2E6B30)` is ARGB in hex: FF = fully opaque, then red, green, blue.
 */

val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B30),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB1F2A9),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF52634F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E8CF),
    onSecondaryContainer = Color(0xFF101F0F),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA3),
    onTertiaryContainer = Color(0xFF261900),
    background = Color(0xFFF7FBF1),
    onBackground = Color(0xFF181D17),
    surface = Color(0xFFF7FBF1),
    onSurface = Color(0xFF181D17),
    surfaceVariant = Color(0xFFDEE5D8),
    onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5EC),
    surfaceContainer = Color(0xFFEBEFE6),
    surfaceContainerHigh = Color(0xFFE6E9E0),
    surfaceContainerHighest = Color(0xFFE0E4DB),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF96D68F),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF145219),
    onPrimaryContainer = Color(0xFFB1F2A9),
    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF253423),
    secondaryContainer = Color(0xFF3B4B39),
    onSecondaryContainer = Color(0xFFD5E8CF),
    tertiary = Color(0xFFF6BE48),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDEA3),
    background = Color(0xFF10140F),
    onBackground = Color(0xFFE0E4DB),
    surface = Color(0xFF10140F),
    onSurface = Color(0xFFE0E4DB),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940),
    surfaceContainerLowest = Color(0xFF0B0F0A),
    surfaceContainerLow = Color(0xFF181D17),
    surfaceContainer = Color(0xFF1C211B),
    surfaceContainerHigh = Color(0xFF272B25),
    surfaceContainerHighest = Color(0xFF313630),
)
