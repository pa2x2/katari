package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

/** Explicit profile-scoped identity used when no complete [Entry] is available to the caller. */
data class EntryMergeSubject(
    val profileId: Long,
    val entryId: Long,
)

/**
 * Requests an editor projection for the caller's current selection.
 *
 * Existing membership is intentionally absent: the Merge feature derives it from the selected Entries. Preparations
 * describe only caller-known facts for selected Entries that may need to be persisted or added to the Library.
 */
data class EntryMergePrepareIntent(
    val selectedEntries: List<Entry>,
    val preparations: List<EntryMergeMemberPreparationIntent> = emptyList(),
)

data class EntryMergeMemberPreparationIntent(
    val entry: Entry,
    val categoryIds: List<Long>,
)

/**
 * User choices made against a Merge-feature-issued editor projection.
 *
 * The ordered and removed references are editable form values, not a list of downstream integrations for the caller
 * to run.
 */
data class EntryMergeCommitIntent(
    val editReference: EntryMergeEditReference,
    val target: EntryMergeEditorEntryReference,
    val orderedEntries: List<EntryMergeEditorEntryReference>,
    val removedEntries: Set<EntryMergeEditorEntryReference> = emptySet(),
    val libraryRemovalEntries: Set<EntryMergeEditorEntryReference> = emptySet(),
) : EntryMergeWorkflowIntent

/** Dissolves the authoritative group containing [subject] without reconstructing it in the caller. */
data class EntryMergeDissolveIntent(
    val subject: EntryMergeSubject,
) : EntryMergeWorkflowIntent

/**
 * Applies user-requested removal consequences to Entries in the authoritative group containing [subject].
 *
 * The booleans represent choices presented to the user by the removal dialog. The Merge feature decides the resulting
 * membership transition and derives any applicable feature consequences; callers do not execute a consequence
 * checklist.
 */
data class EntryMergeRemoveEntriesIntent(
    val subject: EntryMergeSubject,
    val entryIds: Set<Long>,
    val removeFromLibrary: Boolean,
    val removeDownloads: Boolean,
) : EntryMergeWorkflowIntent

sealed interface EntryMergeWorkflowIntent

/**
 * Opaque proof of the authoritative state used to construct an editor projection.
 *
 * The Merge feature accepts only references it issued. Caller implementations are rejected rather than interpreted as
 * authority.
 */
interface EntryMergeEditReference
