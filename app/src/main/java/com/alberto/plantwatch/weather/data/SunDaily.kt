package com.alberto.plantwatch.weather.data

import androidx.room.Entity
import androidx.room.ForeignKey
import com.alberto.plantwatch.plants.data.Plant
import java.time.LocalDate

/**
 * How many minutes of direct sun a plant got on one day (already limited to its
 * sun window and scaled by its exposure). One row per plant per day.
 *
 * - `primaryKeys = ["plantId", "date"]`: a *composite* primary key. There can be
 *   only one row for each (plant, day) pair, so saving a day again replaces it
 *   (see [SunDao.upsert]), which is how re-fetched days get corrected.
 * - The key's index starts with `plantId`, so it also serves the foreign key
 *   (no separate index needed) and "this plant's days" queries.
 * - Deleting a plant deletes its sun rows (CASCADE), like tasks and logs.
 */
@Entity(
    tableName = "sun_daily",
    primaryKeys = ["plantId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SunDaily(
    val plantId: Long,
    val date: LocalDate,
    val sunMinutes: Double,
)
