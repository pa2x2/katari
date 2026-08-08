package mihon.entry.interactions.manga.page.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MangaPageCacheEntryLocks {
    private val guard = Mutex()
    private val entries = mutableMapOf<String, CacheEntryLock>()

    suspend fun <T> withLock(key: String, block: suspend () -> T): T {
        val entry = guard.withLock {
            entries.getOrPut(key, ::CacheEntryLock).also { it.references++ }
        }
        try {
            return entry.mutex.withLock { block() }
        } finally {
            guard.withLock {
                entry.references--
                if (entry.references == 0) entries.remove(key, entry)
            }
        }
    }

    private class CacheEntryLock(
        val mutex: Mutex = Mutex(),
        var references: Int = 0,
    )
}
