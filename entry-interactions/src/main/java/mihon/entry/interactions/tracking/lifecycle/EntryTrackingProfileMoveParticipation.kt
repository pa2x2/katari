package mihon.entry.interactions.tracking.lifecycle

import mihon.entry.interactions.lifecycle.profile.ENTRY_PROFILE_STATE_MOVED_EXECUTION_POINT
import mihon.entry.interactions.tracking.ENTRY_TRACKING_OWNER
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

internal object EntryTrackingProfileMoveBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.tracking.profile-move.behavior")
}

internal val ENTRY_TRACKING_PROFILE_MOVE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.tracking.profile-move"),
    owner = ENTRY_TRACKING_OWNER,
    point = ENTRY_PROFILE_STATE_MOVED_EXECUTION_POINT,
    behavioralContracts = listOf(EntryTrackingProfileMoveBehaviorContract),
)

internal object EntryTrackingProfileMoveContributor : FeatureGraphContributor {
    override val owner = ENTRY_TRACKING_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_TRACKING_PROFILE_MOVE_PARTICIPANT)
    }
}
