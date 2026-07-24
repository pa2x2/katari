package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryDownloadInteraction {
    val changes: Flow<Unit>
    val isInitializing: Flow<Boolean>
    val isRunning: Flow<Boolean>
    val isPaused: Flow<Boolean>
    val queueState: Flow<List<EntryDownloadQueueGroup>>

    fun updates(): Flow<EntryDownloadStatus>
    fun queueStatusUpdates(): Flow<EntryDownloadQueueItem>
    fun queueProgressUpdates(): Flow<EntryDownloadQueueItem>
    fun events(): Flow<EntryDownloadEvent>

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
    suspend fun resolveDownloadOptions(context: Context, entry: Entry, chapter: EntryChapter): EntryDownloadOptions?
    suspend fun resolveBulkDownloadCandidatePool(
        entry: Entry,
        candidates: List<EntryChapter>? = null,
    ): List<EntryChapter>
    suspend fun delete(entry: Entry, chapters: List<EntryChapter>)
    suspend fun cleanup(entry: Entry, chapters: List<EntryChapter>)
    suspend fun deleteEntryDownloads(entry: Entry): Boolean

    fun hasDownloads(entry: Entry): Boolean
    fun getDownloadCount(entry: Entry): Int
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
    fun cancelQueuedDownload(entryType: EntryType, chapterId: Long): EntryDownloadStatus?
}
