package com.alberto.plantwatch.backup

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/*
 * The backup file itself: a ZIP with data.json and a photos/ folder.
 * Plain java.util.zip, no Android, so it's unit-tested in BackupZipTest.
 */

private const val DATA_ENTRY = "data.json"
private const val PHOTOS_DIR = "photos/"

/** A backup read into memory: the data, and each photo's bytes by file name. */
class BackupContents(val data: BackupData, val photos: Map<String, ByteArray>)

/**
 * Writes the backup ZIP to [out], with the given photo files (each copied
 * into the ZIP piece by piece, not loaded into memory whole).
 *
 * Kotlin note: `ZipOutputStream(out).use { zip -> }` closes the ZIP (writing
 * its index at the end) when the block finishes, even after an error.
 */
fun writeBackupZip(out: OutputStream, data: BackupData, photos: List<File>) {
    ZipOutputStream(out).use { zip ->
        zip.putNextEntry(ZipEntry(DATA_ENTRY))
        zip.write(encodeBackup(data).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        for (photo in photos) {
            zip.putNextEntry(ZipEntry(PHOTOS_DIR + photo.name))
            photo.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }
}

/**
 * Reads a backup ZIP. Unexpected entries are ignored, and so are photo names
 * that could escape the photos folder (see [isSafePhotoEntry]).
 * Throws [BackupFormatException] if there's no data.json or it can't be read.
 */
fun readBackupZip(input: InputStream): BackupContents {
    var data: BackupData? = null
    val photos = mutableMapOf<String, ByteArray>()
    try {
        ZipInputStream(input).use { zip ->
            // `generateSequence` calls nextEntry until it returns null (end of ZIP).
            for (entry in generateSequence { zip.nextEntry }) {
                when {
                    entry.isDirectory -> Unit
                    entry.name == DATA_ENTRY -> data = decodeBackup(zip.readBytes().toString(Charsets.UTF_8))
                    isSafePhotoEntry(entry.name) -> photos[entry.name.removePrefix(PHOTOS_DIR)] = zip.readBytes()
                }
            }
        }
    } catch (e: java.io.IOException) {
        throw BackupFormatException("Can't read the file: ${e.message}")
    }
    return BackupContents(data ?: throw BackupFormatException("No data.json in the file"), photos)
}
