package mihon.entry.interactions.download.maintenance.migration

import kotlinx.serialization.json.Json
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadMaintenanceResult
import mihon.entry.interactions.download.EntryDownloadRemovalPlan
import mihon.entry.interactions.download.EntryDownloadRemovalPreparation
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.durable.EntryDurablePayload
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent

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
