package com.alberto.plantwatch.weather

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val DAILY_WORK_NAME = "daily-sun"
private const val NOW_WORK_NAME = "sun-now"

/**
 * `Constraints`: conditions WorkManager waits for before running the job. Here,
 * any network connection; without one, the job simply waits (no failed request).
 */
private val needsNetwork = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

/**
 * Schedules the daily sun fetch (every 24 h, when online). The time of day
 * doesn't matter: it always stores complete past days. `KEEP` leaves an
 * existing schedule alone, so this is safe to call on every app start.
 */
fun scheduleDailySunFetch(context: Context) {
    val request = PeriodicWorkRequestBuilder<SunWorker>(1, TimeUnit.DAYS)
        .setConstraints(needsNetwork)
        .build()
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(DAILY_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}

/**
 * Fetches once now (as soon as there's a network), e.g. right after the location
 * or a plant's sun settings change, so the figures don't wait a day. `REPLACE`:
 * several quick changes result in one fetch, not a queue of them.
 */
fun fetchSunNow(context: Context) {
    val request = OneTimeWorkRequestBuilder<SunWorker>()
        .setConstraints(needsNetwork)
        .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork(NOW_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
}
