package com.alberto.plantwatch.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alberto.plantwatch.PlantWatchApplication
import com.alberto.plantwatch.reminders.scheduleDailyReminder
import com.alberto.plantwatch.plants.data.HomeRoom
import com.alberto.plantwatch.plants.data.RoomDao
import com.alberto.plantwatch.settings.Location
import com.alberto.plantwatch.settings.SettingsStore
import com.alberto.plantwatch.weather.fetchSunNow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * State and actions for the settings screen.
 *
 * Kotlin note: `reschedule: (LocalTime) -> Unit` is a *function-typed
 * dependency*. The ViewModel just calls "reschedule at this time" without
 * knowing about WorkManager or needing a Context; the [Factory] passes the real
 * implementation. (A test could pass a fake lambda instead.)
 */
class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val reschedule: (LocalTime) -> Unit,
    private val fetchSun: () -> Unit,
    private val roomDao: RoomDao,
) : ViewModel() {

    /** The stored reminder time; null until DataStore has been read (a few ms). */
    val reminderTime: StateFlow<LocalTime?> = settingsStore.reminderTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** The location for sunlight data; null if not set (or still loading). */
    val location: StateFlow<Location?> = settingsStore.location
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** Whether fertilizer reminders pause in winter (default on). */
    val pauseFertilizingInWinter: StateFlow<Boolean> = settingsStore.pauseFertilizingInWinter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = true)

    fun setPauseFertilizingInWinter(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setPauseFertilizingInWinter(enabled) }
    }

    /** Weather-aware watering: on/off, and the current factor (1.0 = no change). */
    val adjustWatering: StateFlow<Boolean> = settingsStore.adjustWateringToWeather
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)
    val wateringFactor: StateFlow<Double> = settingsStore.wateringFactor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = 1.0)

    fun setAdjustWatering(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAdjustWateringToWeather(enabled)
            // Fetch now so the factor is up to date right away.
            if (enabled) fetchSun()
        }
    }

    /** All rooms, alphabetically. */
    val rooms: StateFlow<List<HomeRoom>> = roomDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    /** Adds a room (id 0) or saves changes to an existing one. */
    fun saveRoom(room: HomeRoom) {
        if (room.name.isBlank()) return
        val cleaned = room.copy(name = room.name.trim())
        viewModelScope.launch { if (cleaned.id == 0L) roomDao.insert(cleaned) else roomDao.update(cleaned) }
    }

    /** Deletes a room; its plants keep existing, without a room. */
    fun deleteRoom(room: HomeRoom) {
        viewModelScope.launch { roomDao.delete(room) }
    }

    fun setLocation(location: Location) {
        viewModelScope.launch {
            settingsStore.setLocation(location)
            // Fetch for the new place right away instead of waiting for the daily run.
            fetchSun()
        }
    }

    /** Saves the new time, then moves the daily reminder job to it. */
    fun setReminderTime(time: LocalTime) {
        viewModelScope.launch {
            settingsStore.setReminderTime(time)
            reschedule(time)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PlantWatchApplication
                SettingsViewModel(
                    settingsStore = app.container.settingsStore,
                    reschedule = { time -> scheduleDailyReminder(app, time, replace = true) },
                    fetchSun = { fetchSunNow(app) },
                    roomDao = app.container.database.roomDao(),
                )
            }
        }
    }
}
