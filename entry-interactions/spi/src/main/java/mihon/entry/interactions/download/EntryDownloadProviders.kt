package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import mihon.feature.graph.CapabilityId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryDownloadProcessor : EntryInteractionProvider {
    val changes: Flow<Unit>
    val isInitializing: Flow<Boolean>
    val isRunning: Flow<Boolean>
    val queueState: Flow<List<EntryDownloadQueueGroup>>
    val events: Flow<EntryDownloadEvent>

    fun updates(): Flow<EntryDownloadStatus>
    fun queueStatusUpdates(): Flow<EntryDownloadQueueItem>
    fun queueProgressUpdates(): Flow<EntryDownloadQueueItem>

    /** Runs this media-specific downloader until its current queue is idle. */
    suspend fun runDownloadsUntilIdle()

    fun startDownloads()
    fun pauseDownloads()
    fun clearQueue()
    fun invalidateCache()
    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource)
    suspend fun renameEntry(entry: Entry, newTitle: String) = Unit

    /** Reorders pending work without interrupting an unrelated active download. */
    fun reorderQueue(items: List<EntryDownloadQueueItem>)
    fun reorderSeries(entryId: Long, moveToTop: Boolean)

    /** Cancels only the selected work. Pending-item cancellation must not restart active work. */
    fun cancelQueuedDownloads(items: List<EntryDownloadQueueItem>)

    /** Adds work to the queue and starts processing when [autoStart] is true. */
    suspend fun queue(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean)

    /**
     * Adds work and starts processing it. When [startNow] is true, the new work is promoted ahead of
     * other pending work without interrupting an active download.
     */
    suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean)
    suspend fun delete(entry: Entry, chapters: List<EntryChapter>)
    suspend fun cleanup(entry: Entry, chapters: List<EntryChapter>) = delete(entry, chapters)

    /** Returns true only when the owned storage is absent after deletion. */
    suspend fun deleteEntryDownloads(entry: Entry): Boolean

    fun hasDownloads(entry: Entry): Boolean
    fun getDownloadCount(entry: Entry): Int
    fun getTotalDownloadCount(): Int
    fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean = false): Boolean
    fun getStatus(
        chapterId: Long,
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        entryTitle: String,
        sourceId: Long,
    ): EntryDownloadStatus
    fun cancelQueuedDownload(chapterId: Long): EntryDownloadStatus?
}

interface EntryDownloadOptionsProcessor : EntryInteractionProvider {
    suspend fun downloadWithOptions(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean,
    )

    suspend fun resolveDownloadOptions(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
    ): EntryDownloadOptions?
}

/** Marker implemented by a type that provides one or more specialized download-setting behaviors. */
interface EntryDownloadSettingProvider : EntryInteractionProvider

interface EntryBulkDownloadCandidateProcessor : EntryInteractionProvider {
    /** Loads media-specific candidates before shared bulk-action selection is applied. */
    suspend fun resolveBulkDownloadCandidatePool(
        entry: Entry,
        candidates: List<EntryChapter>? = null,
    ): List<EntryChapter>
}

val EntryDownloadCapability = entryInteractionCapability<EntryDownloadProcessor>(
    id = CapabilityId("entry.download"),
)

val EntryDownloadOptionsCapability = entryInteractionCapability<EntryDownloadOptionsProcessor>(
    id = CapabilityId("entry.download.options"),
)

val EntryDownloadArchivePackagingCapability = entryInteractionCapability<EntryDownloadSettingProvider>(
    id = CapabilityId("entry.download.setting.archive-packaging"),
)

val EntryDownloadTallImageSplittingCapability = entryInteractionCapability<EntryDownloadSettingProvider>(
    id = CapabilityId("entry.download.setting.tall-image-splitting"),
)

val EntryDownloadParallelSourceTransfersCapability = entryInteractionCapability<EntryDownloadSettingProvider>(
    id = CapabilityId("entry.download.setting.parallel-source-transfers"),
)

val EntryDownloadParallelItemTransfersCapability = entryInteractionCapability<EntryDownloadSettingProvider>(
    id = CapabilityId("entry.download.setting.parallel-item-transfers"),
)

val EntryBulkDownloadCandidateCapability = entryInteractionCapability<EntryBulkDownloadCandidateProcessor>(
    id = CapabilityId("entry.download.bulk-candidates"),
)
