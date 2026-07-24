package mihon.entry.interactions.host

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

/** One feature-owned database transition. It is deliberately not a general-purpose membership mutation API. */
sealed interface EntryMergeHostTransition {
    val operationId: String
    val profileId: Long

    data class CommitEditor(
        override val operationId: String,
        override val profileId: Long,
        val expected: EntryMergeHostExpectation,
        val preparations: List<EntryMergeHostPreparation>,
        val target: EntryMergeHostMemberKey,
        val orderedEntries: List<EntryMergeHostMemberKey>,
        val removedEntries: Set<EntryMergeHostMemberKey>,
        val libraryRemovalEntries: Set<EntryMergeHostMemberKey>,
        val consequenceRequests: List<EntryMergeConsequenceRequest>,
    ) : EntryMergeHostTransition

    data class ChangeExistingGroup(
        override val operationId: String,
        override val profileId: Long,
        val expected: EntryMergeMembershipSnapshot,
        val replacementTargetEntryId: Long?,
        val replacementOrderedEntryIds: List<Long>,
        val visibleEntryId: Long?,
        val libraryRemovalEntryIds: Set<Long>,
        val consequenceRequests: List<EntryMergeConsequenceRequest>,
    ) : EntryMergeHostTransition

    data class ReplaceForMigration(
        override val operationId: String,
        override val profileId: Long,
        val expectedGroups: List<EntryMergeMembershipSnapshot>,
        val currentEntryId: Long,
        val replacementEntryId: Long,
        val replacementGroups: List<EntryMergeMembershipSnapshot>,
    ) : EntryMergeHostTransition

    data class RestoreBackupGroup(
        override val operationId: String,
        override val profileId: Long,
        val expectedGroups: List<EntryMergeMembershipSnapshot>,
        val targetEntryId: Long,
        val orderedEntryIds: List<Long>,
    ) : EntryMergeHostTransition
}

data class EntryMergeHostExpectation(
    val type: EntryType,
    val entries: List<EntryMergeHostExpectedEntry>,
)

data class EntryMergeHostExpectedEntry(
    val key: EntryMergeHostMemberKey,
    val entry: Entry,
    val persistedEntryId: Long?,
    val membership: EntryMergeMembershipSnapshot?,
)

@JvmInline
value class EntryMergeHostMemberKey(val value: String) {
    init {
        require(value.isNotBlank()) { "Merge host member key cannot be blank" }
    }
}

data class EntryMergeHostPreparation(
    val key: EntryMergeHostMemberKey,
    val entry: Entry,
    val categoryIds: List<Long>,
)

data class EntryMergeConsequenceRequest(
    val memberKey: EntryMergeHostMemberKey?,
    val entryId: Long?,
    val participantId: String,
    val schemaVersion: Int,
    val payload: String = "",
) {
    init {
        require((memberKey == null) != (entryId == null)) {
            "A Merge consequence must identify either an editor member or a persisted Entry"
        }
        require(participantId.isNotBlank()) { "Merge consequence participant ID cannot be blank" }
        require(schemaVersion > 0) { "Merge consequence schema version must be positive" }
    }
}

sealed interface EntryMergeHostTransitionResult {
    data class Applied(
        val visibleEntryId: Long?,
    ) : EntryMergeHostTransitionResult

    data object Conflict : EntryMergeHostTransitionResult

    data class OperationalFailure(
        val retryable: Boolean,
    ) : EntryMergeHostTransitionResult
}
