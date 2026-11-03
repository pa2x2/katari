package mihon.entry.interactions.source

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned boundary for refreshing one Entry from its concrete source. */
interface EntrySourceRefreshFeature {
    suspend fun refresh(request: EntrySourceRefreshRequest): EntrySourceRefreshResult
}

data class EntrySourceRefreshRequest(
    val entry: Entry,
    val fetchDetails: Boolean = true,
    val fetchChildren: Boolean = true,
    val manual: Boolean,
    val fetchWindow: EntrySourceRefreshWindow = EntrySourceRefreshWindow(),
    /** False when the entry's first chapters are new releases, such as those of a season its source just added. */
    val firstFetchIsBackCatalogue: Boolean = true,
    /**
     * True to read every part of a chapter list the source gives in parts before returning, for work that needs every
     * chapter at once. Otherwise the older parts follow in the background.
     */
    val wholeChildList: Boolean = false,
    /**
     * False for library updates, which record their checks against their run. Any other successful chapter refresh of a
     * Library entry is recorded as a check of it, so a failure an earlier update recorded doesn't outlive the fix.
     */
    val recordsUpdateCheck: Boolean = true,
)

data class EntrySourceRefreshWindow(
    val lowerBound: Long = 0L,
    val upperBound: Long = 0L,
)

sealed interface EntrySourceRefreshResult {
    data class Refreshed(
        val insertedChildren: List<EntryChapter>,
        val insertedChildrenTotal: Int,
        val updatedChildren: Int,
        val removedChildren: Int,
        val metadataChanged: Boolean,
        /**
         * Members that joined the entry's merge because its source now lists them. They are stored and in the
         * Library with the entry, but have no chapters until they are refreshed themselves.
         */
        val joinedEntryIds: Set<Long>,
    ) : EntrySourceRefreshResult {
        val hasChanges: Boolean
            get() = insertedChildrenTotal > 0 || updatedChildren > 0 || removedChildren > 0
    }

    data class SourceUnavailable(val sourceId: Long) : EntrySourceRefreshResult

    data class Failed(val reason: EntrySourceRefreshFailure) : EntrySourceRefreshResult
}

sealed interface EntrySourceRefreshFailure {
    data object NoChildren : EntrySourceRefreshFailure

    data class Operation(val error: Throwable) : EntrySourceRefreshFailure
}
