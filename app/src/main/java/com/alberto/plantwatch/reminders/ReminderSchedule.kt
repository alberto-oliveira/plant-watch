package com.alberto.plantwatch.reminders

import com.alberto.plantwatch.plants.data.DueTask
import com.alberto.plantwatch.plants.data.TaskType
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/*
 * Plain-Kotlin rules for the daily reminder (unit-tested in ReminderScheduleTest).
 */

/**
 * How long to wait from [now] until the next time the clock shows [at].
 * If that time has already passed today (or is right now), the answer is
 * tomorrow's [at], so the first reminder never fires immediately on install.
 *
 * Kotlin/java.time notes: `LocalDateTime` = date + time without a time zone;
 * `Duration` = an amount of time (here, what WorkManager's initial delay needs).
 */
fun delayUntilNextRun(now: LocalDateTime, at: LocalTime): Duration {
    val todayAt = now.toLocalDate().atTime(at)
    val next = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
    return Duration.between(now, next)
}

/** One line of the reminder: a task and the plants that need it ("Water: Basil, Fern"). */
data class DueGroup(
    val type: TaskType,
    val customLabel: String?,
    val plantNames: List<String>,
)

/**
 * Groups due tasks by what has to be done (type + label for OTHER tasks), so
 * the notification shows one line per kind of task instead of one per plant.
 * Groups and plant names keep the order of [due] (the query sorts them).
 *
 * Kotlin notes:
 * - `groupBy { key }` returns a Map from each key to the items with that key,
 *   in first-seen order. The key here is a `Pair` (a 2-value tuple).
 * - `.map { (key, tasks) -> ... }` iterates the map; `(key, tasks)` is a
 *   *destructuring declaration* that splits each entry into its two parts.
 */
fun groupDueTasks(due: List<DueTask>): List<DueGroup> =
    due.groupBy { Pair(it.type, it.customLabel) }
        .map { (key, tasks) ->
            DueGroup(
                type = key.first,
                customLabel = key.second,
                plantNames = tasks.map { it.plantName },
            )
        }
