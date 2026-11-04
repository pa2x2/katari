package eu.kanade.tachiyomi.util.storage

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.core.content.ContextCompat
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.lang.Hash
import tachiyomi.core.common.storage.StorageFileNames
import java.io.File

object DiskUtil {

    /**
     * Returns the root folders of all the available external storages.
     */
    fun getExternalStorages(context: Context): List<File> {
        return ContextCompat.getExternalFilesDirs(context, null)
            .filterNotNull()
            .mapNotNull {
                val file = File(it.absolutePath.substringBefore("/Android/"))
                val state = Environment.getExternalStorageState(file)
                if (state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) {
                    file
                } else {
                    null
                }
            }
    }

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
    fun getTotalStorageSpace(file: File): Long {
        return try {
            val stat = StatFs(file.absolutePath)
            stat.blockCountLong * stat.blockSizeLong
        } catch (_: Exception) {
            -1L
        }
    }

    /**
     * Gets the available space for the disk that a file path points to, in bytes.
     */
    fun getAvailableStorageSpace(file: File): Long {
        return try {
            val stat = StatFs(file.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            -1L
        }
    }

    /**
     * Gets the available space for the disk that a file path points to, in bytes.
     */
    fun getAvailableStorageSpace(f: UniFile): Long {
        return try {
            val stat = StatFs(f.uri.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            -1L
        }
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

    /**
     * Scans the given file so that it can be shown in gallery apps, for example.
     */
    fun scanMedia(context: Context, uri: Uri) {
        MediaScannerConnection.scanFile(context, arrayOf(uri.path), null, null)
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
