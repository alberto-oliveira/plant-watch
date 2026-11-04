package com.alberto.plantwatch.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

/**
 * The part of Open-Meteo's forecast response we use. The JSON looks like:
 * ```
 * { "latitude": ..., "timezone": "America/Sao_Paulo", ...,
 *   "hourly": { "time": ["2026-11-03T00:00", ...], "sunshine_duration": [0.0, ...] } }
 * ```
 * The two hourly arrays are parallel: index i of each belongs to the same hour.
 *
 * Kotlin/serialization notes:
 * - `@Serializable`: the serialization compiler plugin generates the code that
 *   turns JSON into these classes (the same plugin used for navigation routes).
 * - `@SerialName("sunshine_duration")`: the JSON name differs from the Kotlin
 *   property name, which follows Kotlin's camelCase convention.
 * - Fields we don't declare (latitude, timezone, ...) are skipped thanks to
 *   `ignoreUnknownKeys` in [OpenMeteoJson].
 */
@Serializable
data class OpenMeteoResponse(
    /**
     * Offset of the location's local time from UTC, in seconds (e.g. -10800 for
     * UTC-3); used to turn the local hours into UTC for the sun's position.
     * `= 0` is a default used if the field is ever missing from the JSON.
     */
    @SerialName("utc_offset_seconds")
    val utcOffsetSeconds: Int = 0,
    val hourly: Hourly,
) {
    @Serializable
    data class Hourly(
        /**
         * Local times (we request `timezone=auto`), ISO format without seconds.
         * Each value covers the hour *before* its time: "10:00" = 09:00-10:00
         * (Open-Meteo's "preceding hour" convention for sums).
         */
        val time: List<String>,
        /** Seconds of sunshine in each hour (0..3600); null when Open-Meteo has no value. */
        @SerialName("sunshine_duration")
        val sunshineDuration: List<Double?>,
    )
}

/** The JSON settings used to decode Open-Meteo responses (also by Retrofit). */
val OpenMeteoJson = Json { ignoreUnknownKeys = true }

/**
 * Pairs each hour's time with its sunshine. `zip` walks both lists side by side;
 * `?: 0.0` counts a missing value as no sun.
 *
 * Open-Meteo labels each value with the *end* of the hour it covers, so one hour
 * is subtracted: [HourlySun.time] is always the *start* of the hour. (Without
 * this, a plant's 08-18 window would count the sun of 07:00-17:00.)
 */
fun OpenMeteoResponse.toHourlySun(): List<HourlySun> =
    hourly.time.zip(hourly.sunshineDuration) { time, seconds ->
        HourlySun(time = LocalDateTime.parse(time).minusHours(1), sunshineSeconds = seconds ?: 0.0)
    }
