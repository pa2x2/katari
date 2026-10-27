package mihon.entry.interactions.host.library

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.util.removeCovers
import mihon.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import tachiyomi.domain.entry.model.Entry

class AppEntryLibraryCustomCoverHost(
    private val coverCache: CoverCache,
) : EntryLibraryCustomCoverHost {
    override suspend fun cleanupAfterLibraryRemoval(entry: Entry) {
        entry.removeCovers(coverCache)
    }
}
