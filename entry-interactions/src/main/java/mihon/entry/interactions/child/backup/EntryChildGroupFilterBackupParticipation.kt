package mihon.entry.interactions.child.backup

import mihon.entry.interactions.child.ENTRY_CHILD_GROUP_FILTER_OWNER
import mihon.entry.interactions.child.EntryChildGroupFilterBehaviorContract
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_RESTORE_EXECUTION_POINT
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT
import mihon.entry.interactions.runtime.EntryChildGroupFilterCapability
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

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
