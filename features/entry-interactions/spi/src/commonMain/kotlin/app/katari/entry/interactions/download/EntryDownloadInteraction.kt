package app.katari.entry.interactions.download

import app.katari.core.platform.PlatformContext
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryDownloadInteraction {
    /** Emits once every processor's persisted downloads can be read, then whenever any of them changes. */
    val changes: Flow<Unit>
    val isInitializing: Flow<Boolean>
    val isRunning: Flow<Boolean>
    val isPaused: Flow<Boolean>
    val queueState: Flow<List<EntryDownloadQueueGroup>>

    fun updates(): Flow<EntryDownloadStatus>
    fun events(): Flow<EntryDownloadEvent>

    suspend fun hasPendingDownloads(): Boolean

    /** Runs every media-specific downloader until its current queue is idle. Runtime use only. */
    suspend fun runDownloadsUntilIdle()

    fun startDownloads()
    fun pauseDownloads()
    fun clearQueue()
    fun invalidateCaches()
    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource)
    suspend fun renameEntry(entry: Entry, newTitle: String)
    fun reorderQueue(items: List<EntryDownloadQueueItem>)
    fun reorderSeries(entryType: EntryType, entryId: Long, moveToTop: Boolean)
    fun cancelQueuedDownloads(items: List<EntryDownloadQueueItem>)

    suspend fun queue(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean = true)
    suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean = false)
    suspend fun downloadWithOptions(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean = false,
    )
    suspend fun resolveDownloadOptions(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter,
        selection: EntryDownloadOptionSelection?,
    ): EntryDownloadOptions?
    suspend fun resolveBulkDownloadCandidatePool(
        entry: Entry,
        candidates: List<EntryChapter>? = null,
    ): List<EntryChapter>
    suspend fun delete(entry: Entry, chapters: List<EntryChapter>)
    suspend fun cleanup(entry: Entry, chapters: List<EntryChapter>)
    suspend fun deleteEntryDownloads(entry: Entry): Boolean

    fun hasDownloads(entry: Entry): Boolean

    /** Returns those of [entries] that [hasDownloads], looking up the downloads once per type. */
    fun filterWithDownloads(entries: List<Entry>): List<Entry>

    fun getDownloadedChapterIds(entry: Entry, chapters: List<EntryChapter>): Set<Long>

    /** Does nothing for a type without downloads. */
    suspend fun moveRenamedDownload(entry: Entry, former: EntryChapter, chapter: EntryChapter)
    fun getTotalDownloadCount(): Int
    fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean = false): Boolean
    fun getStatus(
        entryType: EntryType,
        chapterId: Long,
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        entryTitle: String,
        sourceId: Long,
    ): EntryDownloadStatus
    fun getStatuses(entry: Entry, chapters: List<EntryChapter>): Map<Long, EntryDownloadStatus>
    fun cancelQueuedDownload(entryType: EntryType, chapterId: Long): EntryDownloadStatus?
}
