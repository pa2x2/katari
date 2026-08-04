package mihon.entry.interactions.host.library

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.util.removeCovers
import mihon.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.Entry
import kotlin.time.Clock

class AppEntryLibraryCustomCoverHost(
    private val coverCache: CoverCache,
    private val handler: DatabaseHandler,
) : EntryLibraryCustomCoverHost {
    override suspend fun cleanupAfterLibraryRemoval(entry: Entry) {
        if (entry.removeCovers(coverCache) == entry) return
        handler.await {
            entriesQueries.touchCoverLastModified(
                coverLastModified = Clock.System.now().toEpochMilliseconds(),
                profileId = entry.profileId,
                entryId = entry.id,
            )
        }
    }
}
