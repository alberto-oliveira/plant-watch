package com.alberto.plantwatch.data

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Teaches Room how to store types SQLite doesn't know about.
 *
 * SQLite has no date type, so a [LocalDate] is stored as its "epoch day": the
 * number of days since 1970-01-01 (e.g. 2026-11-03 -> 20760). It's compact,
 * has no time zone, and sorts/compares correctly as a plain integer, so a query
 * like `nextDue <= :today` just works.
 *
 * Kotlin notes:
 * - `object` declares a singleton: exactly one instance, created on first use.
 *   It's Kotlin's replacement for a class with only static methods.
 * - `LocalDate?` / `Long?`: the converters accept and return null, so nullable
 *   columns such as `CareTask.lastDone` work too.
 * - `value?.toEpochDay()` is a "safe call": if `value` is null, the whole
 *   expression is null instead of throwing a NullPointerException.
 * - `value?.let { ... }` runs the block only when `value` isn't null; inside it,
 *   `it` is the non-null value.
 */
object Converters {
    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }
}
