package app.katari.sync.host

import app.katari.entry.interactions.download.EntryDownloadLifecycleEvent
import app.katari.entry.interactions.download.EntryDownloadLifecycleEventSink
import app.katari.sync.client.readelsewhere.ChaptersReadElsewhere
import app.katari.sync.client.readelsewhere.ChaptersReadElsewhereListener
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.repository.EntryRepository

/** Chapters other devices read reach the download settings that act after reading, as reads here do. */
@Inject
@ContributesIntoSet(AppScope::class)
class SyncedReadsDeleteDownloads(
    private val entries: EntryRepository,
    private val downloads: EntryDownloadLifecycleEventSink,
) : ChaptersReadElsewhereListener {
    override suspend fun onReadElsewhere(reads: List<ChaptersReadElsewhere>) {
        reads.forEach { read ->
            val entry = entries.getEntryById(read.entryId) ?: return@forEach
            downloads.onEvent(EntryDownloadLifecycleEvent.ConsumedElsewhere(entry, read.chapterIds))
        }
    }
}
