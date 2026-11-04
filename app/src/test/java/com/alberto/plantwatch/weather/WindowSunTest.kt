package com.alberto.plantwatch.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WindowSunTest {

    // São Paulo: southern hemisphere, UTC-3.
    private val lat = -23.55
    private val lon = -46.63
    private val utcOffset = -3 * 3600

    /** The sun's highest point of a (UTC) day, found by checking every minute. */
    private fun highestSun(date: LocalDate, latitude: Double, longitude: Double): SolarPosition =
        (0 until 24 * 60)
            .map { solarPosition(latitude, longitude, date.atStartOfDay().plusMinutes(it.toLong())) }
            .maxBy { it.elevationDegrees }

    @Test
    fun `noon sun height matches 90 minus latitude minus declination, and is in the north here`() {
        // June solstice: declination +23.44, so 90 - |-23.55 - 23.44| = 43.0 degrees.
        val june = highestSun(LocalDate.of(2026, 6, 21), lat, lon)
        assertEquals(43.0, june.elevationDegrees, 0.5)
        // In the southern hemisphere the midday sun is in the north (azimuth ~0/360).
        assertTrue(june.azimuthDegrees < 5 || june.azimuthDegrees > 355)

        // December solstice: declination -23.44 -> almost straight overhead.
        val december = highestSun(LocalDate.of(2026, 12, 21), lat, lon)
        assertEquals(89.9, december.elevationDegrees, 0.5)

        // Equator at the March equinox: overhead as well.
        assertEquals(90.0, highestSun(LocalDate.of(2026, 3, 20), 0.0, 0.0).elevationDegrees, 0.6)
    }

    @Test
    fun `morning sun is in the east`() {
        // 09:00 local = 12:00 UTC, at the March equinox.
        val sun = solarPosition(lat, lon, LocalDateTime.of(2026, 3, 20, 12, 0))
        assertTrue(sun.azimuthDegrees in 60.0..120.0)
        assertTrue(sun.elevationDegrees in 20.0..60.0)
    }

    @Test
    fun `window checks direction, horizon and how deep the sun reaches`() {
        val juneNoonSun = highestSun(LocalDate.of(2026, 6, 21), lat, lon) // ~43 degrees, north
        val northSill = WindowSettings(Compass.N, WindowDistance.SILL, WindowPlacement.IN_FRONT)

        assertTrue(sunReachesPlant(juneNoonSun, northSill))
        assertFalse(sunReachesPlant(juneNoonSun, northSill.copy(direction = Compass.S))) // behind the window
        assertTrue(sunReachesPlant(juneNoonSun, northSill.copy(distance = WindowDistance.UNDER_1M))) // limit ~63
        assertFalse(sunReachesPlant(juneNoonSun, northSill.copy(distance = WindowDistance.FROM_1_TO_2M))) // limit ~34
        assertFalse(sunReachesPlant(SolarPosition(0.0, 3.0), northSill)) // too close to the horizon
    }

    @Test
    fun `daily minutes count only hours reaching the plant, scaled by glass and placement`() {
        val day = LocalDate.of(2026, 6, 21)
        val sunnyDay = (0..23).map { HourlySun(day.atTime(it, 0), 3600.0) } // full sun every hour
        val north = WindowSettings(Compass.N, WindowDistance.SILL, WindowPlacement.IN_FRONT)

        val inFront = windowSunMinutes(sunnyDay, day, utcOffset, lat, lon, north)
        val toTheSide = windowSunMinutes(sunnyDay, day, utcOffset, lat, lon, north.copy(placement = WindowPlacement.TO_THE_SIDE))
        val southWindow = windowSunMinutes(sunnyDay, day, utcOffset, lat, lon, north.copy(direction = Compass.S))

        // A north window in winter here gets most of the day's sun, through glass (x0.75).
        assertTrue(inFront > 6 * 60 * GLASS_TRANSMISSION)
        assertEquals(inFront / 2, toTheSide, 0.001)
        assertEquals(0.0, southWindow, 0.001) // the winter sun never comes from the south here
    }
}
