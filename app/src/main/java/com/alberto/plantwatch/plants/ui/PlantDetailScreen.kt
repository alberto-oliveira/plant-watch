package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alberto.plantwatch.R
import com.alberto.plantwatch.plants.daysUntilDue
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.TaskType
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Connects [PlantDetailViewModel] to the stateless [PlantDetailScreen] and its dialogs. */
@Composable
fun PlantDetailRoute(
    onBack: () -> Unit,
    viewModel: PlantDetailViewModel = viewModel(factory = PlantDetailViewModel.Factory),
) {
    val plant by viewModel.plant.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    // Which task is waiting for delete confirmation (null = none); same pattern
    // as the plant list.
    var taskIdToDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    // Add-task dialog: open or not, and what has been entered so far. All three
    // values survive rotation. (An enum can be saved because Kotlin enums are
    // Serializable in the Java sense.)
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var newType by rememberSaveable { mutableStateOf(TaskType.WATER) }
    var newLabel by rememberSaveable { mutableStateOf("") }
    var newInterval by rememberSaveable { mutableStateOf("7") }

    PlantDetailScreen(
        // `?.name ?: ""`: the name if the plant is loaded, empty text otherwise.
        plantName = plant?.name ?: "",
        tasks = tasks,
        // Read on every recomposition, e.g. after marking a task done.
        today = LocalDate.now(),
        onBack = onBack,
        onAddClick = { showAddDialog = true },
        onDoneClick = viewModel::markDone,
        onDeleteClick = { task -> taskIdToDelete = task.id },
    )

    if (showAddDialog) {
        fun close() {
            showAddDialog = false
            // Reset for the next time the dialog opens.
            newType = TaskType.WATER
            newLabel = ""
            newInterval = "7"
        }
        TaskDialog(
            title = stringResource(R.string.add_task_title),
            confirmText = stringResource(R.string.add_plant_confirm),
            type = newType,
            onTypeChange = { newType = it },
            label = newLabel,
            onLabelChange = { newLabel = it },
            intervalText = newInterval,
            onIntervalTextChange = { newInterval = it },
            onConfirm = {
                // The dialog only enables confirm for a valid interval, but the
                // type system doesn't know that: parseIntervalDays returns Int?.
                // `?.let { days -> ... }` runs only for a non-null value. (The
                // alternative `!!` asserts non-null and crashes if wrong.)
                parseIntervalDays(newInterval)?.let { days ->
                    viewModel.addTask(newType, newLabel, days)
                }
                close()
            },
            onDismiss = { close() },
        )
    }

    val taskToDelete = tasks.find { it.id == taskIdToDelete }
    if (taskToDelete != null) {
        DeleteTaskDialog(
            taskName = taskName(taskToDelete),
            onConfirm = {
                viewModel.deleteTask(taskToDelete)
                taskIdToDelete = null
            },
            onDismiss = { taskIdToDelete = null },
        )
    }
}

/**
 * One plant's screen: top bar with a back arrow and the plant's name, and the
 * list of its care tasks (or a message when there are none).
 *
 * Kotlin note: `onDoneClick = viewModel::markDone` (in the route above) is a
 * *function reference*: it passes the ViewModel's method itself as the
 * `(CareTask) -> Unit` callback, short for `{ task -> viewModel.markDone(task) }`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantName: String,
    tasks: List<CareTask>,
    today: LocalDate,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onDoneClick: (CareTask) -> Unit,
    onDeleteClick: (CareTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(plantName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.tasks_add),
                )
            }
        },
    ) { innerPadding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.tasks_empty))
            }
        } else {
            // Extra bottom space so the last task can scroll out from under the "+".
            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        today = today,
                        onDoneClick = { onDoneClick(task) },
                        onDeleteClick = { onDeleteClick(task) },
                    )
                }
            }
        }
    }
}

/**
 * One task:
 *   Water                                   [Done] 🗑
 *   Every 7 days · Due in 3 days
 *   Last done 3 Nov 2026
 */
@Composable
private fun TaskRow(
    task: CareTask,
    today: LocalDate,
    onDoneClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val name = taskName(task)
    val days = daysUntilDue(task, today)

    // `when` is Kotlin's (much more capable) switch, and an expression: each
    // branch yields a value. Branch conditions can be any boolean expression.
    val dueText = when {
        days < 0 -> pluralStringResource(R.plurals.task_overdue, (-days).toInt(), -days)
        days == 0L -> stringResource(R.string.task_due_today)
        else -> pluralStringResource(R.plurals.task_due_in, days.toInt(), days)
    }
    val intervalText = pluralStringResource(R.plurals.task_interval, task.intervalDays, task.intervalDays)
    val lastDoneText = task.lastDone?.let { stringResource(R.string.task_last_done, formatDate(it)) }
        ?: stringResource(R.string.task_never_done)

    ListItem(
        headlineContent = { Text(name) },
        supportingContent = {
            Text(
                text = "$intervalText · $dueText\n$lastDoneText",
                // Overdue tasks get the theme's error color to stand out.
                color = if (days < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Disabled once done today, so a double tap can't log it twice.
                FilledTonalButton(onClick = onDoneClick, enabled = task.lastDone != today) {
                    Text(stringResource(R.string.task_done))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.task_delete_description, name),
                    )
                }
            }
        },
    )
}

/**
 * The name shown for a task: the type's name, or the custom label for OTHER.
 * Kotlin notes: `when (x)` with a subject checks it against each value. In
 * [taskTypeName] all enum values are listed, so the compiler knows the `when`
 * is complete (no `else` needed, and adding a new TaskType fails the build
 * until it's handled there).
 */
@Composable
fun taskName(task: CareTask): String = when (task.type) {
    TaskType.OTHER -> task.customLabel ?: taskTypeName(TaskType.OTHER)
    else -> taskTypeName(task.type)
}

/** The display name of a task type ("Water", "Fertilize", "Other"). */
@Composable
fun taskTypeName(type: TaskType): String = when (type) {
    TaskType.WATER -> stringResource(R.string.task_type_water)
    TaskType.FERTILIZE -> stringResource(R.string.task_type_fertilize)
    TaskType.OTHER -> stringResource(R.string.task_type_other)
}

/** A date in the phone's language and format, e.g. "3 Nov 2026" or "3 de nov. de 2026". */
private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

// --- Previews ---

private val previewToday = LocalDate.of(2026, 11, 3)

private val sampleTasks = listOf(
    CareTask(id = 1, plantId = 1, type = TaskType.WATER, intervalDays = 7,
        lastDone = previewToday.minusDays(9), nextDue = previewToday.minusDays(2)),
    CareTask(id = 2, plantId = 1, type = TaskType.OTHER, customLabel = "Mist leaves", intervalDays = 1,
        lastDone = previewToday, nextDue = previewToday.plusDays(1)),
    CareTask(id = 3, plantId = 1, type = TaskType.FERTILIZE, intervalDays = 30,
        nextDue = previewToday),
)

@Preview(showBackground = true)
@Composable
private fun PlantDetailScreenPreview() {
    PlantWatchTheme {
        PlantDetailScreen(
            plantName = "Monstera", tasks = sampleTasks, today = previewToday,
            onBack = {}, onAddClick = {}, onDoneClick = {}, onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantDetailScreenEmptyPreview() {
    PlantWatchTheme {
        PlantDetailScreen(
            plantName = "Monstera", tasks = emptyList(), today = previewToday,
            onBack = {}, onAddClick = {}, onDoneClick = {}, onDeleteClick = {},
        )
    }
}
