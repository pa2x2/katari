package mihon.entry.interactions.merge.lifecycle

import mihon.entry.interactions.lifecycle.removal.ENTRY_DESTRUCTIVE_REMOVING_EXECUTION_POINT
import mihon.entry.interactions.merge.ENTRY_MERGE_FEATURE_OWNER
import mihon.entry.interactions.merge.EntryMergeBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

internal val ENTRY_MERGE_DESTRUCTIVE_REMOVAL_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.merge.destructive-removal"),
    owner = ENTRY_MERGE_FEATURE_OWNER,
    point = ENTRY_DESTRUCTIVE_REMOVING_EXECUTION_POINT,
    behavioralContracts = listOf(EntryMergeBehaviorContract.DESTRUCTIVE_REMOVAL_PARTICIPATION),
)

internal object EntryMergeDestructiveRemovalContributor : FeatureGraphContributor {
    override val owner = ENTRY_MERGE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_MERGE_DESTRUCTIVE_REMOVAL_PARTICIPANT)
    }
}
