package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned boundary for user-initiated individual and bulk download actions. */
interface EntryDownloadActionFeature {
    fun individualAvailability(request: EntryDownloadActionRequest): EntryDownloadActionAvailability

    fun individualSelectionAvailability(
        requests: List<EntryDownloadActionRequest>,
    ): EntryDownloadActionAvailability

    fun bulkAvailability(
        requests: List<EntryDownloadActionRequest>,
        action: EntryBulkDownloadAction,
    ): EntryDownloadActionAvailability

    fun notificationAvailability(
        entry: Entry,
        childCount: Int,
    ): EntryDownloadActionAvailability

    suspend fun download(
        entry: Entry,
        chapters: List<EntryChapter>,
        startNow: Boolean = false,
    ): EntryDownloadActionResult

    suspend fun delete(
        entry: Entry,
        chapters: List<EntryChapter>,
    ): EntryDownloadActionResult

    fun cancel(
        request: EntryDownloadActionRequest,
        chapterId: Long,
    ): EntryDownloadCancellationResult

    /** Restarts processing after a user retries selected failed downloads. */
    fun retry(requests: List<EntryDownloadActionRequest>): EntryDownloadActionResult

    suspend fun resolveBulkDownloadCandidates(
        request: EntryBulkDownloadRequest,
    ): EntryBulkDownloadResolutionResult
}

data class EntryDownloadActionRequest(
    val type: EntryType,
    /** Every source participating in the requested action. The Feature interprets their runtime availability. */
    val sourceIds: Set<Long>,
) {
    init {
        require(sourceIds.isNotEmpty()) { "Download action request must contain at least one source" }
    }

    companion object {
        fun forEntry(entry: Entry): EntryDownloadActionRequest {
            return EntryDownloadActionRequest(entry.type, setOf(entry.source))
        }
    }
}

data class EntryBulkDownloadRequest(
    val entry: Entry,
    val action: EntryBulkDownloadAction,
    val sourceIds: Set<Long> = setOf(entry.source),
    /** The currently visible children, or `null` when the type provider must load the candidate pool. */
    val visibleCandidates: List<EntryChapter>? = null,
    val memberEntryIds: List<Long> = emptyList(),
) {
    init {
        require(sourceIds.isNotEmpty()) { "Bulk Download request must contain at least one source" }
    }
}

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

enum class EntryDownloadActionBlocker {
    EMPTY_SELECTION,
    LOCAL_OR_STUB,
    NOTIFICATION_SELECTION_TOO_LARGE,
}

sealed interface EntryDownloadActionAvailability {
    data object Available : EntryDownloadActionAvailability

    data class Inapplicable(
        val types: Set<EntryType>,
    ) : EntryDownloadActionAvailability

    data class Blocked(
        val blockers: Set<EntryDownloadActionBlocker>,
    ) : EntryDownloadActionAvailability
}

sealed interface EntryDownloadActionResult {
    data object Performed : EntryDownloadActionResult

    data class Inapplicable(
        val types: Set<EntryType>,
    ) : EntryDownloadActionResult

    data class Blocked(
        val blockers: Set<EntryDownloadActionBlocker>,
    ) : EntryDownloadActionResult
}

sealed interface EntryDownloadCancellationResult {
    data class Cancelled(
        val status: EntryDownloadStatus,
    ) : EntryDownloadCancellationResult

    data object NotQueued : EntryDownloadCancellationResult

    data class Inapplicable(
        val types: Set<EntryType>,
    ) : EntryDownloadCancellationResult

    data class Blocked(
        val blockers: Set<EntryDownloadActionBlocker>,
    ) : EntryDownloadCancellationResult
}

sealed interface EntryBulkDownloadResolutionResult {
    data class Candidates(
        val chapters: List<EntryChapter>,
    ) : EntryBulkDownloadResolutionResult

    data object NoCandidates : EntryBulkDownloadResolutionResult

    data class Inapplicable(
        val types: Set<EntryType>,
    ) : EntryBulkDownloadResolutionResult

    data class Blocked(
        val blockers: Set<EntryDownloadActionBlocker>,
    ) : EntryBulkDownloadResolutionResult
}
