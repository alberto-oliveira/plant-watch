package com.alberto.plantwatch.plants.data

/**
 * A care task that is due, together with its plant's name: what the daily
 * reminder needs to say "Water: Basil". A query result (see
 * CareTaskDao.getDueTasks), not a table.
 */
data class DueTask(
    val taskId: Long,
    val plantId: Long,
    val plantName: String,
    val type: TaskType,
    val customLabel: String?,
)
