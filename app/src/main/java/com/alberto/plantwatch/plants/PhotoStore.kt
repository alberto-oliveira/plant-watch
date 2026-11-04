package com.alberto.plantwatch.plants

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import kotlin.math.max

/**
 * Plant photos, kept as resized JPEG copies in the app's private storage
 * ([dir], e.g. /data/data/com.alberto.plantwatch/files/photos). A copy survives
 * deleting the original from the gallery and can go into backups.
 *
 * Created once, in AppContainer. The functions do file work, so callers run
 * them off the main thread (Dispatchers.IO).
 */
class PhotoStore(val dir: File) {

    /** The file for a stored photo name. */
    fun fileFor(name: String): File = File(dir, name)

    /**
     * Copies the image at [uri] (from the photo picker) as a JPEG of at most
     * [MAX_SIDE] pixels on its longer side, and returns the new file's name.
     *
     * `ImageDecoder` (Android 9+) reads the image, applies its EXIF rotation (so
     * phone photos aren't sideways) and can shrink while decoding, which saves
     * memory compared with loading the full-size image first.
     *
     * The name includes the time, so a changed photo gets a new name and image
     * caches can't show the old one.
     */
    fun save(resolver: ContentResolver, uri: Uri, plantId: Long): String {
        val source = ImageDecoder.createSource(resolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longer = max(info.size.width, info.size.height)
            if (longer > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longer
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }
        dir.mkdirs()
        val name = "plant-$plantId-${System.currentTimeMillis()}.jpg"
        // `use` closes the stream even if writing fails.
        fileFor(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        return name
    }

    /** Deletes a stored photo, if there is one. */
    fun delete(name: String?) {
        if (name != null) fileFor(name).delete()
    }

    companion object {
        const val MAX_SIDE = 1024
        const val JPEG_QUALITY = 85
    }
}
