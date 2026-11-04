package com.alberto.plantwatch.guide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeciesSearchTest {

    // A tiny guide for the tests (the real one is checked in CareGuideDataTest).
    private val guide = CareGuide.parse(
        """
        [
          {"id":"epipremnum-aureum","scientificName":"Epipremnum aureum",
           "namesPt":["jiboia","jibóia"],"namesEn":["pothos","golden pothos"],
           "waterEveryDays":7,"water":{"en":"w","pt":"w"},"sun":"BRIGHT_INDIRECT","sunHint":{"en":"s","pt":"s"},
           "fertilizeEveryDays":30,"fertilizer":{"en":"f","pt":"f"},"problems":[{"title":{"en":"t","pt":"t"},"hint":{"en":"h","pt":"h"}}]},
          {"id":"monstera-deliciosa","scientificName":"Monstera deliciosa",
           "namesPt":["costela-de-adão"],"namesEn":["monstera","swiss cheese plant"],
           "waterEveryDays":7,"water":{"en":"w","pt":"w"},"sun":"BRIGHT_INDIRECT","sunHint":{"en":"s","pt":"s"},
           "fertilizer":{"en":"f","pt":"f"},"problems":[{"title":{"en":"t","pt":"t"},"hint":{"en":"h","pt":"h"}}]},
          {"id":"dracaena-trifasciata","scientificName":"Dracaena trifasciata",
           "namesPt":["espada-de-são-jorge"],"namesEn":["snake plant"],
           "waterEveryDays":21,"water":{"en":"w","pt":"w"},"sun":"PART_SUN","sunHint":{"en":"s","pt":"s"},
           "fertilizer":{"en":"f","pt":"f"},"problems":[{"title":{"en":"t","pt":"t"},"hint":{"en":"h","pt":"h"}}],"toxicToPets":true}
        ]
        """,
    )

    private fun ids(query: String) = searchSpecies(guide.entries, query).map { it.entry.id }

    @Test
    fun `names are normalized without accents, hyphens or capitals`() {
        assertEquals("costela de adao", normalizeName("Costela-de-Adão"))
        assertEquals("espada de sao jorge", normalizeName("  Espada-de-São   Jorge "))
    }

    @Test
    fun `Portuguese names are found with or without accents`() {
        assertEquals("epipremnum-aureum", ids("jiboia").first())
        assertEquals("epipremnum-aureum", ids("Jibóia").first())
        assertEquals("monstera-deliciosa", ids("costela de adao").first())
    }

    @Test
    fun `English and scientific names are found too`() {
        assertEquals("dracaena-trifasciata", ids("snake plant").first())
        assertEquals("monstera-deliciosa", ids("monstera").first())
        assertEquals("epipremnum-aureum", ids("Epipremnum").first())
    }

    @Test
    fun `a plant's own name finds its species`() {
        assertEquals("epipremnum-aureum", ids("Jiboia da sala").first())
    }

    @Test
    fun `better matches rank first and nonsense finds nothing`() {
        // "espada" is a word prefix of "espada de sao jorge" -> only that entry.
        assertEquals(listOf("dracaena-trifasciata"), ids("espada"))
        assertTrue(ids("xyz").isEmpty())
        assertTrue(ids("   ").isEmpty())
    }
}
