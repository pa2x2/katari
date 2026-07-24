package mihon.entry.interactions

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

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
