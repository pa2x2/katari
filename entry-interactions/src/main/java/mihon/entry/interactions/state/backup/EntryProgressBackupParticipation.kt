package mihon.entry.interactions

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal val ENTRY_PROGRESS_BACKUP_SNAPSHOT_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.progress-transfer.backup-snapshot"),
    owner = ENTRY_PROGRESS_FEATURE_OWNER,
    point = ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryProgressCapability.definition),
    behavioralContracts = listOf(EntryProgressBehaviorContract),
)

internal val ENTRY_PROGRESS_BACKUP_RESTORE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.progress-transfer.backup-restore"),
    owner = ENTRY_PROGRESS_FEATURE_OWNER,
    point = ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryProgressCapability.definition),
    behavioralContracts = listOf(EntryProgressBehaviorContract),
)

internal object EntryProgressBackupContributor : FeatureGraphContributor {
    override val owner = ENTRY_PROGRESS_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_PROGRESS_BACKUP_SNAPSHOT_PARTICIPANT)
        sink.add(ENTRY_PROGRESS_BACKUP_RESTORE_PARTICIPANT)
    }
}
