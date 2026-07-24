package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

/** Internal Merge projection consumed only by Download feature coordinators. */
internal interface EntryMergeDownloadOwnershipProjection {
    suspend fun resolveDownloadOwners(subject: EntryMergeSubject): EntryMergeDownloadOwners
}

internal data class EntryMergeDownloadOwners(
    val profileId: Long,
    val visibleEntryId: Long,
    val orderedOwners: List<Entry>,
)
