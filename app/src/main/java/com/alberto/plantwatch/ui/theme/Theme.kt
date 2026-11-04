package com.alberto.plantwatch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * App-wide Material 3 theme with the app's own green palette (see Color.kt),
 * in a light and a dark version that follows the phone's dark-mode setting.
 *
 * (Until v0.1 the colours came from the wallpaper, "dynamic color"; a fixed
 * palette gives the app its own look.)
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
    MaterialTheme(
        // `if` is an expression in Kotlin: it returns a value.
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
