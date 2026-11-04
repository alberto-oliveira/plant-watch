package com.alberto.plantwatch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * App-wide Material 3 theme.
 *
 * Uses "dynamic color" (colors derived from the phone's wallpaper). It exists
 * since Android 12 (API 31) and our minSdk is 34, so no version check is needed
 * and no custom palette has to be maintained.
 *
 * `content: @Composable () -> Unit` is a parameter whose type is "a composable
 * function with no arguments returning nothing". Because it is the last parameter,
 * callers can pass it as a trailing lambda: `PlantWatchTheme { ... }`.
 */
@Composable
fun PlantWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // `if` is an expression in Kotlin: it returns a value.
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
