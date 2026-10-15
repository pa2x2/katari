package mihon.entry.interactions.tracking.backup

import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope

internal class EntryTrackingBackupParticipant(
    private val backupFeature: () -> EntryTrackingBackupFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_TRACKING_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        if (!event.selection.includeTrackingState) return
        val state = backupFeature().snapshot(event.profileId, event.entry) ?: return
        event.contributions.add(
            entryBackupStateEnvelope(
                stateId,
                ENTRY_TRACKING_BACKUP_SCHEMA_VERSION,
                EntryTrackingBackupState.serializer(),
                state,
            ),
        )
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_TRACKING_BACKUP_SCHEMA_VERSION,
            EntryTrackingBackupState.serializer(),
        ) ?: return
        backupFeature().restore(event.profileId, event.entry, state)
    }
}
