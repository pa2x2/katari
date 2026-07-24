package mihon.entry.interactions

import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMergePendingConsequence
import tachiyomi.domain.entry.model.Entry

/**
 * Finite compatibility for pending rows written before Merge consequences became owner-contributed.
 *
 * Schema 1 contained no participant-owned payload; its Entry subject lived only in the queue row. New consequences
 * start at schema 2 and never pass through this adapter.
 */
internal class EntryMergeLegacyConsequenceDelivery(
    private val host: EntryMergeHost,
    private val tracking: () -> EntryTrackingFeature,
    private val coverCleanup: suspend (Entry) -> Unit,
    private val downloadMaintenance: () -> EntryDownloadMaintenanceFeature,
) {
    fun handles(consequence: EntryMergePendingConsequence): Boolean {
        return consequence.schemaVersion == LEGACY_SCHEMA_VERSION &&
            consequence.participantId in LEGACY_PARTICIPANT_IDS
    }

    suspend fun deliver(consequence: EntryMergePendingConsequence) {
        require(handles(consequence)) {
            "Merge consequence ${consequence.participantId} is not a recognized schema-1 compatibility record"
        }
        val entry = host.profile(consequence.profileId).entries(listOf(consequence.entryId)).singleOrNull()
            ?: return
        when (consequence.participantId) {
            ENTRY_TRACKING_MERGE_PARTICIPANT.id.value -> tracking().bindAutomatically(entry)
            ENTRY_MERGE_CUSTOM_COVER_PARTICIPANT.id.value -> coverCleanup(entry)
            ENTRY_DOWNLOAD_MERGE_PARTICIPANT.id.value -> {
                check(
                    downloadMaintenance().removeEntryDownloads(entry) == EntryDownloadMaintenanceResult.Performed,
                ) { "Legacy Merge download removal was not verified" }
            }
            else -> error("Unrecognized schema-1 Merge consequence ${consequence.participantId}")
        }
    }

    private companion object {
        const val LEGACY_SCHEMA_VERSION = 1
        val LEGACY_PARTICIPANT_IDS = setOf(
            ENTRY_TRACKING_MERGE_PARTICIPANT.id.value,
            ENTRY_MERGE_CUSTOM_COVER_PARTICIPANT.id.value,
            ENTRY_DOWNLOAD_MERGE_PARTICIPANT.id.value,
        )
    }
}
