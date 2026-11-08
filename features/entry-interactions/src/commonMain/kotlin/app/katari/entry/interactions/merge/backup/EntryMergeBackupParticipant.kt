package app.katari.entry.interactions.merge.backup

import app.katari.entry.interactions.merge.ENTRY_MERGE_BACKUP_SCHEMA_VERSION
import app.katari.entry.interactions.merge.ENTRY_MERGE_BACKUP_STATE_ID
import app.katari.entry.interactions.merge.EntryMergeBackupFeature
import app.katari.entry.interactions.merge.EntryMergeBackupMember
import app.katari.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupRestoreFinalizingEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import app.katari.entry.interactions.persistence.backup.decodeEntryBackupState
import app.katari.entry.interactions.persistence.backup.entryBackupStateEnvelope
import tachiyomi.domain.entry.model.Entry

/** Merge groups restore once every member exists, so members queue during restore and groups form when it ends. */
internal class EntryMergeBackupParticipant(
    private val backupFeature: () -> EntryMergeBackupFeature,
    private val restoreParticipation: () -> EntryMergeBackupRestoreParticipation,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_MERGE_BACKUP_STATE_ID

    override suspend fun companionEntries(profileId: Long, entries: List<Entry>): List<Entry> =
        backupFeature().companionsForBackup(profileId, entries)

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        val states = backupFeature().snapshotForBackup(event.profileId, event.entries.map(Entry::id))
        event.entries.forEach { entry ->
            val state = states[entry.id] ?: return@forEach
            event.contributions.add(
                entry,
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_MERGE_BACKUP_SCHEMA_VERSION,
                    EntryMergeBackupMember.serializer(),
                    state,
                ),
            )
        }
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_MERGE_BACKUP_SCHEMA_VERSION,
            EntryMergeBackupMember.serializer(),
        ) ?: return
        restoreParticipation().enqueue(event, state)
    }

    override suspend fun finalizeRestore(event: EntryBackupRestoreFinalizingEvent) {
        restoreParticipation().finalize(event)
    }
}
