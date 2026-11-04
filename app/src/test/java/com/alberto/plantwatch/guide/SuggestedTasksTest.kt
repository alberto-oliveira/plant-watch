package com.alberto.plantwatch.guide

import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SuggestedTasksTest {

    private val today = LocalDate.of(2026, 11, 4)
    private val basil = CareGuideEntry(
        id = "ocimum-basilicum", scientificName = "Ocimum basilicum", namesPt = listOf("manjericão"),
        namesEn = listOf("basil"), waterEveryDays = 2, water = "w", sun = SunNeed.FULL_SUN, sunHint = "s",
        fertilizeEveryDays = 30, fertilizer = "f", problems = listOf(Problem("t", "h")),
    )

    @Test
    fun `a plant without tasks gets water and fertilize, due today`() {
        val tasks = suggestedTasks(basil, plantId = 7, existing = emptyList(), today = today)

        assertEquals(listOf(TaskType.WATER to 2, TaskType.FERTILIZE to 30), tasks.map { it.type to it.intervalDays })
        assertTrue(tasks.all { it.plantId == 7L && it.nextDue == today })
    }

    @Test
    fun `existing task types are not suggested again, and no fertilizer when not needed`() {
        val water = CareTask(plantId = 7, type = TaskType.WATER, intervalDays = 3, nextDue = today)

        assertEquals(listOf(TaskType.FERTILIZE), suggestedTasks(basil, 7, listOf(water), today).map { it.type })
        assertTrue(suggestedTasks(basil.copy(fertilizeEveryDays = null), 7, listOf(water), today).isEmpty())
    }
}
