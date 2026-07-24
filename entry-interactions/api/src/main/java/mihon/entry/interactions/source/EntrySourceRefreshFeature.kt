package mihon.entry.interactions

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
