package mihon.entry.interactions.download.configuration.backup

import mihon.entry.interactions.download.ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_OWNER
import mihon.entry.interactions.download.EntryDownloadOptionsBehaviorContract
import mihon.entry.interactions.download.EntryDownloadOptionsCapability
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_RESTORE_EXECUTION_POINT
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

internal val ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SNAPSHOT_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.configuration.backup-snapshot"),
    owner = ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_OWNER,
    point = ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadOptionsCapability.definition),
    behavioralContracts = listOf(EntryDownloadOptionsBehaviorContract),
)

internal val ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_RESTORE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.configuration.backup-restore"),
    owner = ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_OWNER,
    point = ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadOptionsCapability.definition),
    behavioralContracts = listOf(EntryDownloadOptionsBehaviorContract),
)

internal object EntryDownloadConfigurationBackupContributor : FeatureGraphContributor {
    override val owner = ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SNAPSHOT_PARTICIPANT)
        sink.add(ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_RESTORE_PARTICIPANT)
    }
}
