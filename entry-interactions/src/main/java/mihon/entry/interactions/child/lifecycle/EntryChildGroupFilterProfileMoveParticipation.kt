package mihon.entry.interactions.child.lifecycle

import mihon.entry.interactions.child.ENTRY_CHILD_GROUP_FILTER_OWNER
import mihon.entry.interactions.lifecycle.profile.ENTRY_PROFILE_STATE_MOVED_EXECUTION_POINT
import mihon.entry.interactions.runtime.EntryChildGroupFilterCapability
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

internal object EntryChildGroupFilterProfileMoveBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.child-group-filter.profile-move.behavior")
}

internal val ENTRY_CHILD_GROUP_FILTER_PROFILE_MOVE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.child-group-filter.profile-move"),
    owner = ENTRY_CHILD_GROUP_FILTER_OWNER,
    point = ENTRY_PROFILE_STATE_MOVED_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryChildGroupFilterCapability.definition),
    behavioralContracts = listOf(EntryChildGroupFilterProfileMoveBehaviorContract),
)

internal object EntryChildGroupFilterProfileMoveContributor : FeatureGraphContributor {
    override val owner = ENTRY_CHILD_GROUP_FILTER_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_CHILD_GROUP_FILTER_PROFILE_MOVE_PARTICIPANT)
    }
}
