package tachiyomi.domain.entry.service

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One lock per entry around reading the next part of its chapter list, so the background walk and a feature that
 * finishes the walk itself, such as migration, never read and save the same part twice.
 */
@Inject
@SingleIn(AppScope::class)
class ChapterListWalkLocks {
    private val locks = mutableMapOf<Long, Mutex>()

    suspend fun <T> withLock(entryId: Long, block: suspend () -> T): T {
        val lock = synchronized(locks) { locks.getOrPut(entryId, ::Mutex) }
        return lock.withLock { block() }
    }
}
