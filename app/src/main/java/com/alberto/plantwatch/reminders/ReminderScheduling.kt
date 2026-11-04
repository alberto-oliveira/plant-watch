package com.alberto.plantwatch.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Time of the daily reminder until the user picks another one in settings. */
val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

private const val WORK_NAME = "daily-reminder"

/**
 * Schedules the daily reminder job at [at].
 *
 * - `replace = false` (app start): if the job already exists it's left alone
 *   (`KEEP`), so restarting the app doesn't push the next run further away.
 * - `replace = true` (reminder time changed in settings): the old job is
 *   cancelled and a new one is queued (`CANCEL_AND_REENQUEUE`), so the next run
 *   moves to the new time.
 *
 * - A *periodic* request repeats every 24 h. Its first run is delayed until the
 *   next reminder time ([delayUntilNextRun], unit-tested), which lines the
 *   repetitions up with that time of day.
 * - *Unique* work: there is only ever one job with this name.
 * - Timing is approximate by design: Android may run it a bit later to save
 *   battery (and the 24 h period can drift slowly). No exact alarms, as
 *   decided in CLAUDE.md.
 *
 * Kotlin note: `PeriodicWorkRequestBuilder<ReminderWorker>(...)` uses a *reified
 * type parameter*: the class to run is passed as `<ReminderWorker>` instead of
 * as an argument like `ReminderWorker::class.java`.
 */
fun scheduleDailyReminder(context: Context, at: LocalTime, replace: Boolean) {
    val initialDelay = delayUntilNextRun(LocalDateTime.now(), at)

    val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME,
        if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}
