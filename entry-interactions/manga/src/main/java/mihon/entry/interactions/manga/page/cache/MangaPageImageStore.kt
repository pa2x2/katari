package mihon.entry.interactions.manga.page.cache

import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import logcat.LogPriority
import okhttp3.Response
import okio.buffer
import okio.sink
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.io.IOException

internal class MangaPageImageStore(
    private val diskCache: DiskLruCache,
    private val entryLocks: MangaPageCacheEntryLocks,
) {
    fun getCommitted(imageUrl: String): File? {
        return try {
            val key = DiskUtil.hashKeyForDisk(imageUrl)
            val inJournal = diskCache.get(key).use { it != null }
            val imageFile = file(imageUrl)
            val fileExists = imageFile.exists()
            if (inJournal && !fileExists) {
                logcat(LogPriority.WARN) { "Image is in journal but file is missing: $imageUrl" }
            }
            imageFile.takeIf { inJournal && fileExists }
        } catch (_: IOException) {
            null
        }
    }

    fun file(imageUrl: String): File {
        val imageName = DiskUtil.hashKeyForDisk(imageUrl) + ".0"
        return File(diskCache.directory, imageName)
    }

    suspend fun getOrPut(
        imageUrl: String,
        force: Boolean,
        onFetch: () -> Unit,
        fetch: suspend () -> Response,
    ): File {
        return entryLocks.withLock("image:$imageUrl") {
            if (!force) getCommitted(imageUrl)?.let { return@withLock it }
            onFetch()
            val response = fetch()
            val coroutineContext = currentCoroutineContext()
            runInterruptible {
                response.use {
                    beginWrite(imageUrl).use { stagingWrite ->
                        val buffer = ByteArray(DOWNLOAD_COPY_BUFFER_SIZE)
                        response.body.source().use { input ->
                            while (true) {
                                coroutineContext.ensureActive()
                                val bytesRead = input.read(buffer)
                                if (bytesRead == -1) break
                                stagingWrite.write(buffer, bytesRead)
                            }
                        }
                        coroutineContext.ensureActive()
                        stagingWrite.commit()
                    }
                }
            }
        }
    }

    fun beginWrite(imageUrl: String): MangaPageStagingWrite {
        val key = DiskUtil.hashKeyForDisk(imageUrl)
        val editor = diskCache.edit(key)
            ?: throw IOException("Page cache entry is already being edited")
        return try {
            MangaPageStagingWrite(
                output = editor.newOutputStream(0).sink().buffer(),
                commitAction = {
                    editor.commit()
                    diskCache.flush()
                    file(imageUrl)
                },
                abortAction = editor::abortUnlessCommitted,
            )
        } catch (error: Throwable) {
            editor.abortUnlessCommitted()
            throw error
        }
    }
}

private const val DOWNLOAD_COPY_BUFFER_SIZE = 8_192
