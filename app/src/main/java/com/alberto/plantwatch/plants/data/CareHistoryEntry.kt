package com.alberto.plantwatch.plants.data

import java.time.LocalDate

/**
 * One line of a plant's care history: "on [doneOn], [type]/[customLabel] was done".
 *
 * This is **not** an @Entity (there's no such table): it's the shape of a query
 * result that combines columns from `care_logs` and `care_tasks` (see
 * [CareTaskDao.observeHistoryForPlant]). Room fills it by matching the query's
 * column names to these property names, and converts `doneOn` with the same
 * LocalDate converter used by the entities.
 */
data class CareHistoryEntry(
    val logId: Long,
    val doneOn: LocalDate,
    val type: TaskType,
    val customLabel: String?,
)
