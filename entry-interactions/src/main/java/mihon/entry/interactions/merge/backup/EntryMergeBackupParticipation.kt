package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal val ENTRY_MERGE_BACKUP_SNAPSHOT_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.merge.backup-snapshot"),
    owner = ENTRY_MERGE_FEATURE_OWNER,
    point = ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
    behavioralContracts = listOf(EntryMergeBehaviorContract.WORKFLOW),
)

internal val ENTRY_MERGE_BACKUP_RESTORE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.merge.backup-restore"),
    owner = ENTRY_MERGE_FEATURE_OWNER,
    point = ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
    behavioralContracts = listOf(EntryMergeBehaviorContract.WORKFLOW),
)

internal val ENTRY_MERGE_BACKUP_FINALIZE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.merge.backup-finalize"),
    owner = ENTRY_MERGE_FEATURE_OWNER,
    point = ENTRY_BACKUP_RESTORE_FINALIZING_EXECUTION_POINT,
    behavioralContracts = listOf(EntryMergeBehaviorContract.WORKFLOW),
)

internal object EntryMergeBackupContributor : FeatureGraphContributor {
    override val owner = ENTRY_MERGE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_MERGE_BACKUP_SNAPSHOT_PARTICIPANT)
        sink.add(ENTRY_MERGE_BACKUP_RESTORE_PARTICIPANT)
        sink.add(ENTRY_MERGE_BACKUP_FINALIZE_PARTICIPANT)
    }
}
