package mihon.entry.interactions.host.lifecycle.profile

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.util.removeCovers
import mihon.entry.interactions.EntryProfileMoveCustomCoverHost
import tachiyomi.domain.entry.model.Entry

class AppEntryProfileMoveCustomCoverHost(
    private val coverCache: CoverCache,
) : EntryProfileMoveCustomCoverHost {
    override suspend fun removeCustomCovers(entries: List<Entry>) {
        entries.forEach { entry -> entry.removeCovers(coverCache) }
    }
}
