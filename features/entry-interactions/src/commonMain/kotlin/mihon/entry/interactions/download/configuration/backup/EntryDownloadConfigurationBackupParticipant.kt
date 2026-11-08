package mihon.entry.interactions.download.configuration.backup

import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope

internal class EntryDownloadConfigurationBackupParticipant(
    private val feature: () -> EntryDownloadConfigurationBackupFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        if (!event.selection.includeContentState) return
        val states = feature().snapshot(event.entries)
        event.entries.forEach { entry ->
            val state = states[entry.id] ?: return@forEach
            event.contributions.add(
                entry,
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SCHEMA_VERSION,
                    EntryDownloadConfigurationBackupState.serializer(),
                    state,
                ),
            )
        }
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SCHEMA_VERSION,
            EntryDownloadConfigurationBackupState.serializer(),
        ) ?: return
        feature().restore(event.entry, state)
    }
}
