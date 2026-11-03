package com.alberto.plantwatch.plants.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Queries for care tasks and their log.
 *
 * Logs are written here too (not in a separate CareLogDao) because a log is only
 * ever written as part of marking a task done; keeping both writes in one DAO
 * lets them share a transaction.
 */
@Dao
interface CareTaskDao {

    /**
     * A plant's tasks, the most urgent first. Re-emits on every change.
     * `:plantId` in the SQL is bound to the function parameter of the same name.
     */
    @Query("SELECT * FROM care_tasks WHERE plantId = :plantId ORDER BY nextDue, id")
    fun observeForPlant(plantId: Long): Flow<List<CareTask>>

    @Insert
    suspend fun insert(task: CareTask): Long

    /** Updates the row with the same `id` (all columns). */
    @Update
    suspend fun update(task: CareTask)

    /** Deletes the task; its logs go with it (CASCADE). */
    @Delete
    suspend fun delete(task: CareTask)

    @Insert
    suspend fun insertLog(log: CareLog)

    /**
     * Saves a task that was just done and records it in the log, as one unit:
     * `@Transaction` makes both writes succeed together or not at all, so the
     * task can't be marked done without a log entry (or the other way round).
     *
     * Kotlin note: interface functions may have a body (a default
     * implementation). Room generates the other methods and keeps this one,
     * wrapping it in a database transaction.
     */
    @Transaction
    suspend fun markDone(updatedTask: CareTask, log: CareLog) {
        update(updatedTask)
        insertLog(log)
    }
}
