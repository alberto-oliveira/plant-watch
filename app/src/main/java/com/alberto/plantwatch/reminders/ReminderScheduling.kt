package com.alberto.plantwatch.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Time of the daily reminder. Fixed for now; the settings screen will make it configurable. */
val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

private const val WORK_NAME = "daily-reminder"

/**
 * Makes sure the daily reminder job is scheduled. Safe to call on every app
 * start.
 *
 * - A *periodic* request repeats every 24 h. Its first run is delayed until the
 *   next reminder time ([delayUntilNextRun], unit-tested), which lines the
 *   repetitions up with that time of day.
 * - *Unique* work with `KEEP`: if a job with this name already exists, it's
 *   left alone, so restarting the app doesn't push the next run further away.
 * - Timing is approximate by design: Android may run it a bit later to save
 *   battery (and the 24 h period can drift slowly). No exact alarms, as
 *   decided in CLAUDE.md.
 *
 * Kotlin note: `PeriodicWorkRequestBuilder<ReminderWorker>(...)` uses a *reified
 * type parameter*: the class to run is passed as `<ReminderWorker>` instead of
 * as an argument like `ReminderWorker::class.java`.
 */
fun scheduleDailyReminder(context: Context, at: LocalTime = DEFAULT_REMINDER_TIME) {
    val initialDelay = delayUntilNextRun(LocalDateTime.now(), at)

    val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}
