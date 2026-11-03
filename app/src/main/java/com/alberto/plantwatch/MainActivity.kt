package com.alberto.plantwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
                PlaceholderScreen()
            }
        }
    }
}

// A @Composable function describes UI; Compose re-runs it when its inputs change.
// This placeholder will be replaced by the plant list in the next step.
@Composable
fun PlaceholderScreen(modifier: Modifier = Modifier) {
    // Scaffold provides the standard screen layout; `innerPadding` keeps
    // content clear of the system bars.
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.placeholder_no_plants))
        }
    }
}

// @Preview renders the composable in the IDE without running the app.
@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    PlantWatchTheme {
        PlaceholderScreen()
    }
}
