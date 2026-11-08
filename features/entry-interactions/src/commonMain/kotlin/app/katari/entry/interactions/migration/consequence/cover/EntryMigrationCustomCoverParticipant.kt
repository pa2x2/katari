package app.katari.entry.interactions.migration.consequence.cover

import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.durable.EntryDurablePayload
import app.katari.entry.interactions.migration.EntryMigrationOption
import app.katari.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import app.katari.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import app.katari.entry.interactions.migration.host.EntryMigrationCustomCoverPayload
import kotlinx.serialization.json.Json

internal const val ENTRY_MIGRATION_CUSTOM_COVER_PARTICIPANT_ID = "entry.migration.custom-cover"

internal class EntryMigrationCustomCoverParticipant(
    private val host: EntryMigrationCustomCoverHost,
) : EntryDurableParticipant<EntryMigrationDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = ENTRY_MIGRATION_CUSTOM_COVER_PARTICIPANT_ID

    override suspend fun prepare(event: EntryMigrationDurableEvent): EntryDurablePayload? {
        if (EntryMigrationOption.CUSTOM_COVER !in event.selectedOptions) return null
        return host.stage(event.operationId, event.source, event.target)?.let { payload ->
            EntryDurablePayload(
                schemaVersion = 1,
                value = json.encodeToString(EntryMigrationCustomCoverPayload.serializer(), payload),
            )
        }
    }

    override suspend fun deliver(payload: EntryDurablePayload) {
        host.promote(payload.decodeCustomCover(json))
    }

    override suspend fun discard(payload: EntryDurablePayload) {
        host.discard(payload.decodeCustomCover(json))
    }
}

internal fun EntryDurablePayload.decodeCustomCover(json: Json): EntryMigrationCustomCoverPayload {
    require(schemaVersion == 1) { "Unsupported custom-cover Migration payload $schemaVersion" }
    return json.decodeFromString(EntryMigrationCustomCoverPayload.serializer(), value)
}
