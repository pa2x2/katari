package mihon.entry.interactions.download.maintenance

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadMaintenanceResult
import mihon.entry.interactions.download.EntryDownloadRemovalPlan
import mihon.entry.interactions.download.EntryDownloadRemovalPreparation
import tachiyomi.domain.entry.model.Entry

/**
 * Downloads of entries removed inside a transaction. They are collected while the rows still exist and deleted only
 * after the transaction commits, so a rolled-back removal keeps its files.
 */
internal class EntryDownloadRemovalCollector(
    private val downloads: () -> EntryDownloadMaintenanceFeature,
) {
    private val owners = linkedMapOf<Pair<Long, Long>, Entry>()

    suspend fun prepare(entries: List<Entry>) {
        entries.forEach { entry ->
            val preparation = downloads().prepareRemoval(entry) as? EntryDownloadRemovalPreparation.Prepared
            preparation?.plan?.owners?.forEach { owner -> owners[owner.profileId to owner.id] = owner }
        }
    }

    suspend fun apply(type: EntryType) {
        val typed = owners.values.filter { it.type == type }
        if (typed.isEmpty()) return
        check(downloads().applyRemoval(EntryDownloadRemovalPlan(typed)) == EntryDownloadMaintenanceResult.Performed) {
            "Download cleanup was incomplete after removing entries"
        }
    }
}
