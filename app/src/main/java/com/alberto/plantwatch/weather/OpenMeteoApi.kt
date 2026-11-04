package com.alberto.plantwatch.weather

import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * The Open-Meteo forecast API (free, no key), as a Retrofit interface: we only
 * describe the request, and Retrofit generates the code that sends it with
 * OkHttp and decodes the JSON answer into [OpenMeteoResponse].
 *
 * The fixed parameters are written into the path:
 * - `hourly=sunshine_duration`: seconds of sunshine per hour
 * - `timezone=auto`: hourly times in the location's local time, so they line up
 *   with the plants' sun windows (e.g. 08..18 h)
 * - `past_days=7&forecast_days=1`: the last 7 days plus today, so missed days
 *   are filled in on the next fetch
 *
 * Kotlin notes: `suspend` makes the call wait for the network without blocking
 * a thread; Retrofit supports suspend functions directly. `@Query` adds
 * `?latitude=...&longitude=...` to the URL.
 */
interface OpenMeteoApi {

    @GET("v1/forecast?hourly=sunshine_duration&timezone=auto&past_days=7&forecast_days=1")
    suspend fun hourlySunshine(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): OpenMeteoResponse

    /**
     * Daily ET0 for the last 92 days (Open-Meteo's maximum) plus today, for
     * weather-aware watering. A second small request, at most once a day.
     */
    @GET("v1/forecast?daily=et0_fao_evapotranspiration&timezone=auto&past_days=92&forecast_days=1")
    suspend fun dailyEt0(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): OpenMeteoDailyResponse

    companion object {
        /** Builds the client. Created once, lazily, in AppContainer. */
        fun create(): OpenMeteoApi = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(OpenMeteoJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create() // reified: the interface type comes from the return type
    }
}
