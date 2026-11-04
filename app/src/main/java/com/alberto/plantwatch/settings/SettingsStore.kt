package com.alberto.plantwatch.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alberto.plantwatch.reminders.DEFAULT_REMINDER_TIME
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

/**
 * The single DataStore file holding the app's settings ("settings.preferences_pb"
 * in the app's private storage).
 *
 * Kotlin notes: this is an *extension property*: it adds `dataStore` to every
 * `Context`, as if Context had declared it. `by preferencesDataStore(...)` is a
 * delegate that creates the DataStore once and returns the same instance after
 * (two instances on the same file would corrupt it). `private` keeps it to this file.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** A place on Earth in decimal degrees, used to fetch its sunshine data. */
data class Location(val latitude: Double, val longitude: Double)

/**
 * The app's settings, stored with DataStore (Preferences): a small key-value
 * store that, unlike the older SharedPreferences, is fully asynchronous. Reads
 * are Flows and writes are suspend functions, so it never blocks the UI thread.
 *
 * Created once, in AppContainer.
 */
class SettingsStore(context: Context) {

    private val dataStore = context.dataStore

    /**
     * The daily reminder time. Stored as minutes since midnight (09:00 = 540),
     * since DataStore keys hold simple types only. Emits again whenever it changes.
     *
     * `prefs[KEY]` is null when nothing was saved yet; `?.let { }` converts it
     * if present, and `?:` falls back to the default.
     */
    val reminderTime: Flow<LocalTime> = dataStore.data.map { prefs ->
        prefs[REMINDER_MINUTES]?.let { minutes -> LocalTime.of(minutes / 60, minutes % 60) }
            ?: DEFAULT_REMINDER_TIME
    }

    /** Saves the reminder time. `edit { }` applies the change atomically. */
    suspend fun setReminderTime(time: LocalTime) {
        dataStore.edit { prefs ->
            prefs[REMINDER_MINUTES] = time.hour * 60 + time.minute
        }
    }

    /**
     * The location for sunlight data, or null until the user enters one (sun
     * tracking does nothing without it). Both keys must be present.
     */
    val location: Flow<Location?> = dataStore.data.map { prefs ->
        val lat = prefs[LATITUDE]
        val lon = prefs[LONGITUDE]
        if (lat != null && lon != null) Location(lat, lon) else null
    }

    suspend fun setLocation(location: Location) {
        dataStore.edit { prefs ->
            prefs[LATITUDE] = location.latitude
            prefs[LONGITUDE] = location.longitude
        }
    }

    /** Pause fertilizer reminders in winter (on unless turned off). */
    val pauseFertilizingInWinter: Flow<Boolean> = dataStore.data.map { it[PAUSE_FERTILIZING] ?: true }

    suspend fun setPauseFertilizingInWinter(enabled: Boolean) {
        dataStore.edit { it[PAUSE_FERTILIZING] = enabled }
    }

    private companion object {
        // Typed keys: the compiler knows each setting's type.
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val PAUSE_FERTILIZING = booleanPreferencesKey("pause_fertilizing_in_winter")
    }
}
