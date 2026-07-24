package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal const val ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID = "entry.merge.profile-move"

internal val ENTRY_MERGE_PROFILE_MOVE_PREPARATION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("$ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID.prepare"),
    owner = ENTRY_MERGE_FEATURE_OWNER,
    point = ENTRY_PROFILE_MOVE_PREPARING_EXECUTION_POINT,
    behavioralContracts = listOf(EntryMergeBehaviorContract.PROFILE_MOVE_PARTICIPATION),
)

internal val ENTRY_MERGE_PROFILE_MOVE_DESTINATION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("$ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID.inspect-destination"),
    owner = ENTRY_MERGE_FEATURE_OWNER,
    point = ENTRY_PROFILE_MOVE_DESTINATION_INSPECTING_EXECUTION_POINT,
    behavioralContracts = listOf(EntryMergeBehaviorContract.PROFILE_MOVE_PARTICIPATION),
)

internal val ENTRY_MERGE_PROFILE_MOVING_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("$ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID.begin"),
    owner = ENTRY_MERGE_FEATURE_OWNER,
    point = ENTRY_PROFILE_MOVING_EXECUTION_POINT,
    behavioralContracts = listOf(EntryMergeBehaviorContract.PROFILE_MOVE_PARTICIPATION),
)

internal val ENTRY_MERGE_PROFILE_STATE_MOVED_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("$ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID.complete"),
    owner = ENTRY_MERGE_FEATURE_OWNER,
    point = ENTRY_PROFILE_STATE_MOVED_EXECUTION_POINT,
    behavioralContracts = listOf(EntryMergeBehaviorContract.PROFILE_MOVE_PARTICIPATION),
)

internal object EntryMergeProfileMoveContributor : FeatureGraphContributor {
    override val owner = ENTRY_MERGE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_MERGE_PROFILE_MOVE_PREPARATION_PARTICIPANT)
        sink.add(ENTRY_MERGE_PROFILE_MOVE_DESTINATION_PARTICIPANT)
        sink.add(ENTRY_MERGE_PROFILE_MOVING_PARTICIPANT)
        sink.add(ENTRY_MERGE_PROFILE_STATE_MOVED_PARTICIPANT)
    }
}

internal fun EntryProfileMoveReference.mergeReference(type: eu.kanade.tachiyomi.source.entry.EntryType) =
    participantReference(ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID, type) as? EntryMergeProfileMoveReference

internal fun EntryProfileMovePlan.toMergeIntent(
    reference: EntryMergeProfileMoveReference,
) = EntryMergeProfileMoveIntent(
    reference = reference,
    destinationProfileId = destinationProfileId,
    destinationEntryIdsBySourceEntryId = mappings.associate { it.sourceEntry.id to it.destinationEntryId },
    destinationEntryIdsToDetach = destinationEntryIdsToDetach,
)
