package app.katari.entry.interactions.child.backup

import app.katari.entry.interactions.child.EntryScanlatorFilterFeature
import app.katari.entry.interactions.child.EntryScanlatorFilterSnapshotResult
import app.katari.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import app.katari.entry.interactions.persistence.backup.decodeEntryBackupState
import app.katari.entry.interactions.persistence.backup.entryBackupStateEnvelope

internal class EntryScanlatorFilterBackupParticipant(
    private val feature: () -> EntryScanlatorFilterFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_SCANLATOR_FILTER_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        val results = feature().snapshot(event.profileId, event.entries)
        event.entries.forEach { entry ->
            val result = results[entry.id]
            if (result is EntryScanlatorFilterSnapshotResult.Available && result.excludedScanlators.isNotEmpty()) {
                event.contributions.add(
                    entry,
                    entryBackupStateEnvelope(
                        stateId,
                        ENTRY_SCANLATOR_FILTER_BACKUP_SCHEMA_VERSION,
                        EntryScanlatorFilterBackupState.serializer(),
                        EntryScanlatorFilterBackupState(result.excludedScanlators),
                    ),
                )
            }
        }
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_SCANLATOR_FILTER_BACKUP_SCHEMA_VERSION,
            EntryScanlatorFilterBackupState.serializer(),
        ) ?: return
        feature().restore(event.profileId, event.entry, state.excludedScanlators)
    }
}
