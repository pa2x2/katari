package mihon.entry.interactions

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry

/** The single application-facing boundary for adding Entries to or removing Entries from the Library. */
interface EntryLibraryMembershipFeature {
    suspend fun add(request: EntryLibraryAddRequest): EntryLibraryAddResult

    suspend fun remove(entries: List<Entry>): EntryLibraryRemovalResult
}

data class EntryLibraryAddRequest(
    val entry: Entry,
    val duplicatePolicy: EntryLibraryDuplicatePolicy = EntryLibraryDuplicatePolicy.CHECK,
    val categorySelection: EntryLibraryCategorySelection = EntryLibraryCategorySelection.ResolveDefault,
)

enum class EntryLibraryDuplicatePolicy {
    CHECK,
    ALLOW,
}

sealed interface EntryLibraryCategorySelection {
    data object ResolveDefault : EntryLibraryCategorySelection

    data class Selected(
        val categoryIds: List<Long>,
    ) : EntryLibraryCategorySelection
}

sealed interface EntryLibraryAddResult {
    data class Added(
        val entry: Entry,
        val consequenceFailures: List<EntryLibraryConsequenceFailure>,
    ) : EntryLibraryAddResult

    data class AlreadyInLibrary(
        val entry: Entry,
    ) : EntryLibraryAddResult

    data class DuplicateCandidates(
        val entry: Entry,
        val candidates: List<DuplicateEntryCandidate>,
    ) : EntryLibraryAddResult

    data class CategorySelectionRequired(
        val entry: Entry,
        val categories: List<Category>,
        val selectedCategoryIds: Set<Long>,
    ) : EntryLibraryAddResult

    data class Failed(
        val entry: Entry,
        val cause: Throwable,
    ) : EntryLibraryAddResult
}

sealed interface EntryLibraryRemovalResult {
    data object NoChange : EntryLibraryRemovalResult

    data class Removed(
        val entries: List<Entry>,
        val downloadDecisionEntries: List<Entry>,
        val consequenceFailures: List<EntryLibraryConsequenceFailure>,
    ) : EntryLibraryRemovalResult

    data class Failed(
        val entries: List<Entry>,
        val cause: Throwable,
    ) : EntryLibraryRemovalResult
}

data class EntryLibraryConsequenceFailure(
    val participantId: String,
    val ownerId: String,
    val cause: Throwable,
)
