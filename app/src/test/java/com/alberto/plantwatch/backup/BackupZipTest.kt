package com.alberto.plantwatch.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupZipTest {

    // A JUnit rule: a fresh temporary folder per test, deleted afterwards.
    @get:Rule val tmp = TemporaryFolder()

    private val data = BackupData(schemaVersion = 6, exportedAt = "2026-11-05T10:00:00", settings = BackupSettings(540))

    @Test
    fun `data and photos come back from the ZIP, unsafe entries are skipped`() {
        val photo = byteArrayOf(1, 2, 3)
        val file = tmp.newFile("plant-1-1.jpg").apply { writeBytes(photo) }
        val out = ByteArrayOutputStream()
        writeBackupZip(out, data, listOf(file))
        // Add an entry trying to escape the photos folder ("zip slip").
        val withEvil = ByteArrayOutputStream()
        ZipOutputStream(withEvil).use { zip ->
            java.util.zip.ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { src ->
                for (e in generateSequence { src.nextEntry }) {
                    zip.putNextEntry(ZipEntry(e.name)); src.copyTo(zip); zip.closeEntry()
                }
            }
            zip.putNextEntry(ZipEntry("photos/../../evil.jpg")); zip.write(9); zip.closeEntry()
        }

        val back = readBackupZip(ByteArrayInputStream(withEvil.toByteArray()))

        assertEquals(data, back.data)
        assertEquals(setOf("plant-1-1.jpg"), back.photos.keys)
        assertArrayEquals(photo, back.photos.getValue("plant-1-1.jpg"))
    }

    @Test(expected = BackupFormatException::class)
    fun `a ZIP without data json is refused`() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { it.putNextEntry(ZipEntry("other.txt")); it.write(1); it.closeEntry() }
        readBackupZip(ByteArrayInputStream(out.toByteArray()))
    }
}
