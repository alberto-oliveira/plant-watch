package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alberto.plantwatch.R
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.ui.theme.PlantWatchTheme

/**
 * The plant list screen as the app shows it: connects the [PlantListViewModel]
 * (data and actions) to the stateless [PlantListScreen] and the dialogs.
 *
 * "Route" composables are the only ones that know about ViewModels; everything
 * they call stays stateless and previewable.
 *
 * Kotlin/Compose notes:
 * - `viewModel(factory = ...)` returns the existing ViewModel if there is one
 *   (e.g. after a rotation) and only creates a new one the first time.
 * - `val plants by ...collectAsStateWithLifecycle()`: reads the StateFlow as
 *   Compose state, so this function re-runs ("recomposes") whenever the list
 *   changes. `by` is a delegated property: `plants` reads the state's value.
 * - `var x by rememberSaveable { mutableStateOf(...) }`: state owned by this
 *   composable. `mutableStateOf` makes changes trigger recomposition;
 *   `rememberSaveable` keeps the value across recompositions *and* across
 *   rotation (plain `remember` would lose it on rotation).
 */
@Composable
fun PlantListRoute(
    onPlantClick: (Plant) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: PlantListViewModel = viewModel(factory = PlantListViewModel.Factory),
) {
    val plants by viewModel.plants.collectAsStateWithLifecycle()

    // UI-only state: is the add dialog open, and what has been typed so far.
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var newPlantName by rememberSaveable { mutableStateOf("") }

    // Which plant is waiting for delete confirmation (null = no dialog). We keep
    // just its id: a Long can be saved across rotation, a whole Plant can't
    // without extra code. `mutableStateOf<Long?>(null)`: the explicit type says
    // "Long or null"; from `null` alone Kotlin couldn't infer it.
    var plantIdToDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    PlantListScreen(
        plants = plants,
        onPlantClick = onPlantClick,
        onSettingsClick = onSettingsClick,
        onAddClick = { showAddDialog = true },
        onDeleteClick = { plant -> plantIdToDelete = plant.id },
    )

    if (showAddDialog) {
        // Local function: closes the dialog and clears the field for next time.
        fun close() {
            newPlantName = ""
            showAddDialog = false
        }
        AddPlantDialog(
            name = newPlantName,
            onNameChange = { newPlantName = it },
            onConfirm = {
                viewModel.addPlant(newPlantName)
                close()
            },
            onDismiss = { close() },
        )
    }

    // `find` returns the first matching plant, or null. If the plant is gone
    // (already deleted), there's nothing to confirm and no dialog is shown.
    val plantToDelete = plants.find { it.id == plantIdToDelete }
    if (plantToDelete != null) {
        // Inside this `if`, Kotlin knows plantToDelete isn't null ("smart cast"),
        // so `.name` needs no `?.`.
        DeletePlantDialog(
            plantName = plantToDelete.name,
            onConfirm = {
                viewModel.deletePlant(plantToDelete)
                plantIdToDelete = null
            },
            onDismiss = { plantIdToDelete = null },
        )
    }
}

/**
 * The plant list, as a **stateless** composable: it receives the data to show
 * and reports user actions through callbacks, but stores nothing and doesn't
 * know about the ViewModel or the database. That's why it can be previewed with
 * fake data, and why the same screen works whoever owns the state.
 *
 * Kotlin notes:
 * - `onAddClick: () -> Unit` is a function-typed parameter: "a function taking
 *   nothing and returning nothing" (`Unit` is Kotlin's `void`).
 * - `onDeleteClick: (Plant) -> Unit` takes the plant the user tapped.
 * - `modifier: Modifier = Modifier` is the Compose convention: callers can
 *   adjust size/padding from outside; the default is "no changes".
 * - `@OptIn(ExperimentalMaterial3Api::class)`: Material 3's `TopAppBar` is still
 *   marked experimental (its API may change). Kotlin refuses to compile calls to
 *   such APIs unless you explicitly opt in, so the risk is a conscious choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantListScreen(
    plants: List<Plant>,
    onPlantClick: (Plant) -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Plant) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                // `actions`: icons at the right end of the top bar.
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.plants_add),
                )
            }
        },
    ) { innerPadding ->
        if (plants.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.plants_empty))
            }
        } else {
            // LazyColumn only composes the rows currently on screen (like
            // RecyclerView), so it stays fast however long the list gets.
            LazyColumn(
                // Keep the list clear of the top bar, and add extra space at the
                // bottom so the last row can scroll out from under the "+" button.
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                // `key`: lets Compose track rows by plant id, so deleting one row
                // doesn't make the following rows look like "changed" items.
                items(plants, key = { it.id }) { plant ->
                    PlantRow(
                        plant = plant,
                        onClick = { onPlantClick(plant) },
                        onDeleteClick = { onDeleteClick(plant) },
                    )
                }
            }
        }
    }
}

/**
 * One row: the plant's name and a trash icon. Tapping the row opens the plant.
 * `private` = visible only in this file.
 */
@Composable
private fun PlantRow(
    plant: Plant,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    ListItem(
        // `clickable` makes the whole row tappable (with the ripple effect).
        // The trash icon handles its own taps, so it doesn't open the plant.
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(plant.name) },
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    // Screen readers say "Delete Monstera" instead of just "Delete".
                    contentDescription = stringResource(R.string.plants_delete_description, plant.name),
                )
            }
        },
    )
}

// --- Previews (rendered by Android Studio; in VS Code they only need to compile) ---

private val samplePlants = listOf(
    Plant(id = 1, name = "Basil"),
    Plant(id = 2, name = "Fern"),
    Plant(id = 3, name = "Monstera"),
)

@Preview(showBackground = true)
@Composable
private fun PlantListScreenPreview() {
    PlantWatchTheme {
        // `{}` is an empty lambda: in a preview, taps do nothing.
        PlantListScreen(plants = samplePlants, onPlantClick = {}, onSettingsClick = {}, onAddClick = {}, onDeleteClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantListScreenEmptyPreview() {
    PlantWatchTheme {
        PlantListScreen(plants = emptyList(), onPlantClick = {}, onSettingsClick = {}, onAddClick = {}, onDeleteClick = {})
    }
}
