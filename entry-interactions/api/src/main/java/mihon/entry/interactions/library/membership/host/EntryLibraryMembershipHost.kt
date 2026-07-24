package mihon.entry.interactions

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry

/** Persistence boundary used by the Library Membership coordinator to own one atomic membership change. */
interface EntryLibraryMembershipHost {
    suspend fun prepareAddition(entry: Entry): EntryLibraryMembershipPreparation

    suspend fun add(
        entry: Entry,
        categoryIds: List<Long>,
        defaultChildFlags: Long,
    ): EntryLibraryMembershipCommit

    suspend fun remove(
        entries: List<Entry>,
        beforeCommit: suspend (persistedEntries: List<Entry>) -> Unit,
    ): EntryLibraryMembershipCommit
}

data class EntryLibraryMembershipPreparation(
    val categories: List<Category>,
    val defaultCategoryId: Long,
    val selectedCategoryIds: Set<Long>,
    val defaultChildFlags: Long,
)

sealed interface EntryLibraryMembershipCommit {
    data class Applied(
        val entries: List<Entry>,
    ) : EntryLibraryMembershipCommit

    data object NoChange : EntryLibraryMembershipCommit

    data object Conflict : EntryLibraryMembershipCommit
}

fun interface EntryLibraryCustomCoverHost {
    suspend fun cleanupAfterLibraryRemoval(entry: Entry)
}
