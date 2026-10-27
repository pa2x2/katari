package mihon.entry.interactions.translation.migration

import kotlinx.serialization.json.Json
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.durable.EntryDurablePayload
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.translation.DefaultEntryTranslationLanguagesFeature
import mihon.entry.interactions.translation.EntryTranslationLanguagesMigrationPayload
import mihon.entry.interactions.translation.EntryTranslationLanguagesMigrationPreparation
import mihon.entry.interactions.translation.EntryTranslationLanguagesWriteResult

internal class EntryTranslationLanguagesMigrationParticipant(
    private val feature: () -> DefaultEntryTranslationLanguagesFeature,
) : EntryDurableParticipant<EntryMigrationDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = "entry.migration.translation-languages"

    override suspend fun prepare(event: EntryMigrationDurableEvent): EntryDurablePayload? {
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
