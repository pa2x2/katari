package mihon.entry.interactions.media.backup

import mihon.entry.interactions.media.ENTRY_VIEWER_SETTINGS_FEATURE_OWNER
import mihon.entry.interactions.media.EntryViewerSettingsBehaviorContract
import mihon.entry.interactions.media.EntryViewerSettingsCapability
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_RESTORE_EXECUTION_POINT
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

internal val ENTRY_VIEWER_SETTINGS_BACKUP_SNAPSHOT_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.viewer-settings.backup-snapshot"),
    owner = ENTRY_VIEWER_SETTINGS_FEATURE_OWNER,
    point = ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryViewerSettingsCapability.definition),
    behavioralContracts = listOf(EntryViewerSettingsBehaviorContract),
)

internal val ENTRY_VIEWER_SETTINGS_BACKUP_RESTORE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.viewer-settings.backup-restore"),
    owner = ENTRY_VIEWER_SETTINGS_FEATURE_OWNER,
    point = ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryViewerSettingsCapability.definition),
    behavioralContracts = listOf(EntryViewerSettingsBehaviorContract),
)

internal object EntryViewerSettingsBackupContributor : FeatureGraphContributor {
    override val owner = ENTRY_VIEWER_SETTINGS_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_VIEWER_SETTINGS_BACKUP_SNAPSHOT_PARTICIPANT)
        sink.add(ENTRY_VIEWER_SETTINGS_BACKUP_RESTORE_PARTICIPANT)
    }
}
