package com.alberto.plantwatch

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alberto.plantwatch.plants.ui.PlantDetailRoute
import com.alberto.plantwatch.plants.ui.PlantListRoute
import kotlinx.serialization.Serializable

/*
 * Destinations ("routes") of the app. With type-safe navigation, each screen is
 * a @Serializable class, and its arguments are its properties: going to a plant
 * is `navigate(PlantDetail(plantId = 3))`, checked by the compiler, instead of
 * building a string like "plant/3" by hand.
 *
 * Kotlin notes:
 * - `data object`: a singleton (like `object`) with a readable toString().
 *   Used for screens without arguments.
 * - `@Serializable`: the kotlinx.serialization compiler plugin generates code
 *   to turn the class into data and back. Navigation uses it to store the
 *   destination in the back stack, which survives rotation and process death.
 */

@Serializable
data object PlantList

@Serializable
data class PlantDetail(val plantId: Long)

/**
 * The app's screens and how to move between them.
 *
 * - `rememberNavController()`: holds the back stack (the list of screens,
 *   current one on top). The system Back gesture pops it automatically.
 * - `composable<PlantDetail> { ... }`: "when PlantDetail is on top, show this".
 * - `navigate(...)` pushes a screen; `popBackStack()` goes back one.
 */
@Composable
fun PlantWatchNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = PlantList) {
        composable<PlantList> {
            PlantListRoute(
                onPlantClick = { plant -> navController.navigate(PlantDetail(plantId = plant.id)) },
            )
        }
        composable<PlantDetail> {
            // The plantId argument isn't read here: PlantDetailViewModel gets it
            // from its SavedStateHandle (see there).
            PlantDetailRoute(onBack = { navController.popBackStack() })
        }
    }
}
