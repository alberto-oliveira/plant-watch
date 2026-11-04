package com.alberto.plantwatch.plants.ui

import androidx.compose.foundation.clickable
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
import com.alberto.plantwatch.plants.data.CareHistoryEntry
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.TaskType
import com.alberto.plantwatch.ui.theme.PlantWatchTheme
import com.alberto.plantwatch.weather.SunSettings
import com.alberto.plantwatch.weather.parseSunSettings
import kotlin.math.roundToInt
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
    val history by viewModel.history.collectAsStateWithLifecycle()
    val sunYesterday by viewModel.sunYesterday.collectAsStateWithLifecycle()
    val sunLastWeek by viewModel.sunLastWeek.collectAsStateWithLifecycle()

    // The plant's current sun settings, or null when tracking is off.
    val sunSettings = plant?.let { p ->
        val start = p.sunStartHour
        val end = p.sunEndHour
        val exposure = p.sunExposurePercent
        if (start != null && end != null && exposure != null) SunSettings(start, end, exposure) else null
    }

    // Sun dialog: open or not, and its three texts.
    var showSunDialog by rememberSaveable { mutableStateOf(false) }
    var sunExposureText by rememberSaveable { mutableStateOf("") }
    var sunStartText by rememberSaveable { mutableStateOf("") }
    var sunEndText by rememberSaveable { mutableStateOf("") }

    // Which task is waiting for delete confirmation (null = none); same pattern
    // as the plant list.
    var taskIdToDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    // Task dialog, shared by "add" and "edit": open or not, which task is being
    // edited (null = adding a new one), and the "draft" values in its fields.
    // All of it survives rotation. (An enum can be saved because Kotlin enums
    // are Serializable in the Java sense.)
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var draftType by rememberSaveable { mutableStateOf(TaskType.WATER) }
    var draftLabel by rememberSaveable { mutableStateOf("") }
    var draftInterval by rememberSaveable { mutableStateOf("7") }

    // Local functions: they can read and change the state variables above.
    fun openAddDialog() {
        editingTaskId = null
        draftType = TaskType.WATER
        draftLabel = ""
        draftInterval = "7"
        showTaskDialog = true
    }

    fun openEditDialog(task: CareTask) {
        editingTaskId = task.id
        draftType = task.type
        draftLabel = task.customLabel ?: ""
        draftInterval = task.intervalDays.toString()
        showTaskDialog = true
    }

    fun openSunDialog() {
        // Prefill with the current settings, or sensible defaults when off.
        sunExposureText = (sunSettings?.exposurePercent ?: 100).toString()
        sunStartText = (sunSettings?.startHour ?: 8).toString()
        sunEndText = (sunSettings?.endHour ?: 18).toString()
        showSunDialog = true
    }

    PlantDetailScreen(
        // `?.name ?: ""`: the name if the plant is loaded, empty text otherwise.
        plantName = plant?.name ?: "",
        sunSettings = sunSettings,
        sunYesterday = sunYesterday,
        sunLastWeek = sunLastWeek,
        onSunClick = ::openSunDialog,
        tasks = tasks,
        history = history,
        // Read on every recomposition, e.g. after marking a task done.
        today = LocalDate.now(),
        onBack = onBack,
        onAddClick = ::openAddDialog,
        onTaskClick = ::openEditDialog,
        onDoneClick = viewModel::markDone,
        onDeleteClick = { task -> taskIdToDelete = task.id },
    )

    if (showTaskDialog) {
        val isEditing = editingTaskId != null
        TaskDialog(
            title = stringResource(if (isEditing) R.string.edit_task_title else R.string.add_task_title),
            confirmText = stringResource(if (isEditing) R.string.save else R.string.add_plant_confirm),
            type = draftType,
            onTypeChange = { draftType = it },
            label = draftLabel,
            onLabelChange = { draftLabel = it },
            intervalText = draftInterval,
            onIntervalTextChange = { draftInterval = it },
            onConfirm = {
                // The dialog only enables confirm for a valid interval, but the
                // type system doesn't know that: parseIntervalDays returns Int?.
                // `?.let { days -> ... }` runs only for a non-null value. (The
                // alternative `!!` asserts non-null and crashes if wrong.)
                parseIntervalDays(draftInterval)?.let { days ->
                    val editing = tasks.find { it.id == editingTaskId }
                    if (editing == null) {
                        viewModel.addTask(draftType, draftLabel, days)
                    } else {
                        viewModel.editTask(editing, draftType, draftLabel, days)
                    }
                }
                showTaskDialog = false
            },
            onDismiss = { showTaskDialog = false },
        )
    }

    if (showSunDialog) {
        SunDialog(
            exposure = sunExposureText,
            onExposureChange = { sunExposureText = it },
            startHour = sunStartText,
            onStartHourChange = { sunStartText = it },
            endHour = sunEndText,
            onEndHourChange = { sunEndText = it },
            isTracked = sunSettings != null,
            onConfirm = {
                parseSunSettings(sunStartText, sunEndText, sunExposureText)?.let(viewModel::setSunSettings)
                showSunDialog = false
            },
            onTurnOff = {
                viewModel.setSunSettings(null)
                showSunDialog = false
            },
            onDismiss = { showSunDialog = false },
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
 * One plant's screen: top bar with a back arrow and the plant's name, a Sun row,
 * the list of its care tasks (or a message when there are none), and below them
 * the recent care history.
 *
 * Tapping a task opens it for editing.
 *
 * Kotlin note: `onDoneClick = viewModel::markDone` (in the route above) is a
 * *function reference*: it passes the ViewModel's method itself as the
 * `(CareTask) -> Unit` callback, short for `{ task -> viewModel.markDone(task) }`.
 * `::openEditDialog` does the same with a local function.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantName: String,
    sunSettings: SunSettings?,
    sunYesterday: Double?,
    sunLastWeek: Double?,
    onSunClick: () -> Unit,
    tasks: List<CareTask>,
    history: List<CareHistoryEntry>,
    today: LocalDate,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onTaskClick: (CareTask) -> Unit,
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
        // Extra bottom space so the last item can scroll out from under the "+".
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 88.dp,
            ),
        ) {
            item(key = "sun") {
                SunRow(sunSettings, sunYesterday, sunLastWeek, onClick = onSunClick)
            }

            if (tasks.isEmpty()) {
                item(key = "tasks-empty") {
                    Text(
                        text = stringResource(R.string.tasks_empty),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
                    )
                }
            } else {
                // Keys must be unique across the whole list; tasks and log entries
                // both have numeric ids, so each gets a prefix.
                items(tasks, key = { "task-${it.id}" }) { task ->
                    TaskRow(
                        task = task,
                        today = today,
                        onClick = { onTaskClick(task) },
                        onDoneClick = { onDoneClick(task) },
                        onDeleteClick = { onDeleteClick(task) },
                    )
                }

                if (history.isNotEmpty()) {
                    // `item { }` adds a single element (here, the section title).
                    item(key = "history-title") {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
                        )
                    }
                    items(history, key = { "log-${it.logId}" }) { entry ->
                        HistoryRow(entry)
                    }
                }
            }
        }
    }
}

/**
 * The plant's sunlight, e.g.
 *   Sun
 *   Yesterday 2 h 15 min · Last 7 days 14 h 30 min
 *   08–18 h at 40%
 * or "Tracking off · Tap to set up". Tapping it opens the sun dialog.
 */
@Composable
private fun SunRow(
    settings: SunSettings?,
    yesterday: Double?,
    lastWeek: Double?,
    onClick: () -> Unit,
) {
    val text = if (settings == null) {
        stringResource(R.string.sun_off)
    } else {
        val window = stringResource(R.string.sun_window, settings.startHour, settings.endHour, settings.exposurePercent)
        val figures = if (yesterday == null && lastWeek == null) {
            stringResource(R.string.sun_no_data)
        } else {
            stringResource(
                R.string.sun_summary,
                formatSunMinutes(yesterday ?: 0.0),
                formatSunMinutes(lastWeek ?: 0.0),
            )
        }
        "$figures\n$window"
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(stringResource(R.string.sun_title)) },
        supportingContent = { Text(text) },
    )
}

/**
 * Minutes as "2 h 15 min", or "45 min" under an hour.
 * `roundToInt()` rounds to the nearest whole minute; `/` and `%` on Ints are
 * integer division and remainder.
 */
@Composable
private fun formatSunMinutes(minutes: Double): String {
    val total = minutes.roundToInt()
    return if (total >= 60) {
        stringResource(R.string.sun_duration_hours_minutes, total / 60, total % 60)
    } else {
        stringResource(R.string.sun_duration_minutes, total)
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
    onClick: () -> Unit,
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
        // Tapping the row (outside the buttons) edits the task.
        modifier = Modifier.clickable(onClick = onClick),
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

/** One history line: the date, and what was done ("Water", "Mist leaves", ...). */
@Composable
private fun HistoryRow(entry: CareHistoryEntry) {
    ListItem(
        headlineContent = { Text(taskName(entry.type, entry.customLabel)) },
        supportingContent = { Text(formatDate(entry.doneOn)) },
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
fun taskName(task: CareTask): String = taskName(task.type, task.customLabel)

/**
 * Same, from the type and label alone (used by history entries). Two functions
 * with the same name but different parameters is *overloading*: Kotlin picks
 * the one matching the arguments.
 */
@Composable
fun taskName(type: TaskType, customLabel: String?): String = when (type) {
    TaskType.OTHER -> customLabel ?: taskTypeName(TaskType.OTHER)
    else -> taskTypeName(type)
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

private val sampleHistory = listOf(
    CareHistoryEntry(logId = 2, doneOn = previewToday, type = TaskType.OTHER, customLabel = "Mist leaves"),
    CareHistoryEntry(logId = 1, doneOn = previewToday.minusDays(9), type = TaskType.WATER, customLabel = null),
)

@Preview(showBackground = true)
@Composable
private fun PlantDetailScreenPreview() {
    PlantWatchTheme {
        PlantDetailScreen(
            plantName = "Monstera", sunSettings = SunSettings(8, 18, 40), sunYesterday = 135.0, sunLastWeek = 870.0,
            onSunClick = {}, tasks = sampleTasks, history = sampleHistory, today = previewToday,
            onBack = {}, onAddClick = {}, onTaskClick = {}, onDoneClick = {}, onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantDetailScreenEmptyPreview() {
    PlantWatchTheme {
        PlantDetailScreen(
            plantName = "Monstera", sunSettings = null, sunYesterday = null, sunLastWeek = null,
            onSunClick = {}, tasks = emptyList(), history = emptyList(), today = previewToday,
            onBack = {}, onAddClick = {}, onTaskClick = {}, onDoneClick = {}, onDeleteClick = {},
        )
    }
}
