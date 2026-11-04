package com.alberto.plantwatch.plants.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A recurring care task for one plant, e.g. "water every 7 days".
 *
 * Only dates are stored (`lastDone`, `nextDue`), never "days remaining":
 * that is computed from today's date when needed, so it can't go stale.
 *
 * - `foreignKeys`: `plantId` must point to an existing plant. `onDelete = CASCADE`
 *   makes SQLite delete this task automatically when its plant is deleted.
 * - `indices`: `plantId` is indexed so the cascade and "tasks of plant X" are
 *   fast (Room warns about un-indexed foreign keys); `nextDue` is indexed for
 *   the daily reminder query "everything due on or before today".
 * - `Plant::class` is a class reference (like `Plant.class` in Java).
 * - `[...]` inside an annotation is an array literal (annotation parameters
 *   take arrays; in normal code you'd write `arrayOf(...)`).
 */
@Entity(
    tableName = "care_tasks",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plantId"), Index("nextDue")],
)
data class CareTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plantId: Long,
    val type: TaskType,
    /**
     * Name shown for [TaskType.OTHER] tasks (e.g. "Mist leaves"); null otherwise.
     * `String?` means "a String or null": in Kotlin, types are non-null unless
     * marked with `?`, and the compiler forces you to handle the null case.
     */
    val customLabel: String? = null,
    val intervalDays: Int,
    /** Null until the task is marked done for the first time. */
    val lastDone: LocalDate? = null,
    val nextDue: LocalDate,
)
