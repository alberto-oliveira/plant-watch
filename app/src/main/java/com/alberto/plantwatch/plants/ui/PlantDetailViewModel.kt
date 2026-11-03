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
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.plants.data.PlantDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

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
) : ViewModel() {

    // `toRoute<PlantDetail>()`: `<PlantDetail>` is a type argument, telling the
    // generic function which route class to rebuild.
    private val plantId: Long = savedStateHandle.toRoute<PlantDetail>().plantId

    /** The plant, or null while loading or if it no longer exists. */
    val plant: StateFlow<Plant?> = plantDao.observeById(plantId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PlantWatchApplication
                PlantDetailViewModel(
                    // Builds the SavedStateHandle for this ViewModel, filled with
                    // the navigation arguments.
                    savedStateHandle = createSavedStateHandle(),
                    plantDao = app.container.database.plantDao(),
                )
            }
        }
    }
}
