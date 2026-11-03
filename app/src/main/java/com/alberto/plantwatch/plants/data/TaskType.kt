package com.alberto.plantwatch.plants.data

/**
 * Kind of care task.
 *
 * An `enum class` is a fixed set of named values, like Java enums. Room stores
 * it in the database as its name (the text "WATER", "FERTILIZE" or "OTHER"),
 * so the order of the values here can change without corrupting data, but
 * renaming a value would require a migration.
 */
enum class TaskType {
    WATER,
    FERTILIZE,

    /** Anything else; the actual name comes from [CareTask.customLabel]. */
    OTHER,
}
