package com.alberto.plantwatch.guide

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * The plant care guide: a hand-written list of species with care hints, bundled
 * with the app as app/src/main/assets/care_guide.json (no network, no API key).
 * Plants can be linked to one entry by its [CareGuideEntry.id].
 */

/** How much light a species wants. Stored in the JSON as the constant's name. */
enum class SunNeed { FULL_SUN, PART_SUN, BRIGHT_INDIRECT, LOW_LIGHT }

/** A common problem and what to do about it. */
@Serializable
data class Problem(val title: String, val hint: String)

/**
 * One species (or, if [generic], a group such as "ferns" whose members need
 * similar care).
 *
 * Kotlin note: `= null` / `= false` are defaults, so the JSON may leave those
 * fields out.
 */
@Serializable
data class CareGuideEntry(
    /** Stable id, e.g. "epipremnum-aureum"; stored on linked plants. */
    val id: String,
    val scientificName: String,
    /** Portuguese names, with and without accents/hyphens as people write them. */
    val namesPt: List<String>,
    val namesEn: List<String>,
    val generic: Boolean = false,
    /** Suggested watering interval in days (a typical value; adjust to the plant). */
    val waterEveryDays: Int,
    val water: String,
    val sun: SunNeed,
    val sunHint: String,
    /** Suggested fertilizing interval in days, or null if it rarely needs feeding. */
    val fertilizeEveryDays: Int? = null,
    val fertilizer: String,
    val problems: List<Problem>,
    /** True if toxic to cats/dogs; null if unknown. */
    val toxicToPets: Boolean? = null,
)

/** The whole guide, with lookup by id. */
class CareGuide(val entries: List<CareGuideEntry>) {

    // `associateBy` builds a Map from each entry's id to the entry: fast lookups.
    private val byId: Map<String, CareGuideEntry> = entries.associateBy { it.id }

    /** The entry with this id, or null (e.g. an id removed from a later guide). */
    fun byId(id: String): CareGuideEntry? = byId[id]

    companion object {
        /**
         * Strict JSON settings: an unknown or misspelled key in the hand-written
         * file is an error (caught by the validation test), not silently ignored.
         */
        private val json = Json { ignoreUnknownKeys = false }

        /** Parses the guide from its JSON text (a list of entries). */
        fun parse(text: String): CareGuide = CareGuide(json.decodeFromString<List<CareGuideEntry>>(text))
    }
}
