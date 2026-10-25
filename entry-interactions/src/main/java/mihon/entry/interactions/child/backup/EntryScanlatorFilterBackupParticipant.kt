package mihon.entry.interactions.child.backup

import mihon.entry.interactions.child.EntryScanlatorFilterFeature
import mihon.entry.interactions.child.EntryScanlatorFilterSnapshotResult
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope

internal class EntryScanlatorFilterBackupParticipant(
    private val feature: () -> EntryScanlatorFilterFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_SCANLATOR_FILTER_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        val result = feature().snapshot(event.profileId, event.entry)
        if (result is EntryScanlatorFilterSnapshotResult.Available && result.excludedScanlators.isNotEmpty()) {
            event.contributions.add(
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_SCANLATOR_FILTER_BACKUP_SCHEMA_VERSION,
                    EntryScanlatorFilterBackupState.serializer(),
                    EntryScanlatorFilterBackupState(result.excludedScanlators),
                ),
            )
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
