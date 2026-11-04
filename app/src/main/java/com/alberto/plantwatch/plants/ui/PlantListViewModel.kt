package com.alberto.plantwatch.plants.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alberto.plantwatch.PlantWatchApplication
import com.alberto.plantwatch.guide.CareGuide
import com.alberto.plantwatch.guide.SpeciesMatch
import com.alberto.plantwatch.guide.searchSpecies
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.CareTaskDao
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.plants.data.PlantDao
import com.alberto.plantwatch.plants.data.TaskType
import com.alberto.plantwatch.plants.fertilizingPausedUntil
import com.alberto.plantwatch.plants.PhotoStore
import com.alberto.plantwatch.plants.mostUrgentTasks
import com.alberto.plantwatch.settings.SettingsStore
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One plant in the list, with its most urgent tasks for the next-care line and
 * the scientific name of its linked species (null if none).
 */
data class PlantSummary(
    val plant: Plant,
    val nextTasks: List<CareTask>,
    val scientificName: String? = null,
    /** The plant's photo file, shown as its avatar; null if none. */
    val photo: File? = null,
)

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
class PlantListViewModel(
    private val plantDao: PlantDao,
    careTaskDao: CareTaskDao,
    private val careGuide: CareGuide,
    settingsStore: SettingsStore,
    private val photoStore: PhotoStore,
) : ViewModel() {

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
    // `combine` merges two Flows: whenever either the plants or the tasks change,
    // the lambda runs again with the latest value of both. Here it pairs each
    // plant with its most urgent tasks (`groupBy` = tasks bucketed by plant id;
    // `orEmpty()` = an empty list for a plant without tasks).
    val plants: StateFlow<List<PlantSummary>> = combine(
        plantDao.observeAll(),
        careTaskDao.observeAll(),
        settingsStore.location,
        settingsStore.pauseFertilizingInWinter,
    ) { plants, tasks, location, pauseEnabled ->
        // Fertilizer tasks paused for winter aren't the plant's next care.
        val paused = fertilizingPausedUntil(LocalDate.now(), location?.latitude, pauseEnabled) != null
        val tasksByPlant = tasks.filterNot { paused && it.type == TaskType.FERTILIZE }.groupBy { it.plantId }
        plants.map { plant ->
            PlantSummary(
                plant = plant,
                nextTasks = mostUrgentTasks(tasksByPlant[plant.id].orEmpty()),
                scientificName = plant.speciesId?.let(careGuide::byId)?.scientificName,
                photo = plant.photoFile?.let(photoStore::fileFor),
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Adds a plant. Surrounding spaces are removed; a blank name is ignored. */
    fun addPlant(name: String, speciesId: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        // `viewModelScope.launch { }` starts a coroutine: a lightweight task
        // that can call `suspend` functions such as `insert`. It's cancelled
        // automatically when the ViewModel is destroyed.
        viewModelScope.launch {
            plantDao.insert(Plant(name = trimmed, speciesId = speciesId)) // named arguments; `id` keeps its default 0
        }
    }

    /**
     * Up to 3 species matching a name being typed, for the add dialog's
     * suggestions. Nothing for very short text, to avoid noisy matches.
     */
    fun speciesSuggestions(name: String): List<SpeciesMatch> =
        if (name.trim().length < 3) emptyList() else searchSpecies(careGuide.entries, name, limit = 3)

    /** Deletes the plant; its tasks and history go with it (database CASCADE). */
    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            plantDao.delete(plant)
            // The photo file isn't in the database, so it's removed separately.
            withContext(Dispatchers.IO) { photoStore.delete(plant.photoFile) }
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
                PlantListViewModel(
                    plantDao = app.container.database.plantDao(),
                    careTaskDao = app.container.database.careTaskDao(),
                    careGuide = app.container.careGuide,
                    settingsStore = app.container.settingsStore,
                    photoStore = app.container.photoStore,
                )
            }
        }
    }
}
