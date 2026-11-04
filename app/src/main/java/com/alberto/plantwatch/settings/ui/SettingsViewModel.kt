package com.alberto.plantwatch.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alberto.plantwatch.PlantWatchApplication
import com.alberto.plantwatch.reminders.scheduleDailyReminder
import com.alberto.plantwatch.settings.SettingsStore
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
) : ViewModel() {

    /** The stored reminder time; null until DataStore has been read (a few ms). */
    val reminderTime: StateFlow<LocalTime?> = settingsStore.reminderTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

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
                )
            }
        }
    }
}
