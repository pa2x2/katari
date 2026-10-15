package mihon.entry.interactions.media.migration

import kotlinx.serialization.json.Json
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.durable.EntryDurablePayload
import mihon.entry.interactions.media.EntryPlaybackPreferencesFeature
import mihon.entry.interactions.media.EntryPlaybackPreferencesMigrationPayload
import mihon.entry.interactions.media.EntryPlaybackPreferencesMigrationPreparation
import mihon.entry.interactions.media.EntryPlaybackPreferencesRestoreResult
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent

internal class EntryPlaybackPreferencesMigrationParticipant(
    private val feature: () -> EntryPlaybackPreferencesFeature,
) : EntryDurableParticipant<EntryMigrationDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = "entry.migration.playback-preferences"

    override suspend fun prepare(event: EntryMigrationDurableEvent): EntryDurablePayload? {
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
