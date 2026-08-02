package mihon.entry.interactions.manga.page

import android.content.Context
import android.text.format.Formatter
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import logcat.LogPriority
import mihon.entry.interactions.runtime.EntryPageImageCache
import okhttp3.Response
import okio.buffer
import okio.sink
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import java.io.File
import java.io.IOException

internal class MangaPageStore(
    private val context: Context,
    private val json: Json,
) : EntryPageImageCache {

    private val diskCache = DiskLruCache.open(
        File(context.cacheDir, "chapter_disk_cache"),
        PARAMETER_APP_VERSION,
        PARAMETER_VALUE_COUNT,
        PARAMETER_CACHE_SIZE,
    )
    private val entryLocksGuard = Mutex()
    private val entryLocks = mutableMapOf<String, CacheEntryLock>()

    override val readableSize: String
        get() = Formatter.formatFileSize(context, DiskUtil.getDirectorySize(diskCache.directory))

    fun getPageListFromCache(chapter: Chapter): List<Page> {
        val key = DiskUtil.hashKeyForDisk(getKey(chapter))
        return diskCache.get(key).use {
            json.decodeFromString(it.getString(0))
        }
    }

    suspend fun getOrPutPageList(
        chapter: Chapter,
        fetch: suspend () -> List<Page>,
    ): List<Page> {
        return withCacheEntryLock("page-list:${getKey(chapter)}") {
            try {
                getPageListFromCache(chapter)
            } catch (_: Exception) {
                fetch().also { putPageListToCache(chapter, it) }
            }
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

    override fun isImageInCache(imageUrl: String): Boolean {
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

    override fun getImageFile(imageUrl: String): File {
        val imageName = DiskUtil.hashKeyForDisk(imageUrl) + ".0"
        return File(diskCache.directory, imageName)
    }

    suspend fun getOrPutImage(
        imageUrl: String,
        force: Boolean,
        onFetch: () -> Unit = {},
        fetch: suspend () -> Response,
    ): File {
        return withCacheEntryLock("image:$imageUrl") {
            if (!force && isImageInCache(imageUrl)) return@withCacheEntryLock getImageFile(imageUrl)
            onFetch()
            val response = fetch()
            val coroutineContext = currentCoroutineContext()
            runInterruptible {
                putImageToCache(imageUrl, response, coroutineContext)
            }
            getImageFile(imageUrl)
        }
    }

    override fun clear(): Int {
        var deletedFiles = 0
        diskCache.directory.listFiles()?.forEach { file ->
            if (file.name != "journal" && !file.name.startsWith("journal.")) {
                try {
                    val key = file.name.substringBeforeLast(".")
                    if (diskCache.remove(key)) deletedFiles++
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "Failed to remove page cache entry" }
                }
            }
        }
        return deletedFiles
    }

    internal fun close() {
        diskCache.close()
    }

    private fun putImageToCache(
        imageUrl: String,
        response: Response,
        coroutineContext: kotlin.coroutines.CoroutineContext,
    ) {
        var editor: DiskLruCache.Editor? = null

        try {
            val key = DiskUtil.hashKeyForDisk(imageUrl)
            editor = diskCache.edit(key) ?: return

            response.body.source().use { input ->
                editor.newOutputStream(0).sink().buffer().use { output ->
                    while (true) {
                        coroutineContext.ensureActive()
                        val bytesRead = input.read(output.buffer, DOWNLOAD_COPY_BUFFER_SIZE)
                        if (bytesRead == -1L) break
                        output.emitCompleteSegments()
                    }
                    output.flush()
                }
            }
            coroutineContext.ensureActive()
            diskCache.flush()
            editor.commit()
        } finally {
            response.close()
            editor?.abortUnlessCommitted()
        }
    }

    private fun getKey(chapter: Chapter): String {
        return "${chapter.mangaId}${chapter.url}"
    }

    private suspend fun <T> withCacheEntryLock(key: String, block: suspend () -> T): T {
        val lock = entryLocksGuard.withLock {
            entryLocks.getOrPut(key, ::CacheEntryLock).also { it.references++ }
        }
        try {
            return lock.mutex.withLock { block() }
        } finally {
            entryLocksGuard.withLock {
                lock.references--
                if (lock.references == 0) entryLocks.remove(key, lock)
            }
        }
    }

    private class CacheEntryLock(
        val mutex: Mutex = Mutex(),
        var references: Int = 0,
    )
}

private const val PARAMETER_APP_VERSION = 1
private const val PARAMETER_VALUE_COUNT = 1
private const val PARAMETER_CACHE_SIZE = 100L * 1024 * 1024
private const val DOWNLOAD_COPY_BUFFER_SIZE = 8_192L
