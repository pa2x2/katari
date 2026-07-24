package mihon.entry.interactions

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionOrder
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal object EntryDownloadProfileMoveBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.maintenance.profile-move.behavior")
}

internal val ENTRY_DOWNLOAD_PROFILE_MOVE_PREPARATION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.maintenance.profile-move.prepare"),
    owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER,
    point = ENTRY_PROFILE_MOVING_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
    order = FeatureExecutionOrder(before = setOf(ENTRY_MERGE_PROFILE_MOVING_PARTICIPANT.id)),
    behavioralContracts = listOf(EntryDownloadProfileMoveBehaviorContract),
)

internal val ENTRY_DOWNLOAD_PROFILE_MOVE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.maintenance.profile-move.apply"),
    owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER,
    point = ENTRY_PROFILE_MOVED_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
    behavioralContracts = listOf(EntryDownloadProfileMoveBehaviorContract),
)

internal object EntryDownloadProfileMoveContributor : FeatureGraphContributor {
    override val owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_DOWNLOAD_PROFILE_MOVE_PREPARATION_PARTICIPANT)
        sink.add(ENTRY_DOWNLOAD_PROFILE_MOVE_PARTICIPANT)
    }
}
