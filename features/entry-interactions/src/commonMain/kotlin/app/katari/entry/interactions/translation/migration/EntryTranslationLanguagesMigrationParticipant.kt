package app.katari.entry.interactions.translation.migration

import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.durable.EntryDurablePayload
import app.katari.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import app.katari.entry.interactions.translation.DefaultEntryTranslationLanguagesFeature
import app.katari.entry.interactions.translation.EntryTranslationLanguagesMigrationPayload
import app.katari.entry.interactions.translation.EntryTranslationLanguagesMigrationPreparation
import app.katari.entry.interactions.translation.EntryTranslationLanguagesWriteResult
import kotlinx.serialization.json.Json

internal class EntryTranslationLanguagesMigrationParticipant(
    private val feature: () -> DefaultEntryTranslationLanguagesFeature,
) : EntryDurableParticipant<EntryMigrationDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = "entry.migration.translation-languages"

    override suspend fun prepare(event: EntryMigrationDurableEvent): EntryDurablePayload? {
        if (event.chapterStateOnly) return null
        return when (val result = feature().prepareMigration(event.source, event.target)) {
            is EntryTranslationLanguagesMigrationPreparation.Prepared -> EntryDurablePayload(
                schemaVersion = 1,
                value = json.encodeToString(EntryTranslationLanguagesMigrationPayload.serializer(), result.payload),
            )
            EntryTranslationLanguagesMigrationPreparation.NothingToCarry,
            is EntryTranslationLanguagesMigrationPreparation.Inapplicable,
            -> null
            is EntryTranslationLanguagesMigrationPreparation.TypeMismatch -> error(
                "Translation-language Migration requires matching Entry types",
            )
        }
    }

    override suspend fun deliver(payload: EntryDurablePayload) {
        require(payload.schemaVersion == 1) {
            "Unsupported translation-language Migration payload ${payload.schemaVersion}"
        }
        val decoded = json.decodeFromString(EntryTranslationLanguagesMigrationPayload.serializer(), payload.value)
        check(feature().applyMigration(decoded) is EntryTranslationLanguagesWriteResult.Applied)
    }
}
