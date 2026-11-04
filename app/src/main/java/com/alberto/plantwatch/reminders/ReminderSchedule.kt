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

/** One plant's due tasks: one notification each. */
data class PlantDue(
    val plantId: Long,
    val plantName: String,
    val tasks: List<DueTask>,
)

/**
 * Groups due tasks by plant, for one notification per plant. Plants are sorted
 * by name (ignoring case); each plant's tasks keep the order of [due].
 *
 * Kotlin note: `sortedBy { }` sorts by the value the lambda returns; `.first()`
 * is safe because groupBy never makes an empty group.
 */
fun groupDueByPlant(due: List<DueTask>): List<PlantDue> =
    due.groupBy { it.plantId }
        .map { (plantId, tasks) -> PlantDue(plantId, tasks.first().plantName, tasks) }
        .sortedBy { it.plantName.lowercase() }

/**
 * Whether a daily job that ran at [now] has drifted too far from its [target]
 * time of day and should be scheduled again. Periodic work slowly drifts (each
 * run is measured from the previous one, and Android may run it late), and a
 * daylight-saving change shifts it by an hour at once.
 *
 * The difference is measured around the clock: 23:50 vs 00:10 is 20 minutes,
 * not 23 h 40 min.
 */
fun needsRealignment(now: LocalTime, target: LocalTime, toleranceMinutes: Long = 45): Boolean {
    val secondsPerDay = 24 * 60 * 60
    val diff = kotlin.math.abs(now.toSecondOfDay() - target.toSecondOfDay())
    val aroundTheClock = minOf(diff, secondsPerDay - diff)
    return aroundTheClock > toleranceMinutes * 60
}
