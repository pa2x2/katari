package mihon.entry.interactions.manga.page

import android.content.Context
import android.text.format.Formatter
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.serialization.json.Json
import logcat.LogPriority
import mihon.entry.interactions.manga.page.cache.MangaPageCacheEntryLocks
import mihon.entry.interactions.manga.page.cache.MangaPageImageStore
import mihon.entry.interactions.manga.page.cache.MangaPageListStore
import mihon.entry.interactions.manga.page.cache.MangaPageStagingWrite
import mihon.entry.interactions.runtime.EntryPageImageCache
import okhttp3.Response
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import java.io.File

internal class MangaPageStore(
    private val context: Context,
    json: Json,
) : EntryPageImageCache {
    private val diskCache = DiskLruCache.open(
        File(context.cacheDir, "chapter_disk_cache"),
        PARAMETER_APP_VERSION,
        PARAMETER_VALUE_COUNT,
        PARAMETER_CACHE_SIZE,
    )
    private val entryLocks = MangaPageCacheEntryLocks()
    private val pageLists = MangaPageListStore(diskCache, json, entryLocks)
    private val images = MangaPageImageStore(diskCache, entryLocks)

    override val readableSize: String
        get() = Formatter.formatFileSize(context, DiskUtil.getDirectorySize(diskCache.directory))

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
        diskCache.close()
    }
}

private const val PARAMETER_APP_VERSION = 1
private const val PARAMETER_VALUE_COUNT = 1
private const val PARAMETER_CACHE_SIZE = 100L * 1024 * 1024
