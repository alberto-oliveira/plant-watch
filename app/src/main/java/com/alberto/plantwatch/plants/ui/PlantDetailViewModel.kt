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
import com.alberto.plantwatch.weather.data.SunDao
import com.alberto.plantwatch.weather.fetchSunNow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    sunDao: SunDao,
    private val fetchSun: () -> Unit,
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

    // The day the screen was opened; "yesterday" and "last 7 days" count from it.
    private val today = LocalDate.now()

    /** Minutes of sun yesterday; null if not fetched (or not tracked). */
    val sunYesterday: StateFlow<Double?> = sunDao.observeDay(plantId, today.minusDays(1))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** Total minutes of sun over the 7 days before today (a SUM query, not stored). */
    val sunLastWeek: StateFlow<Double?> = sunDao.observeTotal(plantId, today.minusDays(7), today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /**
     * Turns sun tracking on (or changes it) with [settings], or off with null.
     * After a change, fetches right away so the figures reflect it.
     */
    fun setSunSettings(settings: SunSettings?) {
        val current = plant.value ?: return
        val updated = current.copy(
            sunStartHour = settings?.startHour,
            sunEndHour = settings?.endHour,
            sunExposurePercent = settings?.exposurePercent,
        )
        viewModelScope.launch {
            plantDao.update(updated)
            if (settings != null) fetchSun()
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
                )
            }
        }
    }
}
