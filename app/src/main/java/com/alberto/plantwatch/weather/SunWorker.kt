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
 * The daily weather job. First it updates the weather-aware watering factor
 * (daily ET0), then it fetches the last 7 days of hourly sunshine for the
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

        // Nothing to do without a location.
        val location = container.settingsStore.location.first() ?: return Result.success()

        val today = LocalDate.now()
        val plants = container.database.plantDao().getSunTracked()
        val response = try {
            // 1. Weather-aware watering: how fast pots dry this week vs usual.
            //    Stored even when the setting is off, so it's ready when turned on.
            val et0 = container.openMeteoApi.dailyEt0(location.latitude, location.longitude).toDailyEt0()
            container.settingsStore.setWateringFactor(wateringFactor(et0, today))

            // 2. Sunshine, only needed if some plant is tracked.
            if (plants.isEmpty()) return Result.success()
            container.openMeteoApi.hourlySunshine(location.latitude, location.longitude)
        } catch (e: IOException) {
            // No network, timeout...: try again later (WorkManager waits longer each time).
            return Result.retry()
        } catch (e: HttpException) {
            // The server answered with an error status (e.g. 500): also worth a retry.
            return Result.retry()
        }

        val hours = response.toHourlySun()
        val completeDays = completeDays(hours, today)

        // `flatMap`: for each plant, a list of rows (one per day), all joined into one list.
        val rows = plants.flatMap { plant ->
            val window = plant.windowSettings
            // A local function: how many minutes this plant got on one day, by mode.
            fun minutesOn(day: LocalDate): Double? = if (window != null) {
                // Window mode: estimated from the sun's position (see WindowSun.kt).
                windowSunMinutes(hours, day, response.utcOffsetSeconds, location.latitude, location.longitude, window)
            } else {
                // Custom hours. The query only returns tracked plants, but the type
                // system doesn't know that: `?: return null` skips missing values.
                val start = plant.sunStartHour ?: return null
                val end = plant.sunEndHour ?: return null
                val exposure = plant.sunExposurePercent ?: return null
                sunMinutesInWindow(hours, day, start, end, exposure)
            }
            // `mapNotNull`: like `map`, but drops the nulls.
            completeDays.mapNotNull { day ->
                minutesOn(day)?.let { minutes -> SunDaily(plantId = plant.id, date = day, sunMinutes = minutes) }
            }
        }
        container.database.sunDao().upsert(rows)
        return Result.success()
    }
}
