package com.alberto.plantwatch.weather

import java.time.LocalDate
import kotlin.math.roundToInt

/*
 * Weather-aware watering (unit-tested in WateringWeatherTest).
 *
 * ET0 ("reference evapotranspiration", from Open-Meteo) is how many millimetres
 * of water a reference lawn would lose per day to evaporation: higher on warm,
 * dry, sunny, windy days. Comparing the last week with the last ~3 months says
 * whether pots are drying faster or slower than usual. The plants are indoors,
 * where outdoor weather matters less, so only half of the change is applied,
 * and never more than 15% either way.
 */

/** Largest change to a watering interval, either way (15%). */
const val MAX_WATERING_ADJUSTMENT = 0.15

/** Share of the weather change applied indoors. */
const val INDOOR_WEATHER_SHARE = 0.5

/**
 * How much faster (> 1) or slower (< 1) pots are drying than usual, from daily
 * ET0 values. Uses complete days only (before [today]): the last 7 against all
 * of them. Returns 1.0 (no change) with fewer than 14 complete days of data.
 *
 * Kotlin note: `coerceIn(a, b)` clamps a value into the range a..b.
 */
fun wateringFactor(dailyEt0: List<Pair<LocalDate, Double?>>, today: LocalDate): Double {
    val complete = dailyEt0.filter { (day, value) -> day < today && value != null }
        .sortedBy { it.first }
        .map { it.second!! } // safe: nulls were filtered out just above
    if (complete.size < 14) return 1.0
    val usual = complete.average()
    if (usual <= 0.0) return 1.0
    val lastWeek = complete.takeLast(7).average()
    val change = lastWeek / usual - 1
    return (1 + change * INDOOR_WEATHER_SHARE).coerceIn(1 - MAX_WATERING_ADJUSTMENT, 1 + MAX_WATERING_ADJUSTMENT)
}

/**
 * The watering interval to use now: shorter when pots dry faster ([factor] > 1),
 * longer when slower; at least 1 day. E.g. 7 days with factor 1.15 -> 6 days.
 */
fun adjustedInterval(intervalDays: Int, factor: Double): Int =
    maxOf(1, (intervalDays / factor).roundToInt())

/**
 * The interval to schedule when [task] is marked done: adjusted for WATER tasks
 * when the setting is on ([factor] non-null), the task's own interval otherwise.
 */
fun intervalWhenDone(task: com.alberto.plantwatch.plants.data.CareTask, factor: Double?): Int =
    if (factor != null && task.type == com.alberto.plantwatch.plants.data.TaskType.WATER) {
        adjustedInterval(task.intervalDays, factor)
    } else {
        task.intervalDays
    }
