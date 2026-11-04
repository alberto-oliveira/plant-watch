package com.alberto.plantwatch.reminders

import com.alberto.plantwatch.plants.data.DueTask
import com.alberto.plantwatch.plants.data.TaskType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderScheduleTest {

    private val nineAm = LocalTime.of(9, 0)

    @Test
    fun `before the reminder time, the next run is later today`() {
        val now = LocalDateTime.of(2026, 11, 3, 7, 30)

        assertEquals(Duration.ofMinutes(90), delayUntilNextRun(now, nineAm))
    }

    @Test
    fun `after the reminder time, the next run is tomorrow`() {
        val now = LocalDateTime.of(2026, 11, 3, 21, 0)

        assertEquals(Duration.ofHours(12), delayUntilNextRun(now, nineAm))
    }

    @Test
    fun `exactly at the reminder time, the next run is tomorrow`() {
        val now = LocalDateTime.of(2026, 11, 3, 9, 0)

        assertEquals(Duration.ofDays(1), delayUntilNextRun(now, nineAm))
    }

    @Test
    fun `due tasks are grouped by task, keeping plant order and separate labels`() {
        val due = listOf(
            DueTask(taskId = 1, plantName = "Basil", type = TaskType.WATER, customLabel = null),
            DueTask(taskId = 2, plantName = "Fern", type = TaskType.WATER, customLabel = null),
            DueTask(taskId = 3, plantName = "Pothos", type = TaskType.FERTILIZE, customLabel = null),
            DueTask(taskId = 4, plantName = "Fern", type = TaskType.OTHER, customLabel = "Mist leaves"),
            DueTask(taskId = 5, plantName = "Basil", type = TaskType.OTHER, customLabel = "Pinch tips"),
        )

        assertEquals(
            listOf(
                DueGroup(TaskType.WATER, null, listOf("Basil", "Fern")),
                DueGroup(TaskType.FERTILIZE, null, listOf("Pothos")),
                DueGroup(TaskType.OTHER, "Mist leaves", listOf("Fern")),
                DueGroup(TaskType.OTHER, "Pinch tips", listOf("Basil")),
            ),
            groupDueTasks(due),
        )
    }
}
