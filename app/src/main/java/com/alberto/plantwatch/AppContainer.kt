package com.alberto.plantwatch

import android.content.Context
import androidx.room.Room
import com.alberto.plantwatch.data.AppDatabase
import com.alberto.plantwatch.guide.CareGuide
import com.alberto.plantwatch.plants.PhotoStore
import com.alberto.plantwatch.settings.SettingsStore
import com.alberto.plantwatch.weather.OpenMeteoApi

/**
 * Manual dependency injection: the one place that creates the app's shared
 * objects (the database, the settings, the Open-Meteo client and the care guide). ViewModels and workers get what they
 * need from here instead of creating their own instances.
 *
 * There's a single AppContainer per app process, owned by [PlantWatchApplication].
 *
 * Kotlin notes:
 * - `class AppContainer(private val context: Context)` declares the constructor
 *   in the class header. `private val` makes the parameter a private property.
 * - `by lazy { ... }` is a delegated property: the block runs the first time
 *   `database` is read, and the result is cached for later reads. Opening the
 *   database therefore costs nothing until something actually uses it.
 * - `AppDatabase::class.java` is the Java `Class` object, which this Java-style
 *   Room API expects.
 */
class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        // No `fallbackToDestructiveMigration()`: if a schema change ever lacks a
        // migration, the app crashes instead of silently wiping my plants.
        Room.databaseBuilder(context, AppDatabase::class.java, "plant-watch.db").build()
    }

    val settingsStore: SettingsStore by lazy { SettingsStore(context) }

    val openMeteoApi: OpenMeteoApi by lazy { OpenMeteoApi.create() }

    /** Plant photos in the app's private files folder. */
    val photoStore: PhotoStore by lazy { PhotoStore(java.io.File(context.filesDir, "photos")) }

    /**
     * The care guide bundled in app/src/main/assets, read and parsed on first use.
     * `assets.open` gives an InputStream; `bufferedReader().use { it.readText() }`
     * reads it all and closes it (`use` closes even if reading fails).
     */
    val careGuide: CareGuide by lazy {
        CareGuide.parse(context.assets.open("care_guide.json").bufferedReader().use { it.readText() })
    }
}
