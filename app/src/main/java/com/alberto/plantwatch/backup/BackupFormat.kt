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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/*
 * The backup file's content (data.json inside the backup ZIP), as its own
 * simple, readable format, deliberately separate from the database classes:
 * dates are ISO text ("2026-11-05"), enums their names ("WATER"). The database
 * can change shape in later versions while old backups stay readable.
 * Unit-tested in BackupFormatTest.
 */

/** Version of this file format. Bump when it changes incompatibly. */
const val BACKUP_FORMAT_VERSION = 1

@Serializable
data class BackupData(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    /** Database schema version of the app that wrote it (informational). */
    val schemaVersion: Int,
    /** When it was written, ISO date-time. */
    val exportedAt: String,
    val settings: BackupSettings,
    val rooms: List<RoomRecord> = emptyList(),
    val plants: List<PlantRecord> = emptyList(),
    val tasks: List<TaskRecord> = emptyList(),
    val logs: List<LogRecord> = emptyList(),
    val sun: List<SunRecord> = emptyList(),
)

@Serializable
data class BackupSettings(
    val reminderMinutes: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pauseFertilizingInWinter: Boolean = true,
    val adjustWateringToWeather: Boolean = false,
)

@Serializable
data class RoomRecord(val id: Long, val name: String, val windowDirection: String? = null)

@Serializable
data class PlantRecord(
    val id: Long,
    val name: String,
    val sunStartHour: Int? = null,
    val sunEndHour: Int? = null,
    val sunExposurePercent: Int? = null,
    val windowDirection: String? = null,
    val windowDistance: String? = null,
    val windowPlacement: String? = null,
    val speciesId: String? = null,
    val notes: String? = null,
    val photoFile: String? = null,
    val roomId: Long? = null,
)

@Serializable
data class TaskRecord(
    val id: Long,
    val plantId: Long,
    val type: String,
    val customLabel: String? = null,
    val intervalDays: Int,
    val lastDone: String? = null,
    val nextDue: String,
)

@Serializable
data class LogRecord(val id: Long, val taskId: Long, val doneOn: String)

@Serializable
data class SunRecord(val plantId: Long, val date: String, val sunMinutes: Double)

/** A backup the app can't read (newer format, or not a Plant Watch backup). */
class BackupFormatException(message: String) : Exception(message)

/** JSON settings for backups: indented (readable), tolerant of unknown fields. */
val BackupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun encodeBackup(data: BackupData): String = BackupJson.encodeToString(BackupData.serializer(), data)

/**
 * Reads data.json. Refuses files written by a newer app (unknown format
 * version) instead of importing them half-understood.
 */
fun decodeBackup(text: String): BackupData {
    val data = try {
        BackupJson.decodeFromString(BackupData.serializer(), text)
    } catch (e: Exception) {
        throw BackupFormatException("Not a Plant Watch backup: ${e.message}")
    }
    if (data.formatVersion > BACKUP_FORMAT_VERSION) {
        throw BackupFormatException("Backup format ${data.formatVersion} is newer than this app supports")
    }
    return data
}

// --- Conversions between database rows and backup records ---------------
// `enumValueOf<T>(name)` turns a stored name back into the enum constant.

fun HomeRoom.toRecord() = RoomRecord(id, name, windowDirection?.name)
fun RoomRecord.toEntity() = HomeRoom(id, name, windowDirection?.let { enumValueOf<Compass>(it) })

fun Plant.toRecord() = PlantRecord(
    id, name, sunStartHour, sunEndHour, sunExposurePercent,
    windowDirection?.name, windowDistance?.name, windowPlacement?.name,
    speciesId, notes, photoFile, roomId,
)

fun PlantRecord.toEntity() = Plant(
    id = id, name = name,
    sunStartHour = sunStartHour, sunEndHour = sunEndHour, sunExposurePercent = sunExposurePercent,
    windowDirection = windowDirection?.let { enumValueOf<Compass>(it) },
    windowDistance = windowDistance?.let { enumValueOf<WindowDistance>(it) },
    windowPlacement = windowPlacement?.let { enumValueOf<WindowPlacement>(it) },
    speciesId = speciesId, notes = notes, photoFile = photoFile, roomId = roomId,
)

fun CareTask.toRecord() = TaskRecord(id, plantId, type.name, customLabel, intervalDays, lastDone?.toString(), nextDue.toString())
fun TaskRecord.toEntity() = CareTask(
    id = id, plantId = plantId, type = enumValueOf<TaskType>(type), customLabel = customLabel,
    intervalDays = intervalDays, lastDone = lastDone?.let(LocalDate::parse), nextDue = LocalDate.parse(nextDue),
)

fun CareLog.toRecord() = LogRecord(id, taskId, doneOn.toString())
fun LogRecord.toEntity() = CareLog(id, taskId, LocalDate.parse(doneOn))

fun SunDaily.toRecord() = SunRecord(plantId, date.toString(), sunMinutes)
fun SunRecord.toEntity() = SunDaily(plantId, LocalDate.parse(date), sunMinutes)

/**
 * Whether a ZIP entry name is an acceptable photo: directly inside "photos/",
 * a plain file name ending in .jpg. Rejects paths like "photos/../../x" that
 * could otherwise write outside the photos folder ("zip slip").
 */
fun isSafePhotoEntry(name: String): Boolean {
    if (!name.startsWith("photos/")) return false
    val file = name.removePrefix("photos/")
    return file.matches(Regex("[A-Za-z0-9._-]+\\.jpg")) && !file.startsWith(".")
}
