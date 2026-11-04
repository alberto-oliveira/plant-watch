package com.alberto.plantwatch.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CareInsightsTest {

    private val today = LocalDate.of(2026, 11, 5)

    /** Dates done, given as "days ago". */
    private fun daysAgo(vararg days: Long) = days.map { today.minusDays(it) }

    @Test
    fun `the typical interval is the median gap`() {
        // Gaps 5, 5, 6 -> 5.
        assertEquals(5, typicalIntervalDays(daysAgo(16, 11, 6, 0), today))
        // Even count: gaps 4 and 6 -> 5.
        assertEquals(5, typicalIntervalDays(daysAgo(10, 6, 0), today))
    }

    @Test
    fun `one long gap doesn't change it much`() {
        // Gaps 21 (a holiday), 5, 5, 6 -> median of 5, 5, 6, 21 = 5.5 -> 6.
        assertEquals(6, typicalIntervalDays(daysAgo(37, 16, 11, 6, 0), today))
    }

    @Test
    fun `too little or too old history gives nothing`() {
        assertNull(typicalIntervalDays(daysAgo(7, 0), today))
        // Only two of these are within the last 90 days.
        assertNull(typicalIntervalDays(daysAgo(120, 100, 7, 0), today))
        // The same day twice counts once.
        assertNull(typicalIntervalDays(daysAgo(7, 0, 0), today))
    }

    @Test
    fun `only clear differences are suggested`() {
        assertEquals(5, suggestedInterval(7, 5))
        assertNull(suggestedInterval(7, 6)) // within 15%
        assertNull(suggestedInterval(7, 7))
        assertEquals(2, suggestedInterval(3, 2))
        assertEquals(34, suggestedInterval(28, 34))
        assertNull(suggestedInterval(28, 31))
        assertNull(suggestedInterval(7, null))
    }
}
