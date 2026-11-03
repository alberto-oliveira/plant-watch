package com.alberto.plantwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alberto.plantwatch.ui.theme.PlantWatchTheme

// `class X : Y()` = X extends Y, calling Y's no-arg constructor.
// ComponentActivity is the minimal activity base class that supports Compose.
class MainActivity : ComponentActivity() {

    // `override fun` overrides a superclass method. `Bundle?` means "Bundle or null":
    // Kotlin types are non-null by default; `?` opts in to nullability.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status/navigation bars (mandatory look when targeting API 35).
        enableEdgeToEdge()
        // setContent { ... } takes a lambda: the UI is described by composable functions.
        setContent {
            PlantWatchTheme {
                // All screens and the navigation between them (PlantWatchNavHost.kt).
                PlantWatchNavHost()
            }
        }
    }
}
