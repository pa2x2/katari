package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

/** Purpose-specific projection for Merge editors. It is not a reusable membership query. */
data class EntryMergeEditorProjection(
    val editReference: EntryMergeEditReference,
    val profileId: Long,
    val type: EntryType,
    val entries: List<EntryMergeEditorEntry>,
    val target: EntryMergeEditorEntryReference,
    val targetLocked: Boolean,
)

data class EntryMergeEditorEntry(
    val reference: EntryMergeEditorEntryReference,
    val entry: Entry,
    val origin: EntryMergeEditorEntryOrigin,
    val removable: Boolean,
    val removableFromLibrary: Boolean,
)

/**
 * Opaque identity for one row in a Merge editor.
 *
 * A row may represent an Entry that will only be materialized when the edit commits, so a database ID is not a valid
 * editor identity. Callers preserve and return these references without interpreting them.
 */
interface EntryMergeEditorEntryReference

enum class EntryMergeEditorEntryOrigin {
    SELECTED,
    EXISTING_MEMBER,
    NEW_MEMBER,
}
