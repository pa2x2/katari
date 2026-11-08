package app.katari.entry.interactions.media.migration

import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.durable.EntryDurablePayload
import app.katari.entry.interactions.media.EntryViewerSettingsFeature
import app.katari.entry.interactions.media.EntryViewerSettingsMigrationPayload
import app.katari.entry.interactions.media.EntryViewerSettingsMigrationPreparation
import app.katari.entry.interactions.media.EntryViewerSettingsRestoreResult
import app.katari.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import kotlinx.serialization.json.Json

internal class EntryViewerSettingsMigrationParticipant(
    private val feature: () -> EntryViewerSettingsFeature,
) : EntryDurableParticipant<EntryMigrationDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = "entry.migration.viewer-settings"

    override suspend fun prepare(event: EntryMigrationDurableEvent): EntryDurablePayload? {
        if (event.chapterStateOnly) return null
        return when (val result = feature().prepareMigration(event.source, event.target)) {
            is EntryViewerSettingsMigrationPreparation.Prepared -> EntryDurablePayload(
                schemaVersion = 1,
                value = json.encodeToString(EntryViewerSettingsMigrationPayload.serializer(), result.payload),
            )
            is EntryViewerSettingsMigrationPreparation.Inapplicable -> null
            is EntryViewerSettingsMigrationPreparation.TypeMismatch -> error(
                "Viewer Settings Migration requires matching Entry types",
            )
        }
    }

    override suspend fun deliver(payload: EntryDurablePayload) {
        require(payload.schemaVersion == 1) {
            "Unsupported Viewer Settings Migration payload ${payload.schemaVersion}"
        }
        val decoded = json.decodeFromString(EntryViewerSettingsMigrationPayload.serializer(), payload.value)
        check(feature().applyMigration(decoded) is EntryViewerSettingsRestoreResult.Restored)
    }
}
