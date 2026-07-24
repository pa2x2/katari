package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

data class EntryProfileMovePreparingEvent(
    val request: EntryProfileMoveRequest,
    val selectedEntries: List<Entry>,
    val contributions: EntryProfileMovePreparationSink,
)

data class EntryProfileMoveDestinationInspectingEvent(
    val type: EntryType,
    val request: EntryProfileMoveRequest,
    val conflicts: List<EntryProfileMoveConflict>,
    val contributions: EntryProfileMovePreparationSink,
)

interface EntryProfileMovePreparationSink {
    fun addAtomicUnit(entries: List<Entry>)

    fun setParticipantReference(
        participantId: String,
        type: EntryType,
        reference: EntryProfileMoveParticipantReference,
    )

    fun participantReference(
        participantId: String,
        type: EntryType,
    ): EntryProfileMoveParticipantReference?

    fun markDestinationAffected(entryIds: Set<Long>)
}

data class EntryProfileMoveMapping(
    val sourceEntry: Entry,
    val destinationEntryId: Long,
)

data class EntryProfileMovePlan(
    val sourceProfileId: Long,
    val destinationProfileId: Long,
    val destinationCategoryId: Long?,
    val mappings: List<EntryProfileMoveMapping>,
    val movedEntries: List<Entry>,
    val removedEntries: List<Entry>,
    val destinationEntryIdsToDetach: Set<Long>,
)

data class EntryProfileMovingEvent(
    val type: EntryType,
    val plan: EntryProfileMovePlan,
    val reference: EntryProfileMoveReference,
    val outcomes: EntryProfileMoveOutcomeSink,
)

data class EntryProfileStateMovedEvent(
    val type: EntryType,
    val plan: EntryProfileMovePlan,
    val reference: EntryProfileMoveReference,
)

data class EntryProfileMovedEvent(
    val type: EntryType,
    val plan: EntryProfileMovePlan,
    val outcomes: EntryProfileMoveOutcomes,
)

interface EntryProfileMoveOutcomeSink {
    fun addDownloadPlan(plan: EntryDownloadRemovalPlan)
}

data class EntryProfileMoveOutcomes(
    val downloadPlans: List<EntryDownloadRemovalPlan>,
)

fun EntryProfileMovePlan.stateRequest() = EntryProfileMoveStateRequest(
    sourceProfileId = sourceProfileId,
    destinationProfileId = destinationProfileId,
    entryIds = movedEntries.map(Entry::id),
)
