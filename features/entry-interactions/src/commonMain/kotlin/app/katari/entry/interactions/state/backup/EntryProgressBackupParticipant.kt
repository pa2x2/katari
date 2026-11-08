package app.katari.entry.interactions.state.backup

import app.katari.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import app.katari.entry.interactions.persistence.backup.decodeEntryBackupState
import app.katari.entry.interactions.persistence.backup.entryBackupStateEnvelope
import app.katari.entry.interactions.state.EntryProgressFeature
import app.katari.entry.interactions.state.EntryProgressSnapshot
import app.katari.entry.interactions.state.EntryProgressSnapshotResult

internal class EntryProgressBackupParticipant(
    private val feature: () -> EntryProgressFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_PROGRESS_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        if (!event.selection.includeContentState) return
        event.entries.forEach { entry ->
            val result = feature().snapshot(entry)
            if (result is EntryProgressSnapshotResult.Available && result.snapshot.states.isNotEmpty()) {
                event.contributions.add(
                    entry,
                    entryBackupStateEnvelope(
                        stateId,
                        ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION,
                        EntryProgressSnapshot.serializer(),
                        result.snapshot,
                    ),
                )
            }
        }
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION,
            EntryProgressSnapshot.serializer(),
        ) ?: return
        feature().restore(event.entry, state)
    }
}
