package com.alberto.plantwatch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
                RequestNotificationPermission()
                // All screens and the navigation between them (PlantWatchNavHost.kt).
                PlantWatchNavHost()
            }
        }
    }
}

/**
 * Asks for the notification permission when the app starts, if it isn't
 * granted yet. Without it, the daily reminder can't be shown. (A settings
 * screen to re-enable it comes later.)
 *
 * Compose/Android notes:
 * - `rememberLauncherForActivityResult` registers a "launcher" for a system
 *   dialog; `RequestPermission()` is the contract for the permission dialog.
 *   The lambda receives the answer (true = granted). We don't need it: the
 *   reminder checks the permission itself before posting.
 * - `LaunchedEffect(Unit) { ... }` runs its block once when this composable
 *   first appears (not on every recomposition).
 * - `rememberSaveable` flag: don't ask again after a screen rotation, which
 *   recreates the activity.
 * - If the user denies twice, Android stops showing the dialog by itself.
 */
@Composable
private fun RequestNotificationPermission() {
    val context = LocalContext.current
    var alreadyAsked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted && !alreadyAsked) {
            alreadyAsked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
