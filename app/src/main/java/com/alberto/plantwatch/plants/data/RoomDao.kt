package com.alberto.plantwatch.plants.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Queries for rooms. */
@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<HomeRoom>>

    /** Inserts a room and returns its generated id. */
    @Insert
    suspend fun insert(room: HomeRoom): Long

    @Update
    suspend fun update(room: HomeRoom)

    /** Deletes the room; its plants stay, with no room (foreign key ON DELETE SET NULL). */
    @Delete
    suspend fun delete(room: HomeRoom)
}
