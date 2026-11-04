package com.alberto.plantwatch.plants.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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

    /** Every task of every plant (for the plant list's next-care line). */
    @Query("SELECT * FROM care_tasks")
    fun observeAll(): Flow<List<CareTask>>

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
     * The plant's care history, newest first (at most 30 entries).
     *
     * SQL notes:
     * - `JOIN ... ON`: combines each log row with the task it belongs to, so we
     *   get the task's type and label next to the log's date.
     * - `AS logId` renames a column to match the property in [CareHistoryEntry].
     * - The Flow re-emits when either table changes (Room tracks both).
     */
    @Query(
        """
        SELECT care_logs.id AS logId, care_logs.doneOn, care_tasks.type, care_tasks.customLabel
        FROM care_logs
        JOIN care_tasks ON care_logs.taskId = care_tasks.id
        WHERE care_tasks.plantId = :plantId
        ORDER BY care_logs.doneOn DESC, care_logs.id DESC
        LIMIT 30
        """,
    )
    fun observeHistoryForPlant(plantId: Long): Flow<List<CareHistoryEntry>>

    /** The plant's log entries since [from], for the "you usually…" insights. */
    @Query(
        """
        SELECT care_logs.* FROM care_logs
        JOIN care_tasks ON care_logs.taskId = care_tasks.id
        WHERE care_tasks.plantId = :plantId AND care_logs.doneOn >= :from
        """,
    )
    fun observeLogsForPlantSince(plantId: Long, from: LocalDate): Flow<List<CareLog>>

    /**
     * Every task due on or before [today], with its plant's name, for the daily
     * reminder. Water first, then fertilize, then other tasks (by label); plants
     * alphabetically within each.
     *
     * Unlike the `observe...` queries this is a one-shot read: a plain `suspend`
     * function returning a List (no Flow), because the reminder job runs once
     * and finishes. `:today` is converted to an epoch day by the type converter,
     * so `nextDue <= :today` is an integer comparison.
     *
     * SQL: `CASE ... END` maps each type to a number, to sort in our own order
     * rather than alphabetically.
     */
    @Query(
        """
        SELECT care_tasks.id AS taskId, plants.id AS plantId, plants.name AS plantName, care_tasks.type, care_tasks.customLabel
        FROM care_tasks
        JOIN plants ON care_tasks.plantId = plants.id
        WHERE care_tasks.nextDue <= :today
        ORDER BY CASE care_tasks.type WHEN 'WATER' THEN 0 WHEN 'FERTILIZE' THEN 1 ELSE 2 END,
                 care_tasks.customLabel,
                 plants.name COLLATE NOCASE
        """,
    )
    suspend fun getDueTasks(today: LocalDate): List<DueTask>

    /**
     * The tasks with these ids (those still existing), read fresh from the
     * database by the notification's Done/Snooze actions.
     * `IN (:ids)`: Room expands the list into `IN (?, ?, ?)`, one per id.
     */
    @Query("SELECT * FROM care_tasks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<CareTask>

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
