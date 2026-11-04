package com.alberto.plantwatch.guide

import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.TaskType
import java.time.LocalDate

/**
 * The care tasks the guide suggests for a plant that it doesn't have yet:
 * watering every [CareGuideEntry.waterEveryDays], and fertilizing every
 * [CareGuideEntry.fertilizeEveryDays] if the species needs it. A type the plant
 * already has (with any interval) is never suggested again, so the user's own
 * choices win. New tasks are due today, like any newly added task.
 *
 * Kotlin note: `buildList { }` creates a list by calling `add` inside the block,
 * convenient when some items are conditional.
 */
fun suggestedTasks(
    entry: CareGuideEntry,
    plantId: Long,
    existing: List<CareTask>,
    today: LocalDate,
): List<CareTask> {
    val hasType = existing.map { it.type }.toSet()
    return buildList {
        if (TaskType.WATER !in hasType) {
            add(CareTask(plantId = plantId, type = TaskType.WATER, intervalDays = entry.waterEveryDays, nextDue = today))
        }
        val fertilizeDays = entry.fertilizeEveryDays
        if (fertilizeDays != null && TaskType.FERTILIZE !in hasType) {
            add(CareTask(plantId = plantId, type = TaskType.FERTILIZE, intervalDays = fertilizeDays, nextDue = today))
        }
    }
}
