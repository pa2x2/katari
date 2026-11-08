package app.katari.entry.interactions.manga.page

import app.katari.core.platform.storage.AppPrivateDirectories
import app.katari.entry.interactions.manga.page.cache.MangaPageCacheEntryLocks
import app.katari.entry.interactions.manga.page.cache.MangaPageImageStore
import app.katari.entry.interactions.manga.page.cache.MangaPageListStore
import app.katari.entry.interactions.manga.page.cache.MangaPageStagingWrite
import app.katari.entry.interactions.runtime.EntryPageImageCache
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okhttp3.Response
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import java.io.File

internal class MangaPageStore(
    private val directories: AppPrivateDirectories,
    json: Json,
) : EntryPageImageCache {
    // Opening reads the journal from disk, and the store is built at startup long before a reader needs it
    private val diskCacheLazy = lazy {
        DiskLruCache.open(
            File(directories.cache, "chapter_disk_cache"),
            PARAMETER_APP_VERSION,
            PARAMETER_VALUE_COUNT,
            PARAMETER_CACHE_SIZE,
        )
    }
    private val diskCache by diskCacheLazy
    private val entryLocks = MangaPageCacheEntryLocks()
    private val pageLists by lazy { MangaPageListStore(diskCache, json, entryLocks) }
    private val images by lazy { MangaPageImageStore(diskCache, entryLocks) }

    override val sizeBytes: Long
        get() = diskCache.directory.walk().filter(File::isFile).sumOf(File::length)

    suspend fun getOrPutPageList(
        chapter: Chapter,
        fetch: suspend () -> List<Page>,
    ): List<Page> = pageLists.getOrPut(chapter, fetch)

    fun putPageListToCache(chapter: Chapter, pages: List<Page>) {
        pageLists.put(chapter, pages)
    }

    override fun isImageInCache(imageUrl: String): Boolean = images.getCommitted(imageUrl) != null

    fun getCommittedImage(imageUrl: String): File? = images.getCommitted(imageUrl)

    override fun getImageFile(imageUrl: String): File = images.file(imageUrl)

    suspend fun getOrPutImage(
        imageUrl: String,
        force: Boolean,
        onFetch: () -> Unit = {},
        fetch: suspend () -> Response,
    ): File = images.getOrPut(imageUrl, force, onFetch, fetch)

    fun beginImageWrite(imageUrl: String): MangaPageStagingWrite = images.beginWrite(imageUrl)

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
        if (diskCacheLazy.isInitialized()) diskCache.close()
    }
}

private const val PARAMETER_APP_VERSION = 1
private const val PARAMETER_VALUE_COUNT = 1
private const val PARAMETER_CACHE_SIZE = 100L * 1024 * 1024
