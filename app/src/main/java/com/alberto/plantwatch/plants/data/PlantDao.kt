package com.alberto.plantwatch.plants.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO = "data access object": the queries for the `plants` table.
 *
 * It's an `interface` (method signatures only). Room's code generator writes the
 * implementing class at build time, with the SQL checked against the schema: a
 * typo in a table or column name fails the build, not the app.
 *
 * Kotlin notes:
 * - `Flow<List<Plant>>` is an observable stream (from Kotlin coroutines). Room
 *   emits the current list immediately and then a new list every time the table
 *   changes, so the UI updates itself after an insert or delete.
 * - `suspend fun` is a function that can pause without blocking a thread. Room
 *   runs these on a background thread; callers must be inside a coroutine
 *   (e.g. `viewModelScope.launch { dao.insert(plant) }` in a ViewModel).
 *   Reading through a Flow needs no `suspend`: the waiting happens when the
 *   stream is collected.
 */
@Dao
interface PlantDao {
    // COLLATE NOCASE: alphabetical order ignoring case ("fern" next to "Fern").
    @Query("SELECT * FROM plants ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Plant>>

    /**
     * One plant, for the detail screen. `Plant?` because the plant may not exist
     * (e.g. deleted): the Flow then emits null instead of failing.
     */
    @Query("SELECT * FROM plants WHERE id = :id")
    fun observeById(id: Long): Flow<Plant?>

    /** Inserts a plant and returns its generated id. */
    @Insert
    suspend fun insert(plant: Plant): Long

    /** Saves changes to a plant (matched by id), e.g. its sun settings. */
    @Update
    suspend fun update(plant: Plant)

    /**
     * Plants with sun tracking on (window mode or custom hours), for the daily
     * sun job (one-shot read).
     */
    @Query(
        """
        SELECT * FROM plants
        WHERE (windowDirection IS NOT NULL AND windowDistance IS NOT NULL AND windowPlacement IS NOT NULL)
           OR (sunExposurePercent IS NOT NULL AND sunStartHour IS NOT NULL AND sunEndHour IS NOT NULL)
        """,
    )
    suspend fun getSunTracked(): List<Plant>

    /** Deletes the plant (matched by id); its tasks and logs go with it (CASCADE). */
    @Delete
    suspend fun delete(plant: Plant)
}
