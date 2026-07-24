package mihon.entry.interactions

import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal object EntryDestructiveRemovalCustomCoverBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.custom-cover.destructive-removal.behavior")
}

internal val ENTRY_CUSTOM_COVER_DESTRUCTIVE_REMOVAL_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.custom-cover.destructive-removal"),
    owner = ENTRY_LIBRARY_CUSTOM_COVER_OWNER,
    point = ENTRY_DESTRUCTIVE_REMOVED_EXECUTION_POINT,
    behavioralContracts = listOf(EntryDestructiveRemovalCustomCoverBehaviorContract),
)

internal object EntryDestructiveRemovalCustomCoverContributor : FeatureGraphContributor {
    override val owner = ENTRY_LIBRARY_CUSTOM_COVER_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_CUSTOM_COVER_DESTRUCTIVE_REMOVAL_PARTICIPANT)
    }
}
