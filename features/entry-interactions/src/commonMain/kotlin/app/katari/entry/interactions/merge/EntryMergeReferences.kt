package app.katari.entry.interactions.merge

import app.katari.entry.interactions.merge.host.EntryMergeHostExpectation
import app.katari.entry.interactions.merge.host.EntryMergeHostMemberKey
import app.katari.entry.interactions.merge.host.EntryMergeHostPreparation
import eu.kanade.tachiyomi.source.entry.EntryType
import java.util.UUID

internal data class FeatureEntryMergeEditReference(
    val sessionId: String,
    val profileId: Long,
    val type: EntryType,
    val expectation: EntryMergeHostExpectation,
    val preparations: List<EntryMergeHostPreparation>,
    val entries: Map<EntryMergeHostMemberKey, EntryMergeEditorEntry>,
    /** The root of the merged entry this edit belongs to; it stays the root and first member. */
    val lockedTarget: EntryMergeHostMemberKey? = null,
) : EntryMergeEditReference

internal data class FeatureEntryMergeEditorEntryReference(
    val sessionId: String,
    val key: EntryMergeHostMemberKey,
) : EntryMergeEditorEntryReference

internal fun newEntryMergeSessionId(): String = UUID.randomUUID().toString()

internal fun newEntryMergeMemberKey(): EntryMergeHostMemberKey = EntryMergeHostMemberKey(UUID.randomUUID().toString())

internal fun EntryMergeEditorEntryReference.requireFeatureReference(
    edit: FeatureEntryMergeEditReference,
): FeatureEntryMergeEditorEntryReference {
    val reference = this as? FeatureEntryMergeEditorEntryReference
        ?: throw UnrecognizedEntryMergeReferenceException()
    if (reference.sessionId != edit.sessionId || reference.key !in edit.entries) {
        throw UnrecognizedEntryMergeReferenceException()
    }
    return reference
}

internal class UnrecognizedEntryMergeReferenceException : IllegalArgumentException()
