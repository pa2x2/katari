package mihon.entry.interactions

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryInteractions {
    val open: EntryOpenInteraction
    val continueEntry: EntryContinueInteraction
    val download: EntryDownloadInteraction
    val capability: EntryCapabilityInteraction
    val consumption: EntryConsumptionInteraction
    val updateEligibility: EntryUpdateEligibilityInteraction
    val preview: EntryPreviewInteraction
    val immersive: EntryImmersiveInteraction
    val progress: EntryProgressInteraction
    val playbackPreferences: EntryPlaybackPreferencesInteraction
    val childList: EntryChildListInteraction
    val childGroupFilter: EntryChildGroupFilterInteraction
    val libraryFilter: EntryLibraryFilterInteraction
}

data class EntryOpenOptions(
    val ownerEntryId: Long? = null,
    val bypassMerge: Boolean = false,
    val pageIndex: Int? = null,
    val newTask: Boolean = false,
    val clearTop: Boolean = false,
)

interface EntryOpenInteraction {
    fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions = EntryOpenOptions())
    fun pendingIntent(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions = EntryOpenOptions(),
    ): PendingIntent
}

interface EntryContinueInteraction {
    suspend fun continueEntry(context: Context, entry: Entry): EntryChapter?
    suspend fun findNext(entry: Entry): EntryChapter?
}

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

    fun startDownloads()
    fun pauseDownloads()
    fun clearQueue()
    fun invalidateCaches()
    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource)
    suspend fun renameEntry(entry: Entry, newTitle: String)
    fun reorderQueue(items: List<EntryDownloadQueueItem>)
    fun reorderSeries(entryType: EntryType, entryId: Long, moveToTop: Boolean)
    fun cancelQueuedDownloads(items: List<EntryDownloadQueueItem>)

    fun supportsDownloads(entryType: EntryType): Boolean

    suspend fun queue(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean = true)
    suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean = false)
    suspend fun downloadWithOptions(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean = false,
    )
    fun supportsDownloadOptions(entry: Entry): Boolean
    suspend fun resolveDownloadOptions(context: Context, entry: Entry, chapter: EntryChapter): EntryDownloadOptions?
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

data class EntryDownloadOption(
    val key: String,
    val label: String,
)

data class EntryDownloadOptionGroup(
    val key: String,
    val label: String,
    val options: List<EntryDownloadOption>,
    val selectedKey: String? = null,
    val defaultLabel: String? = null,
    val required: Boolean = false,
)

data class EntryDownloadOptions(
    val groups: List<EntryDownloadOptionGroup>,
)

data class EntryDownloadOptionSelection(
    val values: Map<String, String?>,
)

data class EntryBulkDownloadAction(
    val type: EntryBulkDownloadActionType,
    val limit: Int? = null,
) {
    companion object {
        fun next(limit: Int): EntryBulkDownloadAction = EntryBulkDownloadAction(EntryBulkDownloadActionType.NEXT, limit)
        val unread: EntryBulkDownloadAction = EntryBulkDownloadAction(EntryBulkDownloadActionType.UNREAD)
        val bookmarked: EntryBulkDownloadAction = EntryBulkDownloadAction(EntryBulkDownloadActionType.BOOKMARKED)
    }
}

enum class EntryBulkDownloadActionType {
    NEXT,
    UNREAD,
    BOOKMARKED,
}

sealed interface EntryBulkDownloadCandidateResult {
    data class Supported(val chapters: List<EntryChapter>) : EntryBulkDownloadCandidateResult
    data object Unsupported : EntryBulkDownloadCandidateResult
}

interface EntryCapabilityInteraction {
    fun supportsMigration(entry: Entry): Boolean
    fun canMigrate(entries: List<Entry>): Boolean
    fun migrationEntries(entries: List<Entry>): List<Entry>
    fun supportsMerge(entry: Entry): Boolean
    fun canMergeSelection(selection: List<EntryMergeCapabilityItem>): Boolean
    fun supportsBulkDownload(entry: Entry): Boolean
}

data class EntryMergeCapabilityItem(
    val entry: Entry,
    val isMerged: Boolean,
)

interface EntryConsumptionInteraction {
    fun canSetConsumed(entryType: EntryType, status: EntryConsumptionStatus, consumed: Boolean): Boolean
    suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean)
    fun supportsBookmark(entryType: EntryType): Boolean
    fun canSetBookmarked(entryType: EntryType, status: EntryConsumptionStatus, bookmarked: Boolean): Boolean
    suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean)
}

data class EntryConsumptionStatus(
    val consumed: Boolean,
    val bookmarked: Boolean,
    val hasPartialProgress: Boolean,
)

fun EntryChapter.consumptionStatus(hasPartialProgress: Boolean = false): EntryConsumptionStatus {
    return EntryConsumptionStatus(
        consumed = read,
        bookmarked = bookmark,
        hasPartialProgress = hasPartialProgress,
    )
}

interface EntryUpdateEligibilityInteraction {
    fun evaluate(request: EntryUpdateEligibilityRequest): EntryUpdateEligibility
}

data class EntryUpdateEligibilityRequest(
    val entry: Entry,
    val totalCount: Long,
    val unconsumedCount: Long,
    val hasStarted: Boolean,
    val restrictions: Set<EntryUpdateRestriction>,
    val fetchWindowUpperBound: Long? = null,
)

enum class EntryUpdateRestriction {
    NON_COMPLETED,
    HAS_UNCONSUMED,
    NON_STARTED,
    OUTSIDE_RELEASE_PERIOD,
}

sealed interface EntryUpdateEligibility {
    data object Eligible : EntryUpdateEligibility
    data class Skipped(val reason: EntryUpdateSkipReason) : EntryUpdateEligibility
}

enum class EntryUpdateSkipReason {
    COMPLETED,
    NOT_CAUGHT_UP,
    NOT_STARTED,
    OUTSIDE_RELEASE_PERIOD,
}

interface EntryChildGroupFilterInteraction {
    fun supports(entry: Entry): Boolean
    fun shouldApplyFilter(entry: Entry): Boolean
    fun availableGroupsChanged(entryId: Long): Flow<Unit>
    suspend fun availableGroups(entry: Entry, memberIds: Collection<Long>): Set<String>
    fun excludedGroupsChanged(entryId: Long): Flow<Unit>
    suspend fun excludedGroups(entry: Entry, memberIds: Collection<Long>): Set<String>
    suspend fun setExcludedGroups(entry: Entry, memberIds: Collection<Long>, excluded: Set<String>)
}

interface EntryLibraryFilterInteraction {
    fun supportsOutsideReleasePeriodFilter(entry: Entry): Boolean
}

interface EntryPreviewInteraction {
    fun isSupported(entry: Entry): Boolean
    fun requiresChapter(entry: Entry): Boolean
    fun config(entry: Entry): EntryPreviewConfig
    fun configChanges(entry: Entry): Flow<EntryPreviewConfig>
    suspend fun loadPreview(
        context: Context,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
        pageCount: Int,
    ): EntryPreviewHandle
    suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int)
    fun release(handle: EntryPreviewHandle)
}
