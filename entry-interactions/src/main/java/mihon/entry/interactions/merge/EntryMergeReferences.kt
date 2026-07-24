package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.host.EntryMergeHostExpectation
import mihon.entry.interactions.host.EntryMergeHostMemberKey
import mihon.entry.interactions.host.EntryMergeHostPreparation
import java.util.UUID

internal data class FeatureEntryMergeEditReference(
    val sessionId: String,
    val profileId: Long,
    val type: EntryType,
    val expectation: EntryMergeHostExpectation,
    val preparations: List<EntryMergeHostPreparation>,
    val entries: Map<EntryMergeHostMemberKey, EntryMergeEditorEntry>,
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
