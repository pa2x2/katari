package mihon.desktop.source

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.source.EntrySourceRefreshRequest
import mihon.entry.interactions.source.EntrySourceRefreshResult

/**
 * Refreshing from sources as the shared feature answers while an entry's source isn't installed, which on desktop is
 * every source: nothing is fetched and the entry screen says the source is missing. Stands in for the shared feature,
 * which reaches the download and merge features desktop doesn't host yet, until desktop loads extensions.
 */
@Inject
class NoEntrySourceRefreshFeature : EntrySourceRefreshFeature {
    override suspend fun refresh(request: EntrySourceRefreshRequest): EntrySourceRefreshResult =
        EntrySourceRefreshResult.SourceUnavailable(request.entry.source)
}
