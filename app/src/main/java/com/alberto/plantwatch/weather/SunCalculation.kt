package com.alberto.plantwatch.weather

import java.time.LocalDate
import java.time.LocalDateTime

/*
 * Sunlight rules in plain Kotlin (unit-tested in SunCalculationTest).
 */

/** One hour of weather data: the local time the hour *starts* and the seconds of sunshine in it. */
data class HourlySun(
    val time: LocalDateTime,
    val sunshineSeconds: Double,
)

/**
 * Minutes of direct sun a plant got on [date]: the sunshine of the hours from
 * [startHour] (included) to [endHour] (excluded), scaled by [exposurePercent]
 * (e.g. 40 for a plant by a window that gets 40% of the outdoor sun).
 *
 * Example: window 8..18 covers the hours starting 08:00, 09:00, ..., 17:00.
 *
 * Kotlin notes: `filter { }` keeps the matching items; `sumOf { }` adds up a
 * value computed from each item; `in a until b` is a range that excludes `b`.
 */
fun sunMinutesInWindow(
    hours: List<HourlySun>,
    date: LocalDate,
    startHour: Int,
    endHour: Int,
    exposurePercent: Int,
): Double {
    val seconds = hours
        .filter { it.time.toLocalDate() == date && it.time.hour in startHour until endHour }
        .sumOf { it.sunshineSeconds }
    return seconds / 60.0 * exposurePercent / 100.0
}

/**
 * The days that can be stored: before [today] (still in progress) and covered
 * by all 24 hours of data. The first day of a fetch is usually partial (its
 * only hour is 23:00-24:00, because of the hour shift in toHourlySun) and is
 * skipped instead of being stored with almost no sun.
 *
 * Kotlin notes: `groupingBy { }.eachCount()` counts items per key (here, hours
 * per date); `filter` on a Map keeps the entries matching the condition.
 */
fun completeDays(hours: List<HourlySun>, today: LocalDate): List<LocalDate> =
    hours.groupingBy { it.time.toLocalDate() }
        .eachCount()
        .filter { (day, count) -> day < today && count == 24 }
        .keys
        .sorted()

/**
 * Validates a latitude/longitude typed by the user. Returns the pair of numbers,
 * or null if either isn't a number or is out of range (latitude -90..90,
 * longitude -180..180). A comma is accepted as the decimal separator ("-23,55").
 */
fun parseLocation(latitude: String, longitude: String): Pair<Double, Double>? {
    val lat = latitude.trim().replace(',', '.').toDoubleOrNull() ?: return null
    val lon = longitude.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return Pair(lat, lon)
}

/** A plant's sun window and exposure, as entered in its sun dialog. */
data class SunSettings(val startHour: Int, val endHour: Int, val exposurePercent: Int)

/**
 * Validates the sun dialog's three fields: start hour 0..23, end hour 1..24,
 * start before end, exposure 0..100. Returns null if anything is off.
 *
 * Kotlin note: `?: return null` inside an expression stops the function early
 * when the left side is null (here, text that isn't a whole number).
 */
fun parseSunSettings(startHour: String, endHour: String, exposurePercent: String): SunSettings? {
    val start = startHour.trim().toIntOrNull() ?: return null
    val end = endHour.trim().toIntOrNull() ?: return null
    val exposure = exposurePercent.trim().toIntOrNull() ?: return null
    if (start !in 0..23 || end !in 1..24 || start >= end || exposure !in 0..100) return null
    return SunSettings(start, end, exposure)
}
