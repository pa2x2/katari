package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okhttp3.Response
import okio.buffer
import okio.sink
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import java.io.File
import java.io.IOException

internal class ReaderPageCache(
    context: Context,
    private val json: Json,
) {

    private val diskCache = DiskLruCache.open(
        File(context.cacheDir, "chapter_disk_cache"),
        PARAMETER_APP_VERSION,
        PARAMETER_VALUE_COUNT,
        PARAMETER_CACHE_SIZE,
    )

    fun getPageListFromCache(chapter: Chapter): List<Page> {
        val key = DiskUtil.hashKeyForDisk(getKey(chapter))
        return diskCache.get(key).use {
            json.decodeFromString(it.getString(0))
        }
    }

    fun putPageListToCache(chapter: Chapter, pages: List<Page>) {
        var editor: DiskLruCache.Editor? = null

        try {
            val key = DiskUtil.hashKeyForDisk(getKey(chapter))
            editor = diskCache.edit(key) ?: return

            editor.newOutputStream(0).sink().buffer().use {
                it.write(json.encodeToString(pages).toByteArray())
                it.flush()
            }

            diskCache.flush()
            editor.commit()
            editor.abortUnlessCommitted()
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Failed to put page list to cache" }
        } finally {
            editor?.abortUnlessCommitted()
        }
    }

    fun isImageInCache(imageUrl: String): Boolean {
        return try {
            val key = DiskUtil.hashKeyForDisk(imageUrl)
            val inJournal = diskCache.get(key).use { it != null }
            val fileExists = getImageFile(imageUrl).exists()
            if (inJournal && !fileExists) {
                logcat(LogPriority.WARN) { "Image is in journal but file is missing: $imageUrl" }
            }
            inJournal && fileExists
        } catch (_: IOException) {
            false
        }
    }

    fun getImageFile(imageUrl: String): File {
        val imageName = DiskUtil.hashKeyForDisk(imageUrl) + ".0"
        return File(diskCache.directory, imageName)
    }

    @Throws(IOException::class)
    fun putImageToCache(imageUrl: String, response: Response) {
        var editor: DiskLruCache.Editor? = null

        try {
            val key = DiskUtil.hashKeyForDisk(imageUrl)
            editor = diskCache.edit(key) ?: return

            response.body.source().use { input ->
                editor.newOutputStream(0).sink().buffer().use {
                    it.writeAll(input)
                    it.flush()
                }
            }
            diskCache.flush()
            editor.commit()
        } finally {
            response.body.close()
            editor?.abortUnlessCommitted()
        }
    }

    private fun getKey(chapter: Chapter): String {
        return "${chapter.mangaId}${chapter.url}"
    }
}

private const val PARAMETER_APP_VERSION = 1
private const val PARAMETER_VALUE_COUNT = 1
private const val PARAMETER_CACHE_SIZE = 100L * 1024 * 1024
