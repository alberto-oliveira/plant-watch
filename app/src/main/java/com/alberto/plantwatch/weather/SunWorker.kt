package com.alberto.plantwatch.weather

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alberto.plantwatch.PlantWatchApplication
import com.alberto.plantwatch.weather.data.SunDaily
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

/**
 * The daily sunlight job: fetches the last 7 days of hourly sunshine for the
 * saved location (one request), then stores, for each tracked plant and each
 * *complete* day (before today), the minutes of sun within its window, scaled
 * by its exposure. Rows already there are replaced (upsert), so re-fetching
 * corrects earlier values and fills in days that were missed.
 */
class SunWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as PlantWatchApplication).container

        // Nothing to do without a location or without tracked plants.
        val location = container.settingsStore.location.first() ?: return Result.success()
        val plants = container.database.plantDao().getSunTracked()
        if (plants.isEmpty()) return Result.success()

        val hours = try {
            container.openMeteoApi.hourlySunshine(location.latitude, location.longitude).toHourlySun()
        } catch (e: IOException) {
            // No network, timeout...: try again later (WorkManager waits longer each time).
            return Result.retry()
        } catch (e: HttpException) {
            // The server answered with an error status (e.g. 500): also worth a retry.
            return Result.retry()
        }

        val today = LocalDate.now()
        val completeDays = completeDays(hours, today)

        // `flatMap`: for each plant, a list of rows (one per day), all joined into one list.
        val rows = plants.flatMap { plant ->
            // The query only returns tracked plants, but the type system doesn't know
            // that; `?: return@flatMap emptyList()` skips a plant with a missing value.
            val start = plant.sunStartHour ?: return@flatMap emptyList()
            val end = plant.sunEndHour ?: return@flatMap emptyList()
            val exposure = plant.sunExposurePercent ?: return@flatMap emptyList()
            completeDays.map { day ->
                SunDaily(
                    plantId = plant.id,
                    date = day,
                    sunMinutes = sunMinutesInWindow(hours, day, start, end, exposure),
                )
            }
        }
        container.database.sunDao().upsert(rows)
        return Result.success()
    }
}
