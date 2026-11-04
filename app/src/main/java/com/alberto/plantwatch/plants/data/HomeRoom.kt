package com.alberto.plantwatch.plants.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alberto.plantwatch.weather.Compass

/**
 * A room (or place) where plants stand, e.g. "Sala" or "Varanda" (schema v6).
 * Plants can be grouped by room in the list, and a room's window direction is
 * offered as the default when setting up a plant's window-based sun tracking.
 *
 * Named HomeRoom rather than Room, so it can't be confused with Room the
 * database library (`androidx.room.Room`).
 */
@Entity(tableName = "rooms")
data class HomeRoom(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** The direction the room's main window faces, if known. */
    val windowDirection: Compass? = null,
)
