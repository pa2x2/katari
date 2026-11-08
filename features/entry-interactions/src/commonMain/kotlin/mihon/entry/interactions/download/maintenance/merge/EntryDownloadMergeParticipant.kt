package mihon.entry.interactions.download.maintenance.merge

import kotlinx.serialization.json.Json
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadMaintenanceResult
import mihon.entry.interactions.download.EntryDownloadRemovalPlan
import mihon.entry.interactions.download.EntryDownloadRemovalPreparation
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.durable.EntryDurablePayload
import mihon.entry.interactions.merge.consequence.EntryMergeDurableChange
import mihon.entry.interactions.merge.consequence.EntryMergeDurableEvent

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
