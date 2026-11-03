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
import com.alberto.plantwatch.plants.data.CareLog
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.CareTaskDao
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.plants.data.PlantDao
import com.alberto.plantwatch.plants.markedDone
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
    plantDao: PlantDao,
    private val careTaskDao: CareTaskDao,
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
                )
            }
        }
    }
}
