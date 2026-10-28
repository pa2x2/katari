package mihon.desktop.download

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadMaintenanceInspection
import mihon.entry.interactions.download.EntryDownloadMaintenanceResult
import mihon.entry.interactions.download.EntryDownloadRemovalPlan
import mihon.entry.interactions.download.EntryDownloadRemovalPreparation
import tachiyomi.domain.entry.model.Entry

/**
 * Download storage that no entry type keeps anything in, as desktop downloads nothing: there is nothing to maintain or
 * remove. Stands in for the shared download maintenance, which needs the download and merge features desktop doesn't
 * host yet.
 */
@Inject
class NoEntryDownloadMaintenanceFeature : EntryDownloadMaintenanceFeature {
    override fun invalidateCaches(): EntryDownloadMaintenanceResult = EntryDownloadMaintenanceResult.NoParticipants

    override fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource): EntryDownloadMaintenanceResult =
        EntryDownloadMaintenanceResult.NoParticipants

    override suspend fun renameEntry(entry: Entry, newTitle: String): EntryDownloadMaintenanceResult =
        EntryDownloadMaintenanceResult.NoParticipants

    override suspend fun inspectEntry(entry: Entry): EntryDownloadMaintenanceInspection =
        EntryDownloadMaintenanceInspection.NoDownloads

    override suspend fun removeEntryDownloads(entry: Entry): EntryDownloadMaintenanceResult =
        EntryDownloadMaintenanceResult.NoParticipants

    override suspend fun prepareRemoval(entry: Entry): EntryDownloadRemovalPreparation =
        EntryDownloadRemovalPreparation.NothingToRemove

    override suspend fun applyRemoval(plan: EntryDownloadRemovalPlan): EntryDownloadMaintenanceResult =
        EntryDownloadMaintenanceResult.NoParticipants
}
