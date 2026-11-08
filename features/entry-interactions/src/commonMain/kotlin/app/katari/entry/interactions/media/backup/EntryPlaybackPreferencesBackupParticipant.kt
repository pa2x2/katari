package app.katari.entry.interactions.media.backup

import app.katari.entry.interactions.media.EntryPlaybackPreferencesFeature
import app.katari.entry.interactions.media.EntryPlaybackPreferencesSnapshot
import app.katari.entry.interactions.media.EntryPlaybackPreferencesSnapshotResult
import app.katari.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import app.katari.entry.interactions.persistence.backup.decodeEntryBackupState
import app.katari.entry.interactions.persistence.backup.entryBackupStateEnvelope

internal class EntryPlaybackPreferencesBackupParticipant(
    private val feature: () -> EntryPlaybackPreferencesFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_PLAYBACK_PREFERENCES_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        event.entries.forEach { entry ->
            val result = feature().snapshot(entry) as? EntryPlaybackPreferencesSnapshotResult.Captured ?: return@forEach
            event.contributions.add(
                entry,
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_PLAYBACK_PREFERENCES_BACKUP_SCHEMA_VERSION,
                    EntryPlaybackPreferencesSnapshot.serializer(),
                    result.snapshot,
                ),
            )
        }
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
