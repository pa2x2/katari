package eu.kanade.tachiyomi.ui.entry.merge

import androidx.compose.runtime.Immutable
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.components.MergeEditorLock
import eu.kanade.presentation.entry.components.MergeTarget
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mihon.entry.interactions.merge.EntryMergeEditReference
import mihon.entry.interactions.merge.EntryMergeEditorEntryReference
import tachiyomi.domain.entry.model.Entry

/**
 * The merge sheet an entry screen shows: picking the library entry to merge the entry into and then editing that merge,
 * or managing the merge the entry is in.
 */
sealed interface EntryMergeSheet {
    data class SelectTarget(
        val entry: Entry,
        val query: String = "",
        val targets: ImmutableList<MergeTarget>,
        val visibleTargets: ImmutableList<MergeTarget>,
    ) : EntryMergeSheet

    data class Edit(
        val entry: Entry,
        val editReference: EntryMergeEditReference,
        val targetId: Long,
        val targetReference: EntryMergeEditorEntryReference,
        val targetLocked: Boolean,
        val lock: MergeEditorLock?,
        val entries: ImmutableList<MergeEditorEntry>,
        val referencesById: Map<Long, EntryMergeEditorEntryReference>,
        val removedIds: Set<Long>,
        val libraryRemovalIds: Set<Long>,
    ) : EntryMergeSheet {
        val enabled: Boolean
            get() = entries.count { it.id !in (removedIds + libraryRemovalIds) } > 1
    }

    data class Manage(
        val editReference: EntryMergeEditReference,
        val targetId: Long,
        val savedTargetId: Long,
        val targetLocked: Boolean,
        val lock: MergeEditorLock?,
        val members: ImmutableList<MergeMember>,
        val removableIds: ImmutableList<Long> = persistentListOf(),
        val libraryRemovalIds: ImmutableList<Long> = persistentListOf(),
    ) : EntryMergeSheet
}

@Immutable
data class MergeMember(
    val id: Long,
    val reference: EntryMergeEditorEntryReference,
    val entry: Entry,
    val subtitle: String,
    val removable: Boolean,
    val locked: Boolean,
)
