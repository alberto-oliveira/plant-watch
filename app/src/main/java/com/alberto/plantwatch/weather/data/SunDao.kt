package com.alberto.plantwatch.weather.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Queries for the daily sun rows. */
@Dao
interface SunDao {

    /**
     * Inserts the rows, or replaces those whose (plantId, date) already exists.
     * `@Upsert` = "update or insert", decided per row by the primary key.
     */
    @Upsert
    suspend fun upsert(rows: List<SunDaily>)

    /** Minutes of sun on one day, or null if that day hasn't been fetched yet. */
    @Query("SELECT sunMinutes FROM sun_daily WHERE plantId = :plantId AND date = :date")
    fun observeDay(plantId: Long, date: LocalDate): Flow<Double?>

    /**
     * Total minutes from [from] (included) to [until] (excluded), e.g. the last 7
     * days. The weekly figure is computed by the database when asked (`SUM`),
     * never stored. `SUM` of no rows is NULL, hence `Double?` (= no data yet).
     */
    @Query("SELECT SUM(sunMinutes) FROM sun_daily WHERE plantId = :plantId AND date >= :from AND date < :until")
    fun observeTotal(plantId: Long, from: LocalDate, until: LocalDate): Flow<Double?>
}
