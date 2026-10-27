package mihon.desktop.download

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadRuntimeState
import mihon.entry.interactions.download.EntryDownloadStatus
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/**
 * The download queue as a feature no entry type contributes to: it stays empty and idle, and nothing is downloaded.
 * Stands in for the shared download runtime, which desktop can host once it has entry types to download and the
 * application services their features contribute to (startup, backups, merge and migration consequences).
 */
@Inject
class NoEntryDownloadRuntimeFeature : EntryDownloadRuntimeFeature {
    override val changes: Flow<Unit> = emptyFlow()

    override val state: Flow<EntryDownloadRuntimeState> = flowOf(EntryDownloadRuntimeState())

    override fun isApplicable(type: EntryType): Boolean = false

    override fun statusUpdates(): Flow<EntryDownloadStatus> = emptyFlow()

    override fun start() = Unit

    override fun pause() = Unit

    override fun clearQueue() = Unit

    override fun reorderQueue(items: List<EntryDownloadQueueItem>) = Unit

    override fun reorderEntry(type: EntryType, entryId: Long, moveToTop: Boolean) = Unit

    override fun cancelQueued(items: List<EntryDownloadQueueItem>) = Unit

    override fun cancelQueued(type: EntryType, childId: Long): EntryDownloadStatus? = null

    override fun downloadCount(entry: Entry): Int = 0

    override fun downloadCounts(entries: List<Entry>): Map<Long, Int> = emptyMap()

    override fun totalDownloadCount(): Int = 0

    override fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean): Boolean = false

    override fun status(
        type: EntryType,
        childId: Long,
        childName: String,
        childScanlator: String?,
        childUrl: String,
        entryTitle: String,
        sourceId: Long,
    ): EntryDownloadStatus? = null

    override fun statuses(entry: Entry, chapters: List<EntryChapter>): Map<Long, EntryDownloadStatus>? = null
}
