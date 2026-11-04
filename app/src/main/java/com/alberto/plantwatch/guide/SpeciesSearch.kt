package com.alberto.plantwatch.guide

import java.text.Normalizer

/*
 * Finding a species by any of its names, in Portuguese or English, the way
 * people actually type them: with or without accents, hyphens or capitals.
 * Unit-tested in SpeciesSearchTest.
 */

/**
 * A name reduced to a comparable form: lowercase, no accents, hyphens and
 * apostrophes as spaces, single spaces. "Costela-de-Adão" -> "costela de adao".
 *
 * How accents are removed: Unicode normalization form NFD splits "ã" into "a"
 * plus a separate "combining tilde" character; the regex `\p{Mn}` (Unicode
 * category "Mark, nonspacing") then deletes those marks.
 */
fun normalizeName(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .replace(Regex("[-'’]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/** A search hit: the entry and the name that matched (to show the user why). */
data class SpeciesMatch(
    val entry: CareGuideEntry,
    val matchedName: String,
)

/**
 * Entries matching [query], best first, at most [limit].
 *
 * Each name gets a score (lower = better):
 * 0. exactly the query ("jiboia")
 * 1. starts with the query ("jib" -> "jiboia")
 * 2. one of its words starts with the query ("adao" -> "costela de adao")
 * 3. the name appears as whole words inside the query, so the plant's own
 *    name works as a search: "jiboia da sala" -> "jiboia"
 * 4. the query appears anywhere ("boia" -> "jiboia")
 * An entry's score is its best name's. Ties: Portuguese names first, then
 * alphabetical.
 *
 * Kotlin notes: `mapNotNull` drops entries without any matching name;
 * `sortedWith(compareBy(...))` sorts by several keys in order.
 */
fun searchSpecies(entries: List<CareGuideEntry>, query: String, limit: Int = 8): List<SpeciesMatch> {
    val q = normalizeName(query)
    if (q.isEmpty()) return emptyList()

    // A scored candidate: (match, score, isPortuguese).
    data class Scored(val match: SpeciesMatch, val score: Int, val portuguese: Boolean)

    return entries.mapNotNull { entry ->
        val names = entry.namesPt.map { it to true } +
            (entry.namesEn + entry.scientificName).map { it to false }
        names.mapNotNull { (name, portuguese) ->
            nameScore(normalizeName(name), q)?.let { Scored(SpeciesMatch(entry, name), it, portuguese) }
        }.minWithOrNull(compareBy<Scored>({ it.score }, { !it.portuguese }))
    }
        .sortedWith(compareBy({ it.score }, { !it.portuguese }, { normalizeName(it.match.matchedName) }))
        .take(limit)
        .map { it.match }
}

/** The score of one normalized name for a normalized query, or null if it doesn't match. */
private fun nameScore(name: String, query: String): Int? = when {
    name == query -> 0
    name.startsWith(query) -> 1
    name.split(' ').any { it.startsWith(query) } -> 2
    " $query ".contains(" $name ") -> 3
    name.contains(query) -> 4
    else -> null
}
