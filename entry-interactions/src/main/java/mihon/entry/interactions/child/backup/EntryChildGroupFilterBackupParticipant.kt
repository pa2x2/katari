package mihon.entry.interactions.child.backup

import mihon.entry.interactions.child.EntryChildGroupFilterFeature
import mihon.entry.interactions.child.EntryChildGroupFilterSnapshotResult
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope

internal class EntryChildGroupFilterBackupParticipant(
    private val feature: () -> EntryChildGroupFilterFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        val result = feature().snapshot(event.profileId, event.entry)
        if (result is EntryChildGroupFilterSnapshotResult.Available && result.excludedGroups.isNotEmpty()) {
            event.contributions.add(
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION,
                    EntryChildGroupFilterBackupState.serializer(),
                    EntryChildGroupFilterBackupState(result.excludedGroups),
                ),
            )
        }
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION,
            EntryChildGroupFilterBackupState.serializer(),
        ) ?: return
        feature().restore(event.profileId, event.entry, state.excludedGroups)
    }
}
