package mihon.entry.interactions.state.backup

import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.state.EntryProgressFeature
import mihon.entry.interactions.state.EntryProgressSnapshot
import mihon.entry.interactions.state.EntryProgressSnapshotResult

internal class EntryProgressBackupParticipant(
    private val feature: () -> EntryProgressFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_PROGRESS_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        if (!event.selection.includeContentState) return
        val result = feature().snapshot(event.entry)
        if (result is EntryProgressSnapshotResult.Available && result.snapshot.states.isNotEmpty()) {
            event.contributions.add(
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION,
                    EntryProgressSnapshot.serializer(),
                    result.snapshot,
                ),
            )
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
