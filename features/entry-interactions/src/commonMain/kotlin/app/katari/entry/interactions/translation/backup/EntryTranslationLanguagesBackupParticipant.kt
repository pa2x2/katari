package app.katari.entry.interactions.translation.backup

import app.katari.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import app.katari.entry.interactions.persistence.backup.decodeEntryBackupState
import app.katari.entry.interactions.persistence.backup.entryBackupStateEnvelope
import app.katari.entry.interactions.translation.DefaultEntryTranslationLanguagesFeature
import app.katari.entry.interactions.translation.EntryTranslationLanguagesSnapshot
import app.katari.entry.interactions.translation.EntryTranslationLanguagesSnapshotResult

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
