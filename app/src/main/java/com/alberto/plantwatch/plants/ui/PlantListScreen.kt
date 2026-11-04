package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alberto.plantwatch.R
import com.alberto.plantwatch.plants.daysUntilDue
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.plants.data.TaskType
import com.alberto.plantwatch.ui.EmptyState
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import java.time.LocalDate
import kotlin.math.absoluteValue

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
        // Read on every recomposition, so "today" is current when the list updates.
        today = LocalDate.now(),
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
    val plantToDelete = plants.find { it.plant.id == plantIdToDelete }?.plant
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
 * The title bar is a *large* top app bar that shrinks into a normal one as the
 * list scrolls: the `scrollBehavior` tells the bar how far the list has
 * scrolled, and `Modifier.nestedScroll(...)` on the Scaffold forwards the
 * list's scroll events to it.
 *
 * Kotlin notes:
 * - `onAddClick: () -> Unit` is a function-typed parameter: "a function taking
 *   nothing and returning nothing" (`Unit` is Kotlin's `void`).
 * - `onDeleteClick: (Plant) -> Unit` takes the plant the user tapped.
 * - `modifier: Modifier = Modifier` is the Compose convention: callers can
 *   adjust size/padding from outside; the default is "no changes".
 * - `@OptIn(ExperimentalMaterial3Api::class)`: Material 3's top app bars are
 *   still marked experimental (their API may change). Kotlin refuses to compile
 *   calls to such APIs unless you explicitly opt in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantListScreen(
    plants: List<PlantSummary>,
    today: LocalDate,
    onPlantClick: (Plant) -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Plant) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
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
                scrollBehavior = scrollBehavior,
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
                EmptyState(
                    icon = R.drawable.ic_potted_plant,
                    title = stringResource(R.string.plants_empty_title),
                    message = stringResource(R.string.plants_empty_message),
                )
            }
        } else {
            // LazyColumn only composes the rows currently on screen (like
            // RecyclerView), so it stays fast however long the list gets.
            LazyColumn(
                // Keep the list clear of the top bar, add side margins around the
                // cards, and extra space at the bottom so the last card can scroll
                // out from under the "+" button.
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
                // Space between the cards.
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // `key`: lets Compose track rows by plant id, so deleting one row
                // doesn't make the following rows look like "changed" items.
                items(plants, key = { it.plant.id }) { summary ->
                    PlantCard(
                        summary = summary,
                        today = today,
                        onClick = { onPlantClick(summary.plant) },
                        onDeleteClick = { onDeleteClick(summary.plant) },
                    )
                }
            }
        }
    }
}

/**
 * One plant as a card:
 *   (B)  Basil                            🗑
 *        Water today · Fertilize in 12 days
 * Tapping the card opens the plant; the trash icon handles its own taps.
 */
@Composable
private fun PlantCard(
    summary: PlantSummary,
    today: LocalDate,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val plant = summary.plant
    // Any overdue task colours the next-care line in the error colour.
    val anyOverdue = summary.nextTasks.any { daysUntilDue(it, today) < 0 }
    val nextCare = if (summary.nextTasks.isEmpty()) {
        stringResource(R.string.plants_no_tasks)
    } else {
        // `map` turns each task into its phrase; `joinToString` glues them together.
        summary.nextTasks.map { nextCarePhrase(it, today) }.joinToString(" · ")
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PlantAvatar(plant.name)
            // `weight(1f)`: the text column takes all the width left over by the
            // avatar and the trash icon.
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = nextCare,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (anyOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    // Screen readers say "Delete Monstera" instead of just "Delete".
                    contentDescription = stringResource(R.string.plants_delete_description, plant.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A round avatar with the plant's first letter. Its colour is picked from the
 * name (`hashCode()`), so a plant always gets the same colour, and neighbours
 * usually differ.
 */
@Composable
private fun PlantAvatar(name: String) {
    val colors = MaterialTheme.colorScheme
    // Pairs of (background, text colour); `%` = remainder, `absoluteValue`
    // because hash codes can be negative.
    val palette = listOf(
        colors.primaryContainer to colors.onPrimaryContainer,
        colors.secondaryContainer to colors.onSecondaryContainer,
        colors.tertiaryContainer to colors.onTertiaryContainer,
    )
    val (background, foreground) = palette[name.hashCode().absoluteValue % palette.size]

    Surface(shape = CircleShape, color = background, modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                // `firstOrNull()?.uppercase()`: the first letter, if the name has one.
                text = name.trim().firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = foreground,
            )
        }
    }
}

/** "Water today", "Fertilize in 12 days", "Mist leaves overdue by 2 days". */
@Composable
private fun nextCarePhrase(task: CareTask, today: LocalDate): String {
    val name = taskName(task)
    val days = daysUntilDue(task, today).toInt()
    return when {
        days < 0 -> pluralStringResource(R.plurals.next_care_overdue, -days, name, -days)
        days == 0 -> stringResource(R.string.next_care_today, name)
        else -> pluralStringResource(R.plurals.next_care_in, days, name, days)
    }
}

// --- Previews (rendered by Android Studio; in VS Code they only need to compile) ---

private val previewToday = LocalDate.of(2026, 11, 4)

private val samplePlants = listOf(
    PlantSummary(
        Plant(id = 1, name = "Basil"),
        listOf(
            CareTask(id = 1, plantId = 1, type = TaskType.WATER, intervalDays = 3, nextDue = previewToday.minusDays(1)),
            CareTask(id = 2, plantId = 1, type = TaskType.FERTILIZE, intervalDays = 30, nextDue = previewToday.plusDays(12)),
        ),
    ),
    PlantSummary(
        Plant(id = 2, name = "Fern"),
        listOf(CareTask(id = 3, plantId = 2, type = TaskType.WATER, intervalDays = 7, nextDue = previewToday)),
    ),
    PlantSummary(Plant(id = 3, name = "Monstera"), emptyList()),
)

@Preview(showBackground = true)
@Composable
private fun PlantListScreenPreview() {
    PlantWatchTheme {
        // `{}` is an empty lambda: in a preview, taps do nothing.
        PlantListScreen(
            plants = samplePlants, today = previewToday,
            onPlantClick = {}, onSettingsClick = {}, onAddClick = {}, onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantListScreenDarkPreview() {
    PlantWatchTheme(darkTheme = true) {
        PlantListScreen(
            plants = samplePlants, today = previewToday,
            onPlantClick = {}, onSettingsClick = {}, onAddClick = {}, onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantListScreenEmptyPreview() {
    PlantWatchTheme {
        PlantListScreen(
            plants = emptyList(), today = previewToday,
            onPlantClick = {}, onSettingsClick = {}, onAddClick = {}, onDeleteClick = {},
        )
    }
}
