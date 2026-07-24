package mihon.entry.interactions

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal val ENTRY_PLAYBACK_PREFERENCES_BACKUP_SNAPSHOT_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.playback-preferences-transfer.backup-snapshot"),
    owner = ENTRY_PLAYBACK_PREFERENCES_FEATURE_OWNER,
    point = ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryPlaybackPreferencesCapability.definition),
    behavioralContracts = listOf(EntryPlaybackPreferencesBehaviorContract),
)

internal val ENTRY_PLAYBACK_PREFERENCES_BACKUP_RESTORE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.playback-preferences-transfer.backup-restore"),
    owner = ENTRY_PLAYBACK_PREFERENCES_FEATURE_OWNER,
    point = ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryPlaybackPreferencesCapability.definition),
    behavioralContracts = listOf(EntryPlaybackPreferencesBehaviorContract),
)

internal object EntryPlaybackPreferencesBackupContributor : FeatureGraphContributor {
    override val owner = ENTRY_PLAYBACK_PREFERENCES_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_PLAYBACK_PREFERENCES_BACKUP_SNAPSHOT_PARTICIPANT)
        sink.add(ENTRY_PLAYBACK_PREFERENCES_BACKUP_RESTORE_PARTICIPANT)
    }
}
