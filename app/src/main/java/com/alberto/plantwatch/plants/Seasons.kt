package com.alberto.plantwatch.plants

import java.time.LocalDate
import java.time.Month

/*
 * Winter, for pausing fertilizer reminders: most plants rest in the cold,
 * darker months and shouldn't be fed then. Unit-tested in SeasonsTest.
 *
 * Meteorological seasons are used (whole months): in the southern hemisphere
 * winter is June-August, in the northern December-February. The hemisphere
 * comes from the latitude of the location set in Settings (negative = south).
 */

/** True if [date] is in winter at [latitude]; false when no location is set (null). */
fun isWinter(date: LocalDate, latitude: Double?): Boolean {
    if (latitude == null) return false
    // `in setOf(...)`: membership test on a set of enum values.
    val winterMonths = if (latitude < 0) {
        setOf(Month.JUNE, Month.JULY, Month.AUGUST)
    } else {
        setOf(Month.DECEMBER, Month.JANUARY, Month.FEBRUARY)
    }
    return date.month in winterMonths
}

/**
 * The first day after the winter that [date] is in: 1 September (south) or
 * 1 March (north, of the following year when [date] is in December).
 * Only meaningful when [isWinter] is true for the same arguments.
 */
fun winterEnd(date: LocalDate, latitude: Double): LocalDate = if (latitude < 0) {
    LocalDate.of(date.year, Month.SEPTEMBER, 1)
} else {
    val year = if (date.month == Month.DECEMBER) date.year + 1 else date.year
    LocalDate.of(year, Month.MARCH, 1)
}

/**
 * Until when fertilizing is paused on [today]: the end of the current winter,
 * or null if not paused (setting off, no location, or not winter).
 */
fun fertilizingPausedUntil(today: LocalDate, latitude: Double?, pauseEnabled: Boolean): LocalDate? =
    if (pauseEnabled && latitude != null && isWinter(today, latitude)) winterEnd(today, latitude) else null
