package com.alberto.plantwatch.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alberto.plantwatch.plants.data.CareLog
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.CareTaskDao
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.plants.data.PlantDao
import com.alberto.plantwatch.weather.data.SunDaily
import com.alberto.plantwatch.weather.data.SunDao

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
    entities = [Plant::class, CareTask::class, CareLog::class, SunDaily::class],
    version = 5,
    exportSchema = true,
    // How to upgrade an existing database, one version at a time. An
    // *auto* migration: Room compares the exported schemas 1.json and 2.json
    // and generates the SQL (here: 3 new nullable columns on plants, and the
    // new sun_daily table; v3: 3 nullable window columns on plants; v4: the
    // nullable speciesId column on plants; v5: nullable notes and photoFile).
    // Complex
    // changes (renames, splits) need a hand-written Migration instead.
    // Room chains them: a v1 database is upgraded 1 -> 2 -> 3 -> 4 -> 5.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun careTaskDao(): CareTaskDao
    abstract fun sunDao(): SunDao
}
