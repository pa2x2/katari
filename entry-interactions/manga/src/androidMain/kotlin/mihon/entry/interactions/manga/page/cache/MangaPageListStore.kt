package mihon.entry.interactions.manga.page.cache

import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okio.buffer
import okio.sink
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter

internal class MangaPageListStore(
    private val diskCache: DiskLruCache,
    private val json: Json,
    private val entryLocks: MangaPageCacheEntryLocks,
) {
    fun get(chapter: Chapter): List<Page> {
        val key = DiskUtil.hashKeyForDisk(chapter.key())
        return diskCache.get(key).use {
            json.decodeFromString(it.getString(0))
        }
    }

    suspend fun getOrPut(
        chapter: Chapter,
        fetch: suspend () -> List<Page>,
    ): List<Page> {
        return entryLocks.withLock("page-list:${chapter.key()}") {
            val cached = try {
                get(chapter)
            } catch (_: Exception) {
                null
            }
            if (!cached.isNullOrEmpty()) {
                cached
            } else {
                if (cached != null) remove(chapter)
                fetch().also { pages ->
                    if (pages.isNotEmpty()) put(chapter, pages)
                }
            }
        }
    }

    fun put(chapter: Chapter, pages: List<Page>) {
        if (pages.isEmpty()) return
        var editor: DiskLruCache.Editor? = null
        try {
            val key = DiskUtil.hashKeyForDisk(chapter.key())
            editor = diskCache.edit(key) ?: return
            editor.newOutputStream(0).sink().buffer().use {
                it.write(json.encodeToString(pages).toByteArray())
                it.flush()
            }
            editor.commit()
            diskCache.flush()
        } catch (error: Exception) {
            logcat(LogPriority.WARN, error) { "Failed to put page list to cache" }
        } finally {
            editor?.abortUnlessCommitted()
        }
    }

    private fun remove(chapter: Chapter) {
        try {
            diskCache.remove(DiskUtil.hashKeyForDisk(chapter.key()))
            diskCache.flush()
        } catch (error: Exception) {
            logcat(LogPriority.WARN, error) { "Failed to remove empty page list from cache" }
        }
    }

    private fun Chapter.key(): String = "$mangaId$url"
}
