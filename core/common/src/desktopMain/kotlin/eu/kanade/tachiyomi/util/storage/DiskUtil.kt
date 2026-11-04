package eu.kanade.tachiyomi.util.storage

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.lang.Hash
import tachiyomi.core.common.storage.StorageFileNames
import java.io.File
import java.io.IOException
import java.nio.file.FileStore
import java.nio.file.Files

/**
 * Android's disk helpers on the desktop's file systems. Desktop has no UniFile, so the members taking one can't be
 * reached there, and no media scanner or removable storages the app knows of.
 */
object DiskUtil {

    fun getExternalStorages(context: Context): List<File> = emptyList()

    fun hashKeyForDisk(key: String): String {
        return Hash.md5(key)
    }

    fun getDirectorySize(f: File): Long {
        var size: Long = 0
        if (f.isDirectory) {
            for (file in f.listFiles().orEmpty()) {
                size += getDirectorySize(file)
            }
        } else {
            size = f.length()
        }
        return size
    }

    /**
     * Gets the total space for the disk that a file path points to, in bytes.
     */
    fun getTotalStorageSpace(file: File): Long = fileStoreOf(file)?.totalSpace ?: -1L

    /**
     * Gets the available space for the disk that a file path points to, in bytes.
     */
    fun getAvailableStorageSpace(file: File): Long = fileStoreOf(file)?.usableSpace ?: -1L

    /**
     * Gets the available space for the disk that a file path points to, in bytes.
     */
    fun getAvailableStorageSpace(f: UniFile): Long {
        val path = f.uri.path ?: return -1L
        return getAvailableStorageSpace(File(path))
    }

    /**
     * Don't display downloaded chapters in gallery apps creating `.nomedia`.
     */
    fun createNoMediaFile(dir: UniFile?, context: Context?) {
        if (dir != null && dir.exists()) {
            val nomedia = dir.findFile(NOMEDIA_FILE)
            if (nomedia == null) {
                dir.createFile(NOMEDIA_FILE)
                context?.let { scanMedia(it, dir.uri) }
            }
        }
    }

    /** Desktop has no media index to tell about new files. */
    fun scanMedia(context: Context, uri: Uri) = Unit

    private fun fileStoreOf(file: File): FileStore? = try {
        Files.getFileStore(file.toPath())
    } catch (_: IOException) {
        null
    }

    // Extensions built against the SDK name files through these; the app uses StorageFileNames.
    fun buildValidFilename(
        origName: String,
        maxBytes: Int = MAX_FILE_NAME_BYTES,
        disallowNonAscii: Boolean = false,
    ): String = StorageFileNames.buildValidFilename(origName, maxBytes, disallowNonAscii)

    fun truncateToLength(s: String, maxBytes: Int): String = StorageFileNames.truncateToLength(s, maxBytes)

    const val NOMEDIA_FILE = StorageFileNames.NOMEDIA_FILE

    const val MAX_FILE_NAME_BYTES = StorageFileNames.MAX_FILE_NAME_BYTES
}
