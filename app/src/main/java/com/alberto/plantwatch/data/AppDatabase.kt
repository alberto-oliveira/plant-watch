package com.alberto.plantwatch.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alberto.plantwatch.plants.data.CareLog
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.plants.data.PlantDao

/**
 * The app's Room database: lists every table (entity) and gives access to the DAOs.
 *
 * - `version`: bump it on every schema change and add a migration (CLAUDE.md:
 *   no destructive migrations). Each version's schema is exported as JSON to
 *   `app/schemas/` (see the `room { }` block in app/build.gradle.kts).
 * - `@TypeConverters`: makes the LocalDate converter available to all entities.
 *
 * Kotlin notes:
 * - `abstract class` can't be instantiated directly; Room generates the
 *   concrete subclass (`AppDatabase_Impl`) at build time.
 * - `: RoomDatabase()` = extends RoomDatabase.
 * - The instance is created once, in AppContainer (next step), not here.
 */
@Database(
    entities = [Plant::class, CareTask::class, CareLog::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
}
