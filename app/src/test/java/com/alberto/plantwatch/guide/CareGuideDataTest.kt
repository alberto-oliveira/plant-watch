package com.alberto.plantwatch.guide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks the real, hand-written care_guide.json, so a typo or an implausible
 * value is caught by `./gradlew testDebugUnitTest` rather than in the app.
 * (Unit tests run with the app module as working directory, hence the path.)
 */
class CareGuideDataTest {

    private val guide = CareGuide.parse(File("src/main/assets/care_guide.json").readText())

    @Test
    fun `the guide parses and has a reasonable size`() {
        assertTrue(guide.entries.size >= 40)
    }

    @Test
    fun `ids are unique and look like slugs`() {
        val ids = guide.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.matches(Regex("[a-z0-9-]+")) })
    }

    @Test
    fun `every entry is complete and plausible`() {
        for (e in guide.entries) {
            val where = "entry ${e.id}"
            assertTrue("$where: Portuguese name", e.namesPt.isNotEmpty())
            assertTrue("$where: English name", e.namesEn.isNotEmpty())
            assertTrue("$where: watering 1-60 days", e.waterEveryDays in 1..60)
            assertTrue("$where: fertilizing 7-120 days", e.fertilizeEveryDays == null || e.fertilizeEveryDays in 7..120)
            assertTrue("$where: hints", listOf(e.water, e.sunHint, e.fertilizer).all { it.isNotBlank() })
            assertTrue("$where: problems", e.problems.isNotEmpty() && e.problems.all { it.title.isNotBlank() && it.hint.isNotBlank() })
        }
    }

    @Test
    fun `no name points to two different species`() {
        // groupBy normalized name -> the set of entry ids using it.
        val owners = guide.entries
            .flatMap { e -> (e.namesPt + e.namesEn + e.scientificName).map { normalizeName(it) to e.id } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
        val clashes = owners.filterValues { it.size > 1 }
        assertTrue("names shared by several species: $clashes", clashes.isEmpty())
    }

    @Test
    fun `common Portuguese names find the expected species`() {
        fun top(query: String) = searchSpecies(guide.entries, query).first().entry.id
        assertEquals("epipremnum-aureum", top("jiboia"))
        assertEquals("monstera-deliciosa", top("costela de adao"))
        assertEquals("dracaena-trifasciata", top("espada de sao jorge"))
        assertEquals("zamioculcas-zamiifolia", top("zamioculca"))
        assertEquals("ocimum-basilicum", top("manjericão"))
        assertEquals("nephrolepis-exaltata", top("samambaia"))
        assertEquals("plumeria-pudica", top("jasmim do caribe"))
        assertEquals("lantana-camara", top("cambara"))
        assertEquals("dracaena-sanderiana", top("bambu da sorte"))
    }
}
