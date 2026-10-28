package eu.kanade.tachiyomi.ui.entry.library

import kotlinx.collections.immutable.ImmutableList
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry

sealed interface EntryLibraryDialog {
    data class ChangeCategory(
        val entry: Entry,
        val initialSelection: List<CheckboxState<Category>>,
    ) : EntryLibraryDialog

    data class DuplicateEntry(val entry: Entry, val duplicates: List<DuplicateEntryCandidate>) : EntryLibraryDialog

    data class Migrate(val target: Entry, val current: Entry) : EntryLibraryDialog

    data class RemoveFromLibrary(val entry: Entry) : EntryLibraryDialog

    data class RemoveMergedEntry(
        val members: ImmutableList<Entry>,
        val containsLocalEntry: Boolean,
    ) : EntryLibraryDialog

    data class SetFetchInterval(val entry: Entry) : EntryLibraryDialog
}
