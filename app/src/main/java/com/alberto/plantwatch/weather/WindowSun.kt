package com.alberto.plantwatch.weather

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.atan

/*
 * Estimating a plant's direct sun from where its window faces and where the plant
 * stands (unit-tested in WindowSunTest). Deliberately simple: the weather data
 * says whether the sun shone at the location, not what is in front of the window
 * (buildings, trees, curtains), so a finer model would only look more precise.
 */

/**
 * The compass direction a window faces (looking out from inside).
 * An enum can carry values: each constant passes its azimuth to the constructor.
 * Room stores the constant's name ("N", "NE", ...).
 */
enum class Compass(val azimuthDegrees: Double) {
    N(0.0), NE(45.0), E(90.0), SE(135.0), S(180.0), SW(225.0), W(270.0), NW(315.0),
}

/**
 * How far into the room the plant is. The further in, the lower the sun must be
 * to shine that deep through the window. [assumedMeters] is a typical distance
 * for each choice; null = on the sill (no limit).
 */
enum class WindowDistance(val assumedMeters: Double?) {
    SILL(null),
    UNDER_1M(0.5),
    FROM_1_TO_2M(1.5),
    OVER_2M(2.5),
    ;

    /**
     * The highest sun elevation that still reaches the plant, assuming the top of
     * the window is [WINDOW_TOP_ABOVE_PLANT_M] above the plant: a ray over the top
     * of the window must drop at most that height over the distance, so
     * tan(elevation) <= height / distance. E.g. 1.5 m -> about 34 degrees.
     * (The semicolon after the last constant above separates constants from members.)
     */
    val maxSunElevationDegrees: Double
        get() = assumedMeters?.let { Math.toDegrees(atan(WINDOW_TOP_ABOVE_PLANT_M / it)) } ?: 90.0
}

/** Whether the plant is right in front of the window or off to one side. */
enum class WindowPlacement(val factor: Double) {
    IN_FRONT(1.0),
    TO_THE_SIDE(0.5),
}

/** Assumed height of the window's top above the plant, in metres. */
const val WINDOW_TOP_ABOVE_PLANT_M = 1.0

/** Share of sunlight that passes through window glass. */
const val GLASS_TRANSMISSION = 0.75

/** Sun lower than this is ignored (hills, buildings, haze near the horizon). */
const val MIN_SUN_ELEVATION_DEGREES = 5.0

/** Sun more than this far to the side of the window only grazes the glass. */
const val MAX_ANGLE_FROM_WINDOW_DEGREES = 80.0

/** A plant's window settings, as stored on the plant. */
data class WindowSettings(
    val direction: Compass,
    val distance: WindowDistance,
    val placement: WindowPlacement,
)

/**
 * Can direct sun at [sun] reach a plant placed as [window] says? Three checks:
 * the sun is in front of the window, above the horizon, and low enough to shine
 * as deep into the room as the plant stands.
 */
fun sunReachesPlant(sun: SolarPosition, window: WindowSettings): Boolean {
    if (sun.elevationDegrees <= MIN_SUN_ELEVATION_DEGREES) return false
    if (sun.elevationDegrees > window.distance.maxSunElevationDegrees) return false
    // Smallest angle between the two directions (0..180): e.g. 350 vs 10 -> 20.
    val angle = abs((sun.azimuthDegrees - window.direction.azimuthDegrees + 540).mod(360.0) - 180)
    return angle <= MAX_ANGLE_FROM_WINDOW_DEGREES
}

/**
 * Minutes of direct sun a plant behind a window got on [date].
 *
 * For each hour of that day, the sun's position is taken at the middle of the
 * hour. If it can reach the plant, the hour's sunshine counts, reduced by the
 * glass and by the placement factor.
 *
 * [hours] are in the location's local time ([HourlySun.time] = start of the
 * hour); [utcOffsetSeconds] (from Open-Meteo) converts them to UTC for the solar
 * formulas.
 */
fun windowSunMinutes(
    hours: List<HourlySun>,
    date: LocalDate,
    utcOffsetSeconds: Int,
    latitude: Double,
    longitude: Double,
    window: WindowSettings,
): Double = hours
    .filter { it.time.toLocalDate() == date }
    .sumOf { hour ->
        val middleUtc = hour.time.plusMinutes(30).minusSeconds(utcOffsetSeconds.toLong())
        val sun = solarPosition(latitude, longitude, middleUtc)
        if (sunReachesPlant(sun, window)) {
            hour.sunshineSeconds / 60.0 * GLASS_TRANSMISSION * window.placement.factor
        } else {
            0.0
        }
    }
