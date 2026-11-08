package app.katari.entry.interactions.download.maintenance.migration

import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.entry.interactions.download.EntryDownloadMaintenanceResult
import app.katari.entry.interactions.download.EntryDownloadRemovalPlan
import app.katari.entry.interactions.download.EntryDownloadRemovalPreparation
import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.durable.EntryDurablePayload
import app.katari.entry.interactions.migration.EntryMigrationOption
import app.katari.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import kotlinx.serialization.json.Json

internal class EntryDownloadMigrationParticipant(
    private val feature: () -> EntryDownloadMaintenanceFeature,
) : EntryDurableParticipant<EntryMigrationDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = "entry.migration.download-removal"

    override suspend fun prepare(event: EntryMigrationDurableEvent): EntryDurablePayload? {
        if (EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS !in event.selectedOptions) return null
        return when (val result = feature().prepareRemoval(event.source)) {
            is EntryDownloadRemovalPreparation.Prepared -> EntryDurablePayload(
                schemaVersion = 1,
                value = json.encodeToString(EntryDownloadRemovalPlan.serializer(), result.plan),
            )
            EntryDownloadRemovalPreparation.NothingToRemove,
            is EntryDownloadRemovalPreparation.Inapplicable,
            -> null
        }
    }

    override suspend fun deliver(payload: EntryDurablePayload) {
        require(payload.schemaVersion == 1) { "Unsupported Download Migration payload ${payload.schemaVersion}" }
        val plan = json.decodeFromString(EntryDownloadRemovalPlan.serializer(), payload.value)
        check(feature().applyRemoval(plan) == EntryDownloadMaintenanceResult.Performed) {
            "Migration download removal was not verified"
        }
    }
}
