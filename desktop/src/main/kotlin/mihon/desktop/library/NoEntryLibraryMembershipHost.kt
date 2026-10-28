package mihon.desktop.library

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.library.membership.host.EntryLibraryMembershipCommit
import mihon.entry.interactions.library.membership.host.EntryLibraryMembershipHost
import mihon.entry.interactions.library.membership.host.EntryLibraryMembershipPreparation
import tachiyomi.domain.entry.model.Entry

/**
 * Stands in for adding entries to and removing them from the library, which desktop doesn't offer yet: its entries
 * come from a restored backup, and the library hides deleting them as it does where entries can't be deleted. Asked
 * anyway, it changes nothing and answers as for entries that changed meanwhile, so the shared membership feature
 * reports the change as failed. Android's database host replaces this once desktop hosts the merge, download and
 * custom cover consequences of a membership change.
 */
@Inject
class NoEntryLibraryMembershipHost : EntryLibraryMembershipHost {
    override suspend fun prepareAddition(entry: Entry): EntryLibraryMembershipPreparation =
        throw UnsupportedOperationException("Desktop cannot add entries to the library")

    override suspend fun add(
        entry: Entry,
        categoryIds: List<Long>,
        defaultChildFlags: Long,
    ): EntryLibraryMembershipCommit = EntryLibraryMembershipCommit.Conflict

    override suspend fun remove(
        entries: List<Entry>,
        beforeCommit: suspend (persistedEntries: List<Entry>) -> Unit,
    ): EntryLibraryMembershipCommit = EntryLibraryMembershipCommit.Conflict
}
