package mihon.entry.interactions.media.backup

import mihon.entry.interactions.media.ENTRY_PLAYBACK_PREFERENCES_FEATURE_OWNER
import mihon.entry.interactions.media.EntryPlaybackPreferencesBehaviorContract
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_RESTORE_EXECUTION_POINT
import mihon.entry.interactions.persistence.backup.ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT
import mihon.entry.interactions.state.EntryPlaybackPreferencesCapability
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

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
