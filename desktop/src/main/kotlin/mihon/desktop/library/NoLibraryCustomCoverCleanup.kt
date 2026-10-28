package mihon.desktop.library

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import tachiyomi.domain.entry.model.Entry

/** No entry leaves the library on desktop yet, so no custom cover is left behind to clean up. */
@Inject
class NoLibraryCustomCoverCleanup : EntryLibraryCustomCoverHost {
    override suspend fun cleanupAfterLibraryRemoval(entry: Entry) = Unit
}
