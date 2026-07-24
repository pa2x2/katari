package mihon.entry.interactions

import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal object EntryCoverHashProfileMoveBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.cover-network.profile-move.behavior")
}

internal val ENTRY_COVER_HASH_PROFILE_MOVE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.cover-network.profile-move"),
    owner = ENTRY_COVER_NETWORK_OWNER,
    point = ENTRY_PROFILE_STATE_MOVED_EXECUTION_POINT,
    behavioralContracts = listOf(EntryCoverHashProfileMoveBehaviorContract),
)

internal object EntryCoverHashProfileMoveContributor : FeatureGraphContributor {
    override val owner = ENTRY_COVER_NETWORK_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_COVER_HASH_PROFILE_MOVE_PARTICIPANT)
    }
}
