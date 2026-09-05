package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned access to the shared download queue and its persisted runtime state. */
interface EntryDownloadRuntimeFeature {
    /** Emits when persisted downloaded content changes. Queue changes are exposed by [state] and [statusUpdates]. */
    val changes: Flow<Unit>

    /**
     * Current queue membership, ordering, phase and progress, together with runtime flags.
     * A collector needs only this stream to render the queue, including when it subscribes mid-transfer.
     */
    val state: Flow<EntryDownloadRuntimeState>

    fun isApplicable(type: EntryType): Boolean

    fun statusUpdates(): Flow<EntryDownloadStatus>

    fun start()
    fun pause()
    fun clearQueue()
    fun reorderQueue(items: List<EntryDownloadQueueItem>)
    fun reorderEntry(type: EntryType, entryId: Long, moveToTop: Boolean)
    fun cancelQueued(items: List<EntryDownloadQueueItem>)
    fun cancelQueued(type: EntryType, childId: Long): EntryDownloadStatus?

    fun downloadCount(entry: Entry): Int
    fun totalDownloadCount(): Int
    fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean = false): Boolean

    /** Returns null when the Entry type does not contribute the download runtime. */
    fun status(
        type: EntryType,
        childId: Long,
        childName: String,
        childScanlator: String?,
        childUrl: String,
        entryTitle: String,
        sourceId: Long,
    ): EntryDownloadStatus?
}

data class EntryDownloadRuntimeState(
    val queue: List<EntryDownloadQueueGroup> = emptyList(),
    val isInitializing: Boolean = false,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
)
