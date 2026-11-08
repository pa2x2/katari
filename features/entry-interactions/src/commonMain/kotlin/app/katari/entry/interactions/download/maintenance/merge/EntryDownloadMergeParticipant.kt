package app.katari.entry.interactions.download.maintenance.merge

import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.entry.interactions.download.EntryDownloadMaintenanceResult
import app.katari.entry.interactions.download.EntryDownloadRemovalPlan
import app.katari.entry.interactions.download.EntryDownloadRemovalPreparation
import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.durable.EntryDurablePayload
import app.katari.entry.interactions.merge.consequence.EntryMergeDurableChange
import app.katari.entry.interactions.merge.consequence.EntryMergeDurableEvent
import kotlinx.serialization.json.Json

internal const val ENTRY_DOWNLOAD_MERGE_PARTICIPANT_ID = "entry.merge.download-removal"

internal class EntryDownloadMergeParticipant(
    private val feature: () -> EntryDownloadMaintenanceFeature,
) : EntryDurableParticipant<EntryMergeDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = ENTRY_DOWNLOAD_MERGE_PARTICIPANT_ID

    override suspend fun prepare(event: EntryMergeDurableEvent): EntryDurablePayload? {
        val removalRequired = event.downloadRemovalRequested ||
            EntryMergeDurableChange.REMOVED_FROM_LIBRARY in event.changes
        if (!removalRequired) return null
        return when (val preparation = feature().prepareRemoval(event.entry)) {
            is EntryDownloadRemovalPreparation.Prepared -> EntryDurablePayload(
                schemaVersion = 2,
                value = json.encodeToString(EntryDownloadRemovalPlan.serializer(), preparation.plan),
            )
            EntryDownloadRemovalPreparation.NothingToRemove,
            is EntryDownloadRemovalPreparation.Inapplicable,
            -> null
        }
    }

    override suspend fun deliver(payload: EntryDurablePayload) {
        require(payload.schemaVersion == 2) { "Unsupported Download Merge payload ${payload.schemaVersion}" }
        val plan = json.decodeFromString(EntryDownloadRemovalPlan.serializer(), payload.value)
        check(feature().applyRemoval(plan) == EntryDownloadMaintenanceResult.Performed) {
            "Merge download removal was not verified"
        }
    }
}
