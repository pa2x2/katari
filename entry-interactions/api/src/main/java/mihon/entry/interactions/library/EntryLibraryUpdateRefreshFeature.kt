package mihon.entry.interactions.library

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned source-refresh boundary for one eligible Library Update entry. */
interface EntryLibraryUpdateRefreshFeature {
    fun newSession(): EntryLibraryUpdateRefreshSession
}

/** One Library update run. Consequence participants may batch work until [complete]. */
interface EntryLibraryUpdateRefreshSession {
    suspend fun refresh(request: EntryLibraryUpdateRefreshRequest): EntryLibraryUpdateRefreshResult

    fun complete()
}

data class EntryLibraryUpdateRefreshRequest(
    val entry: Entry,
    val fetchMetadata: Boolean,
    val fetchWindowLowerBound: Long,
    val fetchWindowUpperBound: Long,
    val firstFetchIsBackCatalogue: Boolean,
)

sealed interface EntryLibraryUpdateRefreshResult {
    /** [joinedEntryIds] are members that joined the entry's merge with this refresh and still need their own. */
    data class Updated(
        val newChildren: List<EntryChapter>,
        val joinedEntryIds: Set<Long>,
    ) : EntryLibraryUpdateRefreshResult

    data object SourceUnavailable : EntryLibraryUpdateRefreshResult

    data object NoChildren : EntryLibraryUpdateRefreshResult

    data class OperationalFailure(val error: Throwable) : EntryLibraryUpdateRefreshResult
}
