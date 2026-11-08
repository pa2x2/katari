package app.katari.entry.interactions.media.migration

import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.durable.EntryDurablePayload
import app.katari.entry.interactions.media.EntryPlaybackPreferencesFeature
import app.katari.entry.interactions.media.EntryPlaybackPreferencesMigrationPayload
import app.katari.entry.interactions.media.EntryPlaybackPreferencesMigrationPreparation
import app.katari.entry.interactions.media.EntryPlaybackPreferencesRestoreResult
import app.katari.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import kotlinx.serialization.json.Json

internal class EntryPlaybackPreferencesMigrationParticipant(
    private val feature: () -> EntryPlaybackPreferencesFeature,
) : EntryDurableParticipant<EntryMigrationDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = "entry.migration.playback-preferences"

    override suspend fun prepare(event: EntryMigrationDurableEvent): EntryDurablePayload? {
        if (event.chapterStateOnly) return null
        return when (val result = feature().prepareMigration(event.source, event.target)) {
            is EntryPlaybackPreferencesMigrationPreparation.Prepared -> EntryDurablePayload(
                schemaVersion = 1,
                value = json.encodeToString(EntryPlaybackPreferencesMigrationPayload.serializer(), result.payload),
            )
            EntryPlaybackPreferencesMigrationPreparation.NoPreferences,
            is EntryPlaybackPreferencesMigrationPreparation.Inapplicable,
            -> null
            is EntryPlaybackPreferencesMigrationPreparation.TypeMismatch -> error(
                "Playback-preference Migration requires matching Entry types",
            )
        }
    }

    override suspend fun deliver(payload: EntryDurablePayload) {
        require(payload.schemaVersion == 1) {
            "Unsupported playback-preference Migration payload ${payload.schemaVersion}"
        }
        val decoded = json.decodeFromString(EntryPlaybackPreferencesMigrationPayload.serializer(), payload.value)
        check(feature().applyMigration(decoded) is EntryPlaybackPreferencesRestoreResult.Applied)
    }
}
