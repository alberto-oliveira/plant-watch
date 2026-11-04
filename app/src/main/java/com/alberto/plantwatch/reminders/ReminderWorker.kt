package com.alberto.plantwatch.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alberto.plantwatch.PlantWatchApplication
import java.time.LocalDate

/**
 * The daily reminder job, run by WorkManager in the background (the app doesn't
 * need to be open). It reads which care tasks are due and shows, replaces or
 * removes the reminder notification accordingly.
 *
 * Android/Kotlin notes:
 * - WorkManager creates this class itself (by reflection), always with these
 *   two constructor parameters, so dependencies can't be passed in; we fetch
 *   the database from the Application's AppContainer instead.
 * - `CoroutineWorker`: `doWork` is a `suspend` function, so it can call the
 *   DAO's suspend queries directly. It runs off the main thread.
 * - `Result.success()` tells WorkManager the run is finished. (There's also
 *   `retry()` and `failure()`; a reading-only job has no reason to retry.)
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PlantWatchApplication
        val due = app.container.database.careTaskDao().getDueTasks(LocalDate.now())

        if (due.isEmpty()) {
            // Nothing due: remove yesterday's reminder if it's still there.
            ReminderNotifications.cancel(applicationContext)
        } else {
            ReminderNotifications.show(applicationContext, due)
        }
        return Result.success()
    }
}
