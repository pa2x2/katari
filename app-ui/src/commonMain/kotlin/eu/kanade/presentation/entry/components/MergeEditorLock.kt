package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Immutable
import app.katari.entry.interactions.merge.EntryMergeEditorProjection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** What a merged entry's source supplies to the merge being edited. */
@Immutable
data class MergeEditorLock(
    val sourceName: String,
    /** Titles the source lists that another merge holds. */
    val takenEntries: ImmutableList<MergeEditorEntry>,
)

/** The lock on this editor's merge, or null when it isn't a merged entry's merge. */
fun EntryMergeEditorProjection.toMergeEditorLock(sourceName: (sourceId: Long) -> String): MergeEditorLock? {
    if (!targetLocked) return null
    val root = entries.firstOrNull { it.reference == target } ?: return null
    return MergeEditorLock(
        sourceName = sourceName(root.entry.source),
        takenEntries = takenEntries
            .map { MergeEditorEntry(id = it.id, entry = it, subtitle = sourceName(it.source)) }
            .toImmutableList(),
    )
}
