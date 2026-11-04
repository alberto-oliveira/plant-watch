package com.alberto.plantwatch.plants.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.alberto.plantwatch.PlantDetail
import com.alberto.plantwatch.PlantWatchApplication
import com.alberto.plantwatch.guide.CareGuide
import com.alberto.plantwatch.guide.CareGuideEntry
import com.alberto.plantwatch.guide.SpeciesMatch
import com.alberto.plantwatch.guide.searchSpecies
import com.alberto.plantwatch.guide.suggestedTasks
import com.alberto.plantwatch.plants.data.CareHistoryEntry
import com.alberto.plantwatch.plants.data.CareLog
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.CareTaskDao
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.plants.data.PlantDao
import com.alberto.plantwatch.plants.data.TaskType
import com.alberto.plantwatch.plants.markedDone
import com.alberto.plantwatch.plants.withNewInterval
import com.alberto.plantwatch.weather.SunSettings
import com.alberto.plantwatch.weather.WindowSettings
import com.alberto.plantwatch.weather.data.SunDao
import com.alberto.plantwatch.weather.fetchSunNow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * State and actions for one plant's detail screen.
 *
 * Which plant? Navigation stores the destination's arguments in the
 * ViewModel's [SavedStateHandle]; `toRoute<PlantDetail>()` turns them back into
 * the `PlantDetail(plantId = ...)` object we navigated with. The handle also
 * survives process death, so the screen comes back showing the right plant.
 */
class PlantDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val plantDao: PlantDao,
    private val careTaskDao: CareTaskDao,
    private val sunDao: SunDao,
    private val fetchSun: () -> Unit,
    private val careGuide: CareGuide,
) : ViewModel() {

    // `toRoute<PlantDetail>()`: `<PlantDetail>` is a type argument, telling the
    // generic function which route class to rebuild.
    private val plantId: Long = savedStateHandle.toRoute<PlantDetail>().plantId

    /** The plant, or null while loading or if it no longer exists. */
    val plant: StateFlow<Plant?> = plantDao.observeById(plantId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** The plant's care tasks, most urgent first. */
    val tasks: StateFlow<List<CareTask>> = careTaskDao.observeForPlant(plantId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    /** Recent care history (newest first), updated whenever a task is done or deleted. */
    val history: StateFlow<List<CareHistoryEntry>> = careTaskDao.observeHistoryForPlant(plantId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    /**
     * The care-guide entry of the plant's linked species: null if not linked, or
     * if the id isn't in the guide (the card then shows "unknown species").
     * `map` transforms each value the Flow emits.
     */
    val guideEntry: StateFlow<CareGuideEntry?> = plant
        .map { it?.speciesId?.let(careGuide::byId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /**
     * Tasks the guide suggests that the plant doesn't have yet (empty when not
     * linked, or nothing is missing). Recomputed when the species or the tasks change.
     */
    val suggestedTasks: StateFlow<List<CareTask>> = combine(guideEntry, tasks) { entry, existing ->
        if (entry == null) emptyList() else suggestedTasks(entry, plantId, existing, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    /** Adds the suggested tasks (each due today). */
    fun addSuggestedTasks() {
        val toAdd = suggestedTasks.value
        viewModelScope.launch {
            toAdd.forEach { careTaskDao.insert(it) }
        }
    }

    /** Species matching [query], best first (the guide is small, so this is instant). */
    fun searchSpecies(query: String): List<SpeciesMatch> = searchSpecies(careGuide.entries, query)

    /** Links the plant to a guide species, or unlinks it with null. */
    fun setSpecies(speciesId: String?) {
        val current = plant.value ?: return
        viewModelScope.launch {
            plantDao.update(current.copy(speciesId = speciesId))
        }
    }

    // The day the screen was opened; "yesterday" and "last 7 days" count from it.
    private val today = LocalDate.now()

    /** Minutes of sun yesterday; null if not fetched (or not tracked). */
    val sunYesterday: StateFlow<Double?> = sunDao.observeDay(plantId, today.minusDays(1))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** Total minutes of sun over the 7 days before today (a SUM query, not stored). */
    val sunLastWeek: StateFlow<Double?> = sunDao.observeTotal(plantId, today.minusDays(7), today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** Window mode: sun estimated from the window's direction and the plant's position. */
    fun setSunWindow(window: WindowSettings) = updateSun { plant ->
        plant.copy(
            windowDirection = window.direction,
            windowDistance = window.distance,
            windowPlacement = window.placement,
        )
    }

    /** Custom-hours mode: a fixed hour window and exposure (e.g. outdoor plants). */
    fun setSunHours(settings: SunSettings) = updateSun { plant ->
        plant.copy(
            sunStartHour = settings.startHour,
            sunEndHour = settings.endHour,
            sunExposurePercent = settings.exposurePercent,
            // Clear window mode, which would otherwise take precedence.
            windowDirection = null,
            windowDistance = null,
            windowPlacement = null,
        )
    }

    /** No sun tracking. */
    fun turnOffSun() = updateSun { plant ->
        plant.copy(
            sunStartHour = null, sunEndHour = null, sunExposurePercent = null,
            windowDirection = null, windowDistance = null, windowPlacement = null,
        )
    }

    /**
     * Saves the plant as changed by [change], clears its old sun rows, then
     * fetches right away if it's tracked, so the figures reflect the new settings.
     *
     * Kotlin note: `change: (Plant) -> Plant` takes a function, which the three
     * methods above pass as trailing lambdas: "how to change the plant".
     */
    private fun updateSun(change: (Plant) -> Plant) {
        val updated = change(plant.value ?: return)
        viewModelScope.launch {
            plantDao.update(updated)
            // Old rows were computed with the old settings: drop them; the fetch
            // below recomputes the last 7 days with the new ones.
            sunDao.deleteForPlant(updated.id)
            if (updated.isSunTracked) fetchSun()
        }
    }

    /**
     * Adds a task to this plant. It's due today: marking it done starts the
     * cycle. The label is only kept for OTHER tasks.
     */
    fun addTask(type: TaskType, label: String, intervalDays: Int) {
        val task = CareTask(
            plantId = plantId,
            type = type,
            customLabel = if (type == TaskType.OTHER) label.trim() else null,
            intervalDays = intervalDays,
            nextDue = LocalDate.now(),
        )
        viewModelScope.launch {
            careTaskDao.insert(task)
        }
    }

    /**
     * Saves changes to a task. A new interval recalculates the due date from the
     * last time it was done ([withNewInterval], unit-tested). The history is
     * kept, even if the type changes.
     */
    fun editTask(task: CareTask, type: TaskType, label: String, intervalDays: Int) {
        val updated = withNewInterval(task, intervalDays).copy(
            type = type,
            customLabel = if (type == TaskType.OTHER) label.trim() else null,
        )
        viewModelScope.launch {
            careTaskDao.update(updated)
        }
    }

    /**
     * Marks the task done today: the scheduling rule ([markedDone], unit-tested)
     * computes the new dates, and the DAO saves the task and the log entry in
     * one transaction.
     */
    fun markDone(task: CareTask) {
        val today = LocalDate.now()
        viewModelScope.launch {
            careTaskDao.markDone(
                updatedTask = markedDone(task, today),
                log = CareLog(taskId = task.id, doneOn = today),
            )
        }
    }

    /** Deletes the task; its log entries go with it (CASCADE). */
    fun deleteTask(task: CareTask) {
        viewModelScope.launch {
            careTaskDao.delete(task)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PlantWatchApplication
                PlantDetailViewModel(
                    // Builds the SavedStateHandle for this ViewModel, filled with
                    // the navigation arguments.
                    savedStateHandle = createSavedStateHandle(),
                    plantDao = app.container.database.plantDao(),
                    careTaskDao = app.container.database.careTaskDao(),
                    sunDao = app.container.database.sunDao(),
                    fetchSun = { fetchSunNow(app) },
                    careGuide = app.container.careGuide,
                )
            }
        }
    }
}
