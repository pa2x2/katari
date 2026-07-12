package eu.kanade.tachiyomi.entry

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.util.removeCovers
import tachiyomi.domain.entry.model.Entry

fun interface EntryRemovalCleanupInteraction {
    fun cleanupAfterLibraryRemoval(entry: Entry)
}

class AppEntryRemovalCleanupInteraction(
    private val coverCache: CoverCache,
) : EntryRemovalCleanupInteraction {

    override fun cleanupAfterLibraryRemoval(entry: Entry) {
        entry.removeCovers(coverCache)
    }
}
