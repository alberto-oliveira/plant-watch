package com.alberto.plantwatch.backup

import com.alberto.plantwatch.data.AppDatabase
import com.alberto.plantwatch.plants.PhotoStore
import com.alberto.plantwatch.settings.Location
import com.alberto.plantwatch.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Exporting and importing everything: the database, the settings and the
 * photos. Created once, in AppContainer.
 *
 * Kotlin note: `withContext(Dispatchers.IO) { }` runs the block on a thread
 * pool meant for file and network work, so the UI thread never waits on it.
 */
class Backup(
    private val database: AppDatabase,
    private val settingsStore: SettingsStore,
    private val photoStore: PhotoStore,
) {
    private val dao = database.backupDao()

    /** Writes a complete backup ZIP to [out] (the caller closes [out]). */
    suspend fun export(out: OutputStream) = withContext(Dispatchers.IO) {
        val plants = dao.allPlants()
        val location = settingsStore.location.first()
        val reminder = settingsStore.reminderTime.first()
        val data = BackupData(
            schemaVersion = SCHEMA_VERSION,
            exportedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
            settings = BackupSettings(
                reminderMinutes = reminder.hour * 60 + reminder.minute,
                latitude = location?.latitude,
                longitude = location?.longitude,
                pauseFertilizingInWinter = settingsStore.pauseFertilizingInWinter.first(),
                adjustWateringToWeather = settingsStore.adjustWateringToWeather.first(),
            ),
            rooms = dao.allRooms().map { it.toRecord() },
            plants = plants.map { it.toRecord() },
            tasks = dao.allTasks().map { it.toRecord() },
            logs = dao.allLogs().map { it.toRecord() },
            sun = dao.allSun().map { it.toRecord() },
        )
        // The plants' photos that exist on disk.
        val photos = plants.mapNotNull { it.photoFile }.map { photoStore.fileFor(it) }.filter { it.isFile }
        writeBackupZip(out, data, photos)
    }

    /** Reads a backup ZIP (to preview it before importing). */
    suspend fun read(input: InputStream): BackupContents = withContext(Dispatchers.IO) { readBackupZip(input) }

    /**
     * Replaces all data with the backup's. Returns the restored reminder time,
     * so the caller can move the daily reminder to it.
     *
     * Steps, ordered so a failure leaves the current data usable:
     * 1. write the backup's photos (new names never clash with old ones,
     *    and if a name exists it is the same photo);
     * 2. replace the database in one transaction (all or nothing);
     * 3. restore the settings;
     * 4. delete photo files no plant refers to anymore.
     */
    suspend fun import(contents: BackupContents): LocalTime = withContext(Dispatchers.IO) {
        val data = contents.data
        photoStore.dir.mkdirs()
        for ((name, bytes) in contents.photos) photoStore.fileFor(name).writeBytes(bytes)

        dao.replaceAll(
            rooms = data.rooms.map { it.toEntity() },
            plants = data.plants.map { it.toEntity() },
            tasks = data.tasks.map { it.toEntity() },
            logs = data.logs.map { it.toEntity() },
            sun = data.sun.map { it.toEntity() },
        )

        val s = data.settings
        val reminder = LocalTime.of(s.reminderMinutes / 60, s.reminderMinutes % 60)
        settingsStore.setReminderTime(reminder)
        // A backup without a location leaves the current one in place.
        if (s.latitude != null && s.longitude != null) settingsStore.setLocation(Location(s.latitude, s.longitude))
        settingsStore.setPauseFertilizingInWinter(s.pauseFertilizingInWinter)
        settingsStore.setAdjustWateringToWeather(s.adjustWateringToWeather)

        val used = data.plants.mapNotNull { it.photoFile }.toSet()
        photoStore.dir.listFiles()?.filter { it.name !in used }?.forEach { it.delete() }
        reminder
    }

    private companion object {
        /** The database version written into backups (see AppDatabase). */
        const val SCHEMA_VERSION = 6
    }
}
