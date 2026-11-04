package com.alberto.plantwatch.backup

import com.alberto.plantwatch.plants.data.CareLog
import com.alberto.plantwatch.plants.data.CareTask
import com.alberto.plantwatch.plants.data.HomeRoom
import com.alberto.plantwatch.plants.data.Plant
import com.alberto.plantwatch.plants.data.TaskType
import com.alberto.plantwatch.weather.Compass
import com.alberto.plantwatch.weather.WindowDistance
import com.alberto.plantwatch.weather.WindowPlacement
import com.alberto.plantwatch.weather.data.SunDaily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupFormatTest {

    private val day = LocalDate.of(2026, 11, 5)
    private val room = HomeRoom(1, "Sala", Compass.N)
    private val plant = Plant(
        id = 3, name = "Jiboia", windowDirection = Compass.NE, windowDistance = WindowDistance.UNDER_1M,
        windowPlacement = WindowPlacement.TO_THE_SIDE, speciesId = "epipremnum-aureum",
        notes = "Gift", photoFile = "plant-3-1.jpg", roomId = 1,
    )
    private val task = CareTask(5, 3, TaskType.OTHER, "Mist", 3, lastDone = day.minusDays(1), nextDue = day.plusDays(2))
    private val log = CareLog(7, 5, day.minusDays(1))
    private val sun = SunDaily(3, day.minusDays(2), 123.5)

    @Test
    fun `rows survive a round trip through the backup JSON unchanged`() {
        val data = BackupData(
            schemaVersion = 6, exportedAt = "2026-11-05T10:00",
            settings = BackupSettings(reminderMinutes = 540, latitude = -23.5, longitude = -46.6, adjustWateringToWeather = true),
            rooms = listOf(room.toRecord()), plants = listOf(plant.toRecord()), tasks = listOf(task.toRecord()),
            logs = listOf(log.toRecord()), sun = listOf(sun.toRecord()),
        )

        val back = decodeBackup(encodeBackup(data))

        assertEquals(data, back)
        assertEquals(room, back.rooms.single().toEntity())
        assertEquals(plant, back.plants.single().toEntity())
        assertEquals(task, back.tasks.single().toEntity())
        assertEquals(log, back.logs.single().toEntity())
        assertEquals(sun, back.sun.single().toEntity())
    }

    @Test(expected = BackupFormatException::class)
    fun `a backup from a newer app is refused`() {
        decodeBackup("""{"formatVersion": 99, "schemaVersion": 9, "exportedAt": "x", "settings": {"reminderMinutes": 540}}""")
    }

    @Test(expected = BackupFormatException::class)
    fun `something that isn't a backup is refused`() {
        decodeBackup("""{"hello": "world"}""")
    }

    @Test
    fun `only plain photo names inside photos are accepted`() {
        assertTrue(isSafePhotoEntry("photos/plant-3-1730800000000.jpg"))
        assertFalse(isSafePhotoEntry("photos/../../databases/plant-watch.db"))
        assertFalse(isSafePhotoEntry("photos/sub/x.jpg"))
        assertFalse(isSafePhotoEntry("data.json"))
        assertFalse(isSafePhotoEntry("photos/.hidden.jpg"))
    }
}
