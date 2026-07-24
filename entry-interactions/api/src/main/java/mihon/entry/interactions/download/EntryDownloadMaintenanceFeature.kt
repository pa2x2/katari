package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.serialization.Serializable
import tachiyomi.domain.entry.model.Entry

/** Feature-owned boundary for download storage maintenance outside normal download actions. */
interface EntryDownloadMaintenanceFeature {
    fun invalidateCaches(): EntryDownloadMaintenanceResult

    fun renameSource(
        oldSource: UnifiedSource,
        newSource: UnifiedSource,
    ): EntryDownloadMaintenanceResult

    suspend fun renameEntry(
        entry: Entry,
        newTitle: String,
    ): EntryDownloadMaintenanceResult

    suspend fun inspectEntry(entry: Entry): EntryDownloadMaintenanceInspection

    suspend fun removeEntryDownloads(entry: Entry): EntryDownloadMaintenanceResult

    /** Captures concrete download owners before another feature can change Merge membership. */
    suspend fun prepareRemoval(entry: Entry): EntryDownloadRemovalPreparation

    /** Deletes only the captured owners and verifies that no owner still reports downloads. */
    suspend fun applyRemoval(plan: EntryDownloadRemovalPlan): EntryDownloadMaintenanceResult
}

@Serializable
data class EntryDownloadRemovalPlan(val owners: List<Entry>)

sealed interface EntryDownloadRemovalPreparation {
    data class Prepared(val plan: EntryDownloadRemovalPlan) : EntryDownloadRemovalPreparation
    data object NothingToRemove : EntryDownloadRemovalPreparation
    data class Inapplicable(val type: EntryType) : EntryDownloadRemovalPreparation
}

sealed interface EntryDownloadMaintenanceResult {
    data object Performed : EntryDownloadMaintenanceResult

    /** Aggregate maintenance had no Download providers to visit. */
    data object NoParticipants : EntryDownloadMaintenanceResult

    data class Inapplicable(
        val types: Set<EntryType>,
    ) : EntryDownloadMaintenanceResult

    data class Incomplete(
        val remainingOwners: List<Entry>,
    ) : EntryDownloadMaintenanceResult
}

sealed interface EntryDownloadMaintenanceInspection {
    data object HasDownloads : EntryDownloadMaintenanceInspection

    data object NoDownloads : EntryDownloadMaintenanceInspection

    data class Inapplicable(
        val type: EntryType,
    ) : EntryDownloadMaintenanceInspection
}
