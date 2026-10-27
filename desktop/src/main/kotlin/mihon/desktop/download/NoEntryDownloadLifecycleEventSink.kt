package mihon.desktop.download

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.download.EntryDownloadLifecycleEvent
import mihon.entry.interactions.download.EntryDownloadLifecycleEventSink
import mihon.entry.interactions.download.EntryDownloadLifecycleResult

/**
 * Download clean-up and download-ahead after reading, as for a type without downloads: every event is inapplicable.
 * Stands in for the shared download lifecycle, which arrives with the rest of the shared download features.
 */
@Inject
class NoEntryDownloadLifecycleEventSink : EntryDownloadLifecycleEventSink {
    override suspend fun onEvent(event: EntryDownloadLifecycleEvent): EntryDownloadLifecycleResult =
        EntryDownloadLifecycleResult.Inapplicable(event.visibleEntry.type)
}
