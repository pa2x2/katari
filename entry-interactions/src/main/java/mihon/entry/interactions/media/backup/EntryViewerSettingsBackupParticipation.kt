package mihon.entry.interactions

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

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
