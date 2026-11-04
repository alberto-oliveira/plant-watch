package com.alberto.plantwatch.weather

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WateringWeatherTest {

    private val today = LocalDate.of(2026, 11, 5)

    /** [days] complete days of ET0 ending yesterday: [usual] mm, then the last 7 at [lastWeek] mm. */
    private fun et0(days: Int, usual: Double, lastWeek: Double) = (days downTo 1).map { back ->
        today.minusDays(back.toLong()) to (if (back <= 7) lastWeek else usual)
    } + (today to 99.0) // today is incomplete and must be ignored

    @Test
    fun `half of the weekly change is applied`() {
        // 30 days: 23 at 3.0 and 7 at 3.6 -> average 3.14, change +14.6%, half = +7.3%.
        val factor = wateringFactor(et0(30, usual = 3.0, lastWeek = 3.6), today)
        assertEquals(1 + (3.6 / ((23 * 3.0 + 7 * 3.6) / 30) - 1) * 0.5, factor, 1e-9)
    }

    @Test
    fun `the factor is limited to plus or minus 15 percent`() {
        assertEquals(1.15, wateringFactor(et0(60, usual = 2.0, lastWeek = 8.0), today), 1e-9)
        assertEquals(0.85, wateringFactor(et0(60, usual = 4.0, lastWeek = 0.5), today), 1e-9)
    }

    @Test
    fun `too little data means no change`() {
        assertEquals(1.0, wateringFactor(et0(10, usual = 3.0, lastWeek = 6.0), today), 0.0)
        assertEquals(1.0, wateringFactor(emptyList(), today), 0.0)
    }

    @Test
    fun `adjusted interval rounds and never goes below one day`() {
        assertEquals(6, adjustedInterval(7, 1.15)) // 6.09
        assertEquals(8, adjustedInterval(7, 0.85)) // 8.24
        assertEquals(7, adjustedInterval(7, 1.0))
        assertEquals(1, adjustedInterval(1, 1.15))
    }
}
