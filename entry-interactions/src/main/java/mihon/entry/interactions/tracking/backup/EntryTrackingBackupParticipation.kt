package mihon.entry.interactions.tracking.backup

import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_RESTORE_EXECUTION_POINT
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT
import mihon.entry.interactions.tracking.ENTRY_TRACKING_OWNER
import mihon.entry.interactions.tracking.EntryTrackingBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

internal val ENTRY_TRACKING_BACKUP_SNAPSHOT_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.tracking.backup-snapshot"),
    owner = ENTRY_TRACKING_OWNER,
    point = ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
    behavioralContracts = listOf(EntryTrackingBehaviorContract.REGISTRY),
)

internal val ENTRY_TRACKING_BACKUP_RESTORE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.tracking.backup-restore"),
    owner = ENTRY_TRACKING_OWNER,
    point = ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
    behavioralContracts = listOf(EntryTrackingBehaviorContract.REGISTRY),
)

internal object EntryTrackingBackupContributor : FeatureGraphContributor {
    override val owner = ENTRY_TRACKING_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_TRACKING_BACKUP_SNAPSHOT_PARTICIPANT)
        sink.add(ENTRY_TRACKING_BACKUP_RESTORE_PARTICIPANT)
    }
}
