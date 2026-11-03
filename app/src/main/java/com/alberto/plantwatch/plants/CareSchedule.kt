package com.alberto.plantwatch.plants

import com.alberto.plantwatch.plants.data.CareTask
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * Scheduling rules for care tasks, as plain Kotlin functions: no Android, no
 * database, no "current date". Everything they need comes in as parameters,
 * which is what makes them easy to unit-test (see CareScheduleTest).
 *
 * Kotlin notes:
 * - These are *top-level functions*: they live directly in the file, not inside
 *   a class. Kotlin doesn't need a "utility class" to hold them. Other files
 *   call them by name after `import com.alberto.plantwatch.plants.markedDone`.
 * - `task.copy(...)` (from `data class`) returns a new CareTask with only the
 *   named properties changed; the original is untouched (immutability).
 * - `= expression` after the signature is a single-expression function body.
 */

/**
 * The task after being done on [today]: `lastDone = today` and the next due date
 * counts from today (not from the old due date), so doing it late or early
 * doesn't shift the schedule unfairly.
 */
fun markedDone(task: CareTask, today: LocalDate): CareTask =
    task.copy(
        lastDone = today,
        nextDue = today.plusDays(task.intervalDays.toLong()),
    )

/**
 * The task with a new interval. If it has been done before, the due date is
 * recalculated from the last time it was done; if it never was, it stays as
 * is (a brand-new task is due right away anyway).
 */
fun withNewInterval(task: CareTask, intervalDays: Int): CareTask {
    // `lastDone?.plusDays(...)` is null when lastDone is null (safe call);
    // `?:` ("elvis") then supplies the fallback value.
    val newNextDue = task.lastDone?.plusDays(intervalDays.toLong()) ?: task.nextDue
    return task.copy(intervalDays = intervalDays, nextDue = newNextDue)
}

/**
 * Days from [today] until the task is due: negative = overdue by that many
 * days, 0 = due today, positive = due in that many days.
 */
fun daysUntilDue(task: CareTask, today: LocalDate): Long =
    ChronoUnit.DAYS.between(today, task.nextDue)
