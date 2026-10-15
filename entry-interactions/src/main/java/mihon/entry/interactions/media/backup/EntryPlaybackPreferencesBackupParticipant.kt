package mihon.entry.interactions.media.backup

import mihon.entry.interactions.media.EntryPlaybackPreferencesFeature
import mihon.entry.interactions.media.EntryPlaybackPreferencesSnapshot
import mihon.entry.interactions.media.EntryPlaybackPreferencesSnapshotResult
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope

internal class EntryPlaybackPreferencesBackupParticipant(
    private val feature: () -> EntryPlaybackPreferencesFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_PLAYBACK_PREFERENCES_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        val result = feature().snapshot(event.entry) as? EntryPlaybackPreferencesSnapshotResult.Captured ?: return
        event.contributions.add(
            entryBackupStateEnvelope(
                stateId,
                ENTRY_PLAYBACK_PREFERENCES_BACKUP_SCHEMA_VERSION,
                EntryPlaybackPreferencesSnapshot.serializer(),
                result.snapshot,
            ),
        )
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_PLAYBACK_PREFERENCES_BACKUP_SCHEMA_VERSION,
            EntryPlaybackPreferencesSnapshot.serializer(),
        ) ?: return
        feature().restore(event.entry, state)
    }
}
