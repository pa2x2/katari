package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryMergeProfileMoveFeature {
    suspend fun prepare(
        sourceProfileId: Long,
        selectedVisibleEntryIds: List<Long>,
    ): EntryMergeProfileMovePreparationResult

    suspend fun inspectDestination(
        reference: EntryMergeProfileMoveReference,
        destinationProfileId: Long,
        destinationEntryIds: List<Long>,
    ): EntryMergeProfileMoveDestinationResult

    suspend fun begin(intent: EntryMergeProfileMoveIntent): EntryMergeProfileMoveExecutionResult

    suspend fun complete(intent: EntryMergeProfileMoveIntent): EntryMergeProfileMoveExecutionResult
}

interface EntryMergeProfileMoveReference : EntryProfileMoveParticipantReference

data class EntryMergeProfileMoveUnit(
    val entries: List<Entry>,
)

sealed interface EntryMergeProfileMovePreparationResult {
    data class Ready(
        val reference: EntryMergeProfileMoveReference,
        val units: List<EntryMergeProfileMoveUnit>,
    ) : EntryMergeProfileMovePreparationResult

    data object Empty : EntryMergeProfileMovePreparationResult
}

sealed interface EntryMergeProfileMoveDestinationResult {
    data class Ready(
        val reference: EntryMergeProfileMoveReference,
        val mergeAffectedEntryIds: Set<Long>,
    ) : EntryMergeProfileMoveDestinationResult

    data object InvalidReference : EntryMergeProfileMoveDestinationResult
}

data class EntryMergeProfileMoveIntent(
    val reference: EntryMergeProfileMoveReference,
    val destinationProfileId: Long,
    val destinationEntryIdsBySourceEntryId: Map<Long, Long>,
    val destinationEntryIdsToDetach: Set<Long>,
)

sealed interface EntryMergeProfileMoveExecutionResult {
    data object Applied : EntryMergeProfileMoveExecutionResult
    data object Conflict : EntryMergeProfileMoveExecutionResult

    data class OperationalFailure(
        val retryable: Boolean,
    ) : EntryMergeProfileMoveExecutionResult
}
