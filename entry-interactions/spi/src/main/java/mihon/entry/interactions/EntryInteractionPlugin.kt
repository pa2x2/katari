package mihon.entry.interactions

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryOpenProcessor {
    val type: EntryType
    fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions)
    fun pendingIntent(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions): PendingIntent
}

interface EntryContinueProcessor {
    val type: EntryType
    suspend fun findNext(entry: Entry): EntryChapter?
    fun open(context: Context, entry: Entry, chapter: EntryChapter)
}

interface EntryDownloadProcessor {
    val type: EntryType
    val changes: Flow<Unit>
    val isInitializing: Flow<Boolean>
    val isRunning: Flow<Boolean>
    val queueState: Flow<List<EntryDownloadQueueGroup>>
    val events: Flow<EntryDownloadEvent>

    fun updates(): Flow<EntryDownloadStatus>
    fun queueStatusUpdates(): Flow<EntryDownloadQueueItem>
    fun queueProgressUpdates(): Flow<EntryDownloadQueueItem>

    fun startDownloads()
    fun pauseDownloads()
    fun clearQueue()
    fun invalidateCache()
    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource)
    suspend fun renameEntry(entry: Entry, newTitle: String) = Unit
    fun reorderQueue(items: List<EntryDownloadQueueItem>)
    fun reorderSeries(entryId: Long, moveToTop: Boolean)
    fun cancelQueuedDownloads(items: List<EntryDownloadQueueItem>)

    suspend fun queue(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean)
    suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean)
    suspend fun downloadWithOptions(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean,
    ) {
        download(entry, chapters, startNow)
    }
    fun supportsDownloadOptions(entry: Entry): Boolean = false
    suspend fun resolveDownloadOptions(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
    ): EntryDownloadOptions? = null
    fun supportsBulkDownload(entry: Entry): Boolean
    suspend fun resolveBulkDownloadCandidates(
        entry: Entry,
        action: EntryBulkDownloadAction,
        candidates: List<EntryChapter>? = null,
        memberEntryIds: List<Long> = emptyList(),
    ): EntryBulkDownloadCandidateResult
    suspend fun filterAutoDownloadCandidates(entry: Entry, chapters: List<EntryChapter>): List<EntryChapter>
    suspend fun delete(entry: Entry, chapters: List<EntryChapter>)
    suspend fun deleteEntryDownloads(entry: Entry)

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

interface EntryCapabilityProcessor {
    val type: EntryType

    fun supportsMigration(entry: Entry): Boolean = false

    fun supportsMerge(entry: Entry): Boolean = false
}

interface EntryConsumptionProcessor {
    val type: EntryType
    val supportsBookmark: Boolean

    fun canSetConsumed(status: EntryConsumptionStatus, consumed: Boolean): Boolean {
        return when (consumed) {
            true -> !status.consumed
            false -> status.consumed
        }
    }

    suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean)

    fun canSetBookmarked(status: EntryConsumptionStatus, bookmarked: Boolean): Boolean {
        return supportsBookmark && status.bookmarked != bookmarked
    }

    suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean)
}

interface EntryUpdateEligibilityProcessor {
    val type: EntryType
    fun evaluate(request: EntryUpdateEligibilityRequest): EntryUpdateEligibility
}

interface EntryProgressProcessor {
    val type: EntryType
    suspend fun snapshot(entry: Entry): EntryProgressSnapshot
    suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot)
    suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    )
}

interface EntryPlaybackPreferencesProcessor {
    val type: EntryType
    suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshot?
    suspend fun restore(entry: Entry, snapshot: EntryPlaybackPreferencesSnapshot)
    suspend fun copy(sourceEntry: Entry, targetEntry: Entry)
}

interface EntryImmersiveProcessor : EntryImmersiveInteraction {
    val type: EntryType
    override fun preloadRadius(entryType: EntryType): Int
}

interface EntryChildListProcessor {
    val type: EntryType
    fun sortedForReading(entry: Entry, chapters: List<EntryChapter>, memberIds: List<Long>): List<EntryChapter>
    fun sortedForDisplay(entry: Entry, chapters: List<EntryChapter>, memberIds: List<Long>): List<EntryChapter>
    fun buildDisplayList(request: EntryChildListRequest): List<EntryChildListRow>
    fun progressLabels(
        request: EntryChildProgressRequest,
    ): Flow<Map<Long, EntryChildProgressLabel>> = flowOf(emptyMap())
}

interface EntryChildGroupFilterProcessor {
    val type: EntryType

    fun supports(entry: Entry): Boolean
    fun shouldApplyFilter(entry: Entry): Boolean
    fun availableGroupsChanged(entryId: Long): Flow<Unit>
    suspend fun availableGroups(entry: Entry, memberIds: Collection<Long>): Set<String>
    fun excludedGroupsChanged(entryId: Long): Flow<Unit>
    suspend fun excludedGroups(entry: Entry, memberIds: Collection<Long>): Set<String>
    suspend fun setExcludedGroups(entry: Entry, memberIds: Collection<Long>, excluded: Set<String>)
}

interface EntryLibraryFilterProcessor {
    val type: EntryType
    fun supportsOutsideReleasePeriodFilter(entry: Entry): Boolean
}

interface EntryPreviewProcessor : EntryPreviewInteraction {
    val type: EntryType
}

fun interface EntryInteractionPlugin {
    fun register(registry: EntryInteractionRegistry)
}

interface EntryInteractionRegistry {
    fun registerOpenProcessor(processor: EntryOpenProcessor)
    fun registerContinueProcessor(processor: EntryContinueProcessor)
    fun registerDownloadProcessor(processor: EntryDownloadProcessor)
    fun registerCapabilityProcessor(processor: EntryCapabilityProcessor)
    fun registerConsumptionProcessor(processor: EntryConsumptionProcessor)
    fun registerUpdateEligibilityProcessor(processor: EntryUpdateEligibilityProcessor)
    fun registerProgressProcessor(processor: EntryProgressProcessor)
    fun registerPlaybackPreferencesProcessor(processor: EntryPlaybackPreferencesProcessor)
    fun registerChildListProcessor(processor: EntryChildListProcessor)
    fun registerChildGroupFilterProcessor(processor: EntryChildGroupFilterProcessor)
    fun registerLibraryFilterProcessor(processor: EntryLibraryFilterProcessor)
    fun registerPreviewProcessor(processor: EntryPreviewProcessor)
    fun registerImmersiveProcessor(processor: EntryImmersiveProcessor)
}
