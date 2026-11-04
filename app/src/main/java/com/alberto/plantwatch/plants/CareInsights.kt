package com.alberto.plantwatch.plants

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/*
 * Learning from the care history: how often a task is *actually* done, to
 * suggest a better interval when the habit differs from the setting.
 * Unit-tested in CareInsightsTest.
 */

/** How far back the history is considered. */
const val INSIGHT_WINDOW_DAYS = 90L

/** At least this many "done" days are needed (that's two gaps). */
const val INSIGHT_MIN_ENTRIES = 3

/**
 * The typical number of days between doing a task, from the dates it was done:
 * the **median** gap over the last [INSIGHT_WINDOW_DAYS] days, or null with
 * fewer than [INSIGHT_MIN_ENTRIES] dates.
 *
 * Why the median: one long gap (a holiday) or a double entry barely moves it,
 * while it would drag an average around. Gaps 5, 5, 6, 21 -> median 5.5 -> 6.
 * With an even count the two middle gaps are averaged and rounded.
 *
 * Kotlin notes: `zipWithNext { a, b -> }` pairs each item with the next one
 * ([1, 4, 9] -> (1,4), (4,9)); `distinct()` drops repeated dates.
 */
fun typicalIntervalDays(doneDates: List<LocalDate>, today: LocalDate): Int? {
    val since = today.minusDays(INSIGHT_WINDOW_DAYS)
    val recent = doneDates.filter { it > since && it <= today }.distinct().sorted()
    if (recent.size < INSIGHT_MIN_ENTRIES) return null
    val gaps = recent.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b) }.sorted()
    val middle = gaps.size / 2
    val median = if (gaps.size % 2 == 1) gaps[middle].toDouble() else (gaps[middle - 1] + gaps[middle]) / 2.0
    return Math.round(median).toInt()
}

/**
 * The interval to suggest for a task set to [intervalDays], or null if the
 * habit is close enough. "Close enough" is within 15% (and always within less
 * than a day): the same margin as weather-aware watering, so its small
 * adjustments don't trigger suggestions on their own.
 */
fun suggestedInterval(intervalDays: Int, typicalDays: Int?): Int? {
    if (typicalDays == null || typicalDays < 1) return null
    val difference = abs(typicalDays - intervalDays)
    return typicalDays.takeIf { difference >= 1 && difference > intervalDays * 0.15 }
}
