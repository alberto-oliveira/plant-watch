package com.alberto.plantwatch.backup

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.alberto.plantwatch.plants.data.CareLog
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.HomeRoom
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.weather.data.SunDaily

/**
 * Whole-database reads and the all-or-nothing replace used by backups.
 * (Kept apart from the everyday DAOs: nothing else should wipe every table.)
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM rooms") suspend fun allRooms(): List<HomeRoom>
    @Query("SELECT * FROM plants") suspend fun allPlants(): List<Plant>
    @Query("SELECT * FROM care_tasks") suspend fun allTasks(): List<CareTask>
    @Query("SELECT * FROM care_logs") suspend fun allLogs(): List<CareLog>
    @Query("SELECT * FROM sun_daily") suspend fun allSun(): List<SunDaily>

    @Query("DELETE FROM care_logs") suspend fun deleteLogs()
    @Query("DELETE FROM sun_daily") suspend fun deleteSun()
    @Query("DELETE FROM care_tasks") suspend fun deleteTasks()
    @Query("DELETE FROM plants") suspend fun deletePlants()
    @Query("DELETE FROM rooms") suspend fun deleteRooms()

    // Inserting with the backup's own ids keeps the links between rows intact.
    @Insert suspend fun insertRooms(rows: List<HomeRoom>)
    @Insert suspend fun insertPlants(rows: List<Plant>)
    @Insert suspend fun insertTasks(rows: List<CareTask>)
    @Insert suspend fun insertLogs(rows: List<CareLog>)
    @Insert suspend fun insertSun(rows: List<SunDaily>)

    /**
     * Replaces every table's content in one transaction: if any insert fails
     * (e.g. a task pointing to a plant that isn't in the backup, which the
     * foreign keys reject), everything is rolled back and the current data stays.
     *
     * Order matters because of the foreign keys: children are deleted before
     * their parents, and parents inserted before their children.
     */
    @Transaction
    suspend fun replaceAll(
        rooms: List<HomeRoom>,
        plants: List<Plant>,
        tasks: List<CareTask>,
        logs: List<CareLog>,
        sun: List<SunDaily>,
    ) {
        deleteLogs()
        deleteSun()
        deleteTasks()
        deletePlants()
        deleteRooms()
        insertRooms(rooms)
        insertPlants(plants)
        insertTasks(tasks)
        insertLogs(logs)
        insertSun(sun)
    }
}
