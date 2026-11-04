package com.alberto.plantwatch.settings.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alberto.plantwatch.PlantWatchApplication
import com.alberto.plantwatch.backup.Backup
import com.alberto.plantwatch.backup.BackupContents
import com.alberto.plantwatch.backup.BackupFormatException
import com.alberto.plantwatch.reminders.scheduleDailyReminder
import com.alberto.plantwatch.plants.data.HomeRoom
import com.alberto.plantwatch.plants.data.RoomDao
import com.alberto.plantwatch.settings.Location
import com.alberto.plantwatch.settings.SettingsStore
import com.alberto.plantwatch.weather.fetchSunNow
import com.alberto.plantwatch.widget.refreshWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val backup: Backup,
    private val contentResolver: ContentResolver,
    private val refreshWidget: suspend () -> Unit,
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

    /** Where an export or import stands; null when there's nothing to show. */
    private val _backupStatus = MutableStateFlow<BackupStatus?>(null)
    val backupStatus: StateFlow<BackupStatus?> = _backupStatus.asStateFlow()

    /**
     * Writes a backup to the file the user just created ([uri], from the
     * system's "save as" screen). `openOutputStream` gives write access to it.
     */
    fun exportTo(uri: Uri) {
        runBackupStep {
            val out = contentResolver.openOutputStream(uri) ?: error("Can't open $uri")
            out.use { backup.export(it) }
            BackupStatus.Exported
        }
    }

    /** Reads the picked backup file and asks for confirmation before importing it. */
    fun readImport(uri: Uri) {
        runBackupStep {
            val input = contentResolver.openInputStream(uri) ?: error("Can't open $uri")
            BackupStatus.ReadyToImport(input.use { backup.read(it) })
        }
    }

    /** Replaces all data with the backup being confirmed, then reschedules the jobs. */
    fun confirmImport() {
        val ready = _backupStatus.value as? BackupStatus.ReadyToImport ?: return
        runBackupStep {
            val reminder = backup.import(ready.contents)
            reschedule(reminder)
            fetchSun()
            refreshWidget()
            BackupStatus.Imported
        }
    }

    fun dismissBackupStatus() {
        _backupStatus.value = null
    }

    /**
     * Shows "Working…", runs [step] and shows its result, or an error.
     * `suspend () -> BackupStatus` is a function type for a suspending lambda.
     */
    private fun runBackupStep(step: suspend () -> BackupStatus) {
        _backupStatus.value = BackupStatus.Working
        viewModelScope.launch {
            _backupStatus.value = try {
                step()
            } catch (e: BackupFormatException) {
                BackupStatus.Refused
            } catch (e: Exception) {
                BackupStatus.Failed
            }
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
                    backup = app.container.backup,
                    contentResolver = app.contentResolver,
                    refreshWidget = { refreshWidget(app) },
                )
            }
        }
    }
}

/**
 * The backup dialog's states.
 *
 * Kotlin note: a `sealed interface` has a fixed set of implementations, all in
 * this module, so a `when` over it can list every case and needs no `else`.
 * `data object` is a singleton with a readable toString().
 */
sealed interface BackupStatus {
    data object Working : BackupStatus
    data object Exported : BackupStatus
    class ReadyToImport(val contents: BackupContents) : BackupStatus
    data object Imported : BackupStatus
    /** Not a backup, or from a newer app version. */
    data object Refused : BackupStatus
    data object Failed : BackupStatus
}
