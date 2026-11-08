package app.katari.entry.interactions.host.library

import app.katari.entry.interactions.host.removeCovers
import app.katari.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.entry.model.Entry

class AppEntryLibraryCustomCoverHost(
    private val coverCache: CoverCache,
) : EntryLibraryCustomCoverHost {
    override suspend fun cleanupAfterLibraryRemoval(entry: Entry) {
        entry.removeCovers(coverCache)
    }
}
