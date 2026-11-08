package app.katari.entry.interactions.host.lifecycle.removal

import app.katari.entry.interactions.host.removeCovers
import app.katari.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalCustomCoverHost
import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.entry.model.Entry

class AppEntryDestructiveRemovalCustomCoverHost(
    private val coverCache: CoverCache,
) : EntryDestructiveRemovalCustomCoverHost {
    override suspend fun removeCustomCover(entry: Entry) {
        entry.removeCovers(coverCache)
    }
}
