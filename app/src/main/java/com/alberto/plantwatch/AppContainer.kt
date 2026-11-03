package com.alberto.plantwatch

import android.content.Context
import androidx.room.Room
import com.alberto.plantwatch.data.AppDatabase

/**
 * Manual dependency injection: the one place that creates the app's shared
 * objects (for now, just the database). ViewModels and workers get what they
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
}
