package com.alberto.plantwatch.plants

import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.TaskType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for the scheduling rules. They run on the computer's JVM
 * (`./gradlew testDebugUnitTest`), no phone needed.
 *
 * Kotlin notes:
 * - Function names in backticks may contain spaces: handy for readable test names.
 * - `LocalDate.of(2026, 11, 3)`: fixed dates, so the tests give the same result
 *   on any day.
 */
class CareScheduleTest {

    private val today = LocalDate.of(2026, 11, 3)

    // A helper that builds a task with sensible defaults; each test overrides
    // only what matters to it.
    private fun task(
        intervalDays: Int = 7,
        lastDone: LocalDate? = null,
        nextDue: LocalDate = today,
    ) = CareTask(
        id = 1,
        plantId = 1,
        type = TaskType.WATER,
        intervalDays = intervalDays,
        lastDone = lastDone,
        nextDue = nextDue,
    )

    @Test
    fun `marking done sets lastDone to today and nextDue to today plus interval`() {
        val result = markedDone(task(intervalDays = 7), today)

        assertEquals(today, result.lastDone)
        assertEquals(LocalDate.of(2026, 11, 10), result.nextDue)
    }

    @Test
    fun `done late counts from the done date, not from the old due date`() {
        // Was due 3 days ago, done today: next time is in a full interval from today.
        val overdue = task(intervalDays = 7, nextDue = today.minusDays(3))

        assertEquals(today.plusDays(7), markedDone(overdue, today).nextDue)
    }

    @Test
    fun `new interval recalculates nextDue from lastDone`() {
        val done = task(intervalDays = 7, lastDone = today.minusDays(2), nextDue = today.plusDays(5))

        val result = withNewInterval(done, intervalDays = 3)

        assertEquals(3, result.intervalDays)
        assertEquals(today.plusDays(1), result.nextDue) // done 2 days ago + 3
    }

    @Test
    fun `new interval keeps nextDue when the task was never done`() {
        val neverDone = task(intervalDays = 7, lastDone = null, nextDue = today)

        assertEquals(today, withNewInterval(neverDone, intervalDays = 14).nextDue)
    }

    @Test
    fun `snooze moves an overdue task to tomorrow and keeps lastDone`() {
        val overdue = task(intervalDays = 7, lastDone = today.minusDays(10), nextDue = today.minusDays(3))

        val result = snoozed(overdue, today)

        assertEquals(today.plusDays(1), result.nextDue)
        assertEquals(today.minusDays(10), result.lastDone)
    }

    @Test
    fun `most urgent tasks keeps the earliest per kind, sorted, limited`() {
        val water1 = task(nextDue = today.plusDays(3)).copy(id = 1)
        val water2 = task(nextDue = today).copy(id = 2) // same kind, more urgent
        val fertilize = task(nextDue = today.plusDays(12)).copy(id = 3, type = TaskType.FERTILIZE)
        val mist = task(nextDue = today.plusDays(1)).copy(id = 4, type = TaskType.OTHER, customLabel = "Mist")

        val result = mostUrgentTasks(listOf(water1, fertilize, water2, mist))

        assertEquals(listOf(2L, 4L), result.map { it.id }) // water today, then mist tomorrow
    }

    @Test
    fun `daysUntilDue is negative when overdue, zero today, positive ahead`() {
        assertEquals(-2L, daysUntilDue(task(nextDue = today.minusDays(2)), today))
        assertEquals(0L, daysUntilDue(task(nextDue = today), today))
        assertEquals(3L, daysUntilDue(task(nextDue = today.plusDays(3)), today))
    }
}
