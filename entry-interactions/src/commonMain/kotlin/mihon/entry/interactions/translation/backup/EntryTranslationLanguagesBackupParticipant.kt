package mihon.entry.interactions.translation.backup

import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.translation.DefaultEntryTranslationLanguagesFeature
import mihon.entry.interactions.translation.EntryTranslationLanguagesSnapshot
import mihon.entry.interactions.translation.EntryTranslationLanguagesSnapshotResult

internal const val ENTRY_TRANSLATION_LANGUAGES_BACKUP_STATE_ID = "entry.translation-languages.backup"
internal const val ENTRY_TRANSLATION_LANGUAGES_BACKUP_SCHEMA_VERSION = 1

internal class EntryTranslationLanguagesBackupParticipant(
    private val feature: () -> DefaultEntryTranslationLanguagesFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_TRANSLATION_LANGUAGES_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        val results = feature().snapshot(event.entries)
        event.entries.forEach { entry ->
            val result = results[entry.id] as? EntryTranslationLanguagesSnapshotResult.Captured ?: return@forEach
            event.contributions.add(
                entry,
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_TRANSLATION_LANGUAGES_BACKUP_SCHEMA_VERSION,
                    EntryTranslationLanguagesSnapshot.serializer(),
                    result.snapshot,
                ),
            )
        }
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val snapshot = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_TRANSLATION_LANGUAGES_BACKUP_SCHEMA_VERSION,
            EntryTranslationLanguagesSnapshot.serializer(),
        ) ?: return
        feature().restore(event.entry, snapshot)
    }
}
