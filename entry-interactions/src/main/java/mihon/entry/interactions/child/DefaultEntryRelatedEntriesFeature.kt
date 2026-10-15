package mihon.entry.interactions.child

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.RelatedEntriesSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.identity
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.SourceManager

internal class DefaultEntryRelatedEntriesFeature(
    private val sourceManager: SourceManager,
    private val networkToLocalEntry: NetworkToLocalEntry,
    private val getEntry: GetEntry,
    private val sourceDescription: EntrySourceDescriptionResolutionPort,
) : EntryRelatedEntriesFeature {

    override fun availability(context: EntryRelatedEntriesContext): EntryRelatedEntriesAvailability {
        val source = context.source
        if (source != null) {
            check(source.id == context.entry.source) {
                "Related Entries context source ${source.id} does not own Entry source ${context.entry.source}"
            }
        }
        return availability(context.entry.type, source)
    }

    override suspend fun load(entryId: Long): EntryRelatedEntriesLoadResult {
        val entry = getEntry.await(entryId) ?: error("Entry $entryId was not found")
        val source = sourceManager.get(entry.source)
        val availability = availability(entry.type, source)
        if (availability is EntryRelatedEntriesAvailability.Unavailable) {
            return EntryRelatedEntriesLoadResult.Unavailable(availability.reason)
        }
        val orientation = (availability as EntryRelatedEntriesAvailability.Available).orientation
        val relatedSource = source as RelatedEntriesSource
        val entries = relatedSource.getRelatedEntries(entry.toSEntry())
            .map { it.toEntry(source.id) }
            .distinctBy(Entry::identity)
            .let { networkToLocalEntry(it, entry.profileId) }
        return EntryRelatedEntriesLoadResult.Loaded(entries, orientation)
    }

    override suspend fun observeEntries(entryIds: List<Long>): Flow<List<Entry>> {
        return getEntry.subscribe(entryIds)
    }

    private fun availability(type: EntryType, source: UnifiedSource?): EntryRelatedEntriesAvailability {
        return if (source is RelatedEntriesSource) {
            EntryRelatedEntriesAvailability.Available(sourceDescription.describe(source).itemOrientation)
        } else {
            EntryRelatedEntriesAvailability.Unavailable(
                if (source == null) {
                    EntryRelatedEntriesUnavailableReason.SOURCE_MISSING
                } else {
                    EntryRelatedEntriesUnavailableReason.SOURCE_UNSUPPORTED
                },
            )
        }
    }
}
