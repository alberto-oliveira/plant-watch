package com.alberto.plantwatch.plants.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alberto.plantwatch.PlantWatchApplication
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.plants.data.PlantDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State and actions for the plant list screen.
 *
 * A ViewModel survives configuration changes (e.g. rotating the phone), while
 * the Activity and the composables are recreated. So the data lives here and
 * the UI only displays it and reports events (add, delete) back.
 *
 * The ViewModel gets its [PlantDao] through the constructor instead of creating
 * it, which keeps it independent of how the database is built (see [Factory]).
 */
class PlantListViewModel(private val plantDao: PlantDao) : ViewModel() {

    /**
     * All plants, always up to date.
     *
     * - `plantDao.observeAll()` is a cold `Flow`: it only runs while someone
     *   collects it, and every collector would run its own query.
     * - `stateIn(...)` turns it into a `StateFlow`: one shared collection that
     *   always holds the latest value (starting with `emptyList()`), which is
     *   what the UI reads.
     * - `WhileSubscribed(5_000)`: stop listening to the database 5 s after the
     *   screen stops observing (e.g. app in the background), but not during a
     *   quick rotation, which would otherwise restart the query for nothing.
     *   (`5_000`: underscores in number literals are just for readability.)
     */
    val plants: StateFlow<List<Plant>> = plantDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Adds a plant. Surrounding spaces are removed; a blank name is ignored. */
    fun addPlant(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        // `viewModelScope.launch { }` starts a coroutine: a lightweight task
        // that can call `suspend` functions such as `insert`. It's cancelled
        // automatically when the ViewModel is destroyed.
        viewModelScope.launch {
            plantDao.insert(Plant(name = trimmed)) // named argument; `id` keeps its default 0
        }
    }

    /** Deletes the plant; its tasks and history go with it (database CASCADE). */
    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            plantDao.delete(plant)
        }
    }

    /**
     * `companion object` = members that belong to the class itself, not to an
     * instance (similar to Java's `static`). Used as `PlantListViewModel.Factory`.
     */
    companion object {
        /**
         * Tells Android how to build this ViewModel, since its constructor needs a
         * [PlantDao]. This is the manual-DI bridge: it fetches the DAO from the
         * [com.alberto.plantwatch.AppContainer] held by the Application.
         * `this[APPLICATION_KEY]` looks up the Application object.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PlantWatchApplication
                PlantListViewModel(app.container.database.plantDao())
            }
        }
    }
}
