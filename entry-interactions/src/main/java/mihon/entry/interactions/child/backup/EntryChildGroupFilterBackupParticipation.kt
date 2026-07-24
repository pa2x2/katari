package mihon.entry.interactions

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal val ENTRY_CHILD_GROUP_FILTER_BACKUP_SNAPSHOT_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.child-group-filter.backup-snapshot"),
    owner = ENTRY_CHILD_GROUP_FILTER_OWNER,
    point = ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryChildGroupFilterCapability.definition),
    behavioralContracts = listOf(EntryChildGroupFilterBehaviorContract),
)

internal val ENTRY_CHILD_GROUP_FILTER_BACKUP_RESTORE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.child-group-filter.backup-restore"),
    owner = ENTRY_CHILD_GROUP_FILTER_OWNER,
    point = ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryChildGroupFilterCapability.definition),
    behavioralContracts = listOf(EntryChildGroupFilterBehaviorContract),
)

internal object EntryChildGroupFilterBackupContributor : FeatureGraphContributor {
    override val owner = ENTRY_CHILD_GROUP_FILTER_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_CHILD_GROUP_FILTER_BACKUP_SNAPSHOT_PARTICIPANT)
        sink.add(ENTRY_CHILD_GROUP_FILTER_BACKUP_RESTORE_PARTICIPANT)
    }
}
