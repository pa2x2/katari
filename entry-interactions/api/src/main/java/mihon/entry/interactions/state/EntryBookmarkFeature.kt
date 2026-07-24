package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned boundary for bookmark applicability, eligibility, and mutation. */
interface EntryBookmarkFeature {
    fun isApplicable(type: EntryType): Boolean

    fun availability(
        target: EntryBookmarkTarget,
        bookmarked: Boolean,
    ): EntryBookmarkAvailability

    fun selectionAvailability(
        targets: List<EntryBookmarkTarget>,
        bookmarked: Boolean,
    ): EntryBookmarkAvailability

    suspend fun setBookmarked(
        entry: Entry,
        chapters: List<EntryChapter>,
        bookmarked: Boolean,
    ): EntryBookmarkMutationResult
}

data class EntryBookmarkTarget(
    val type: EntryType,
    val status: EntryBookmarkStatus,
)

sealed interface EntryBookmarkAvailability {
    data object Available : EntryBookmarkAvailability
    data object NoChange : EntryBookmarkAvailability
    data class Inapplicable(val types: Set<EntryType>) : EntryBookmarkAvailability
}

sealed interface EntryBookmarkMutationResult {
    data class Applied(val changedCount: Int) : EntryBookmarkMutationResult
    data object NoChange : EntryBookmarkMutationResult
    data class Inapplicable(val type: EntryType) : EntryBookmarkMutationResult
}
