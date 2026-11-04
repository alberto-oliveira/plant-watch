package com.alberto.plantwatch.weather

import java.time.LocalDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Where the sun is in the sky, seen from one place at one moment.
 *
 * @property azimuthDegrees compass direction of the sun: 0 = north, 90 = east,
 *   180 = south, 270 = west.
 * @property elevationDegrees height above the horizon: 0 = on the horizon,
 *   90 = straight overhead, negative = below the horizon (night).
 */
data class SolarPosition(val azimuthDegrees: Double, val elevationDegrees: Double)

/**
 * The sun's position at [latitude]/[longitude] (decimal degrees; south and west
 * negative) at the given **UTC** time.
 *
 * Uses NOAA's solar calculator formulas (the ones behind their online "Solar
 * Calculator" spreadsheet). They are accurate to well under a degree, far more
 * than this app needs; atmospheric refraction is ignored.
 *
 * Outline:
 * 1. Express the moment as Julian centuries since the year 2000 (astronomers' clock).
 * 2. From that, the sun's position along its yearly path, giving the
 *    *declination* (how far north/south of the equator the sun is overhead,
 *    +-23.44 degrees over the year) and the *equation of time* (how far sundials
 *    run ahead of/behind clocks, up to ~16 minutes).
 * 3. The *hour angle* (how far the sun is from its highest point of the day)
 *    combined with the latitude and declination gives elevation and azimuth.
 *
 * Kotlin notes: `Math.toRadians`/`toDegrees` convert angles because sin/cos
 * work in radians; `.mod(360.0)` is a remainder that is never negative (unlike
 * `%`); `coerceIn` clamps a value into a range (here guarding `acos` against
 * rounding just past +-1).
 */
fun solarPosition(latitude: Double, longitude: Double, utc: LocalDateTime): SolarPosition {
    // 1. Julian day (days since noon on 1 January 4713 BC) and centuries since J2000.
    val minutesOfDay = utc.hour * 60.0 + utc.minute + utc.second / 60.0
    val julianDay = utc.toLocalDate().toEpochDay() + 2440587.5 + minutesOfDay / 1440.0
    val t = (julianDay - 2451545.0) / 36525.0

    // 2. The sun's path.
    val meanLongitude = (280.46646 + t * (36000.76983 + t * 0.0003032)).mod(360.0)
    val meanAnomaly = 357.52911 + t * (35999.05029 - 0.0001537 * t)
    val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
    val anomalyRad = Math.toRadians(meanAnomaly)
    val equationOfCenter = sin(anomalyRad) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
        sin(2 * anomalyRad) * (0.019993 - 0.000101 * t) +
        sin(3 * anomalyRad) * 0.000289
    val trueLongitude = meanLongitude + equationOfCenter
    val omega = Math.toRadians(125.04 - 1934.136 * t)
    val apparentLongitude = trueLongitude - 0.00569 - 0.00478 * sin(omega)
    val meanObliquity = 23 + (26 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60
    val obliquity = meanObliquity + 0.00256 * cos(omega)
    val declinationRad = asin(sin(Math.toRadians(obliquity)) * sin(Math.toRadians(apparentLongitude)))

    val y = tan(Math.toRadians(obliquity / 2)).let { it * it }
    val l0 = Math.toRadians(meanLongitude)
    val equationOfTimeMinutes = 4 * Math.toDegrees(
        y * sin(2 * l0) - 2 * eccentricity * sin(anomalyRad) +
            4 * eccentricity * y * sin(anomalyRad) * cos(2 * l0) -
            0.5 * y * y * sin(4 * l0) - 1.25 * eccentricity * eccentricity * sin(2 * anomalyRad),
    )

    // 3. Position in the sky. True solar time: minutes since solar midnight here.
    val trueSolarTime = (minutesOfDay + equationOfTimeMinutes + 4 * longitude).mod(1440.0)
    val hourAngle = if (trueSolarTime / 4 < 0) trueSolarTime / 4 + 180 else trueSolarTime / 4 - 180
    val latRad = Math.toRadians(latitude)
    val cosZenith = (sin(latRad) * sin(declinationRad) +
        cos(latRad) * cos(declinationRad) * cos(Math.toRadians(hourAngle))).coerceIn(-1.0, 1.0)
    val zenithRad = acos(cosZenith)

    val azimuthBase = Math.toDegrees(
        acos(
            ((sin(latRad) * cos(zenithRad) - sin(declinationRad)) / (cos(latRad) * sin(zenithRad)))
                .coerceIn(-1.0, 1.0),
        ),
    )
    // Afternoon (sun past its highest point) and morning use mirrored formulas.
    val azimuth = if (hourAngle > 0) (azimuthBase + 180).mod(360.0) else (540 - azimuthBase).mod(360.0)

    return SolarPosition(azimuthDegrees = azimuth, elevationDegrees = 90 - Math.toDegrees(zenithRad))
}
