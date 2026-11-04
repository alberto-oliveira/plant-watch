package com.alberto.plantwatch.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SeasonsTest {

    private val saoPaulo = -23.55
    private val lisbon = 38.7

    @Test
    fun `winter months depend on the hemisphere, edges included`() {
        assertTrue(isWinter(LocalDate.of(2027, 6, 1), saoPaulo))
        assertTrue(isWinter(LocalDate.of(2027, 8, 31), saoPaulo))
        assertFalse(isWinter(LocalDate.of(2027, 9, 1), saoPaulo))
        assertFalse(isWinter(LocalDate.of(2027, 1, 15), saoPaulo))

        assertTrue(isWinter(LocalDate.of(2026, 12, 1), lisbon))
        assertTrue(isWinter(LocalDate.of(2027, 2, 28), lisbon))
        assertFalse(isWinter(LocalDate.of(2027, 7, 15), lisbon))
    }

    @Test
    fun `no location means never winter`() {
        assertFalse(isWinter(LocalDate.of(2027, 7, 1), null))
        assertNull(fertilizingPausedUntil(LocalDate.of(2027, 7, 1), null, pauseEnabled = true))
    }

    @Test
    fun `pause lasts until the end of winter, and only when enabled`() {
        assertEquals(LocalDate.of(2027, 9, 1), fertilizingPausedUntil(LocalDate.of(2027, 7, 10), saoPaulo, true))
        assertEquals(LocalDate.of(2027, 3, 1), fertilizingPausedUntil(LocalDate.of(2026, 12, 20), lisbon, true))
        assertNull(fertilizingPausedUntil(LocalDate.of(2027, 7, 10), saoPaulo, pauseEnabled = false))
        assertNull(fertilizingPausedUntil(LocalDate.of(2027, 10, 10), saoPaulo, true))
    }
}
