package com.alberto.plantwatch.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SunCalculationTest {

    private val day = LocalDate.of(2026, 11, 3)

    /** 24 hours of [day] with full sun (3600 s) every hour, plus one sunny hour the next day. */
    private val sunnyDay = (0..23).map { hour -> HourlySun(day.atTime(hour, 0), 3600.0) } +
        HourlySun(day.plusDays(1).atTime(10, 0), 3600.0)

    @Test
    fun `window counts start hour and excludes end hour`() {
        // 8..18 = hours 08:00 .. 17:00 = 10 full hours = 600 minutes
        assertEquals(600.0, sunMinutesInWindow(sunnyDay, day, startHour = 8, endHour = 18, exposurePercent = 100), 0.001)
    }

    @Test
    fun `exposure scales the minutes`() {
        assertEquals(240.0, sunMinutesInWindow(sunnyDay, day, 8, 18, exposurePercent = 40), 0.001)
    }

    @Test
    fun `hours of other days are ignored and partial sun is summed`() {
        val hours = listOf(
            HourlySun(LocalDateTime.of(2026, 11, 2, 10, 0), 3600.0), // previous day
            HourlySun(day.atTime(10, 0), 1800.0), // half an hour of sun
            HourlySun(day.atTime(11, 0), 900.0), // a quarter hour
        )
        assertEquals(45.0, sunMinutesInWindow(hours, day, 0, 24, 100), 0.001)
    }

    @Test
    fun `location parsing accepts valid input and rejects bad input`() {
        assertEquals(Pair(-23.55, -46.63), parseLocation(" -23,55 ", "-46.63"))
        assertNull(parseLocation("91", "0")) // latitude out of range
        assertNull(parseLocation("10", "-181")) // longitude out of range
        assertNull(parseLocation("abc", "10"))
    }

    @Test
    fun `sun settings parsing enforces ranges and start before end`() {
        assertEquals(SunSettings(8, 18, 40), parseSunSettings("8", " 18 ", "40"))
        assertEquals(SunSettings(0, 24, 100), parseSunSettings("0", "24", "100")) // whole day
        assertNull(parseSunSettings("18", "8", "40")) // start after end
        assertNull(parseSunSettings("8", "8", "40")) // empty window
        assertNull(parseSunSettings("8", "18", "120")) // exposure over 100
        assertNull(parseSunSettings("", "18", "40"))
    }

    @Test
    fun `Open-Meteo response is parsed into hours starting one hour before the label, missing values count as no sun`() {
        // Shaped like a real response (trimmed), including fields we don't use.
        val json = """
            {"latitude":-23.5,"longitude":-46.625,"timezone":"America/Sao_Paulo",
             "hourly_units":{"time":"iso8601","sunshine_duration":"s"},
             "hourly":{"time":["2026-11-03T09:00","2026-11-03T10:00"],
                       "sunshine_duration":[3600.0,null]}}
        """.trimIndent()

        val hours = OpenMeteoJson.decodeFromString<OpenMeteoResponse>(json).toHourlySun()

        // "09:00" covers 08:00-09:00 and "10:00" covers 09:00-10:00.
        assertEquals(
            listOf(
                HourlySun(LocalDateTime.of(2026, 11, 3, 8, 0), 3600.0),
                HourlySun(LocalDateTime.of(2026, 11, 3, 9, 0), 0.0),
            ),
            hours,
        )
    }

    @Test
    fun `only past days with all 24 hours are complete`() {
        val partialFirstDay = listOf(HourlySun(day.minusDays(1).atTime(23, 0), 0.0))
        val fullDay = (0..23).map { HourlySun(day.atTime(it, 0), 0.0) }
        val todayHours = (0..23).map { HourlySun(day.plusDays(1).atTime(it, 0), 0.0) }

        assertEquals(listOf(day), completeDays(partialFirstDay + fullDay + todayHours, today = day.plusDays(1)))
    }
}
