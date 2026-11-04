package com.alberto.plantwatch.plants.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A plant I take care of. One row in the `plants` table.
 *
 * Kotlin notes:
 * - `data class` automatically generates `equals`, `hashCode`, `toString` and
 *   `copy(...)` from the properties in the constructor. Ideal for plain data.
 * - `val` = read-only property (like `final` in Java). Changing a plant means
 *   creating a modified copy: `plant.copy(name = "New name")`.
 * - `id: Long = 0` is a default value. Room treats 0 as "not set yet" and
 *   assigns a real id when inserting (because of `autoGenerate = true`).
 * - Annotations (`@Entity`, `@PrimaryKey`) are metadata read by Room's code
 *   generator at build time; they don't do anything at runtime by themselves.
 */
@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /*
     * Sun tracking (added in schema v2). All three null = the plant isn't
     * tracked (e.g. indoors away from windows). Otherwise its sun is counted
     * from sunStartHour (included) to sunEndHour (excluded), scaled by
     * sunExposurePercent (100 outdoors, ~40 by a window).
     * Nullable columns need no default for existing rows: they simply become
     * NULL when the database is migrated.
     */
    val sunStartHour: Int? = null,
    val sunEndHour: Int? = null,
    val sunExposurePercent: Int? = null,
) {
    /**
     * A computed property (it has a getter, no stored value), so Room doesn't
     * turn it into a column.
     */
    val isSunTracked: Boolean
        get() = sunStartHour != null && sunEndHour != null && sunExposurePercent != null
}
