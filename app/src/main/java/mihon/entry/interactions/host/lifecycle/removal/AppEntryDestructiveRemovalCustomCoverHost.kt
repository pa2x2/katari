package mihon.entry.interactions.host.lifecycle.removal

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.util.removeCovers
import mihon.entry.interactions.EntryDestructiveRemovalCustomCoverHost
import tachiyomi.domain.entry.model.Entry

class AppEntryDestructiveRemovalCustomCoverHost(
    private val coverCache: CoverCache,
) : EntryDestructiveRemovalCustomCoverHost {
    override suspend fun removeCustomCover(entry: Entry) {
        entry.removeCovers(coverCache)
    }
}
