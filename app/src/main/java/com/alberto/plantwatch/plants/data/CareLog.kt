package com.alberto.plantwatch.plants.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * History entry: "task X was done on date Y". One row each time a task is
 * marked done.
 *
 * Deleting a task deletes its logs (CASCADE). Since deleting a plant deletes
 * its tasks, deleting a plant removes its whole history too.
 */
@Entity(
    tableName = "care_logs",
    foreignKeys = [
        ForeignKey(
            entity = CareTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class CareLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val doneOn: LocalDate,
)
