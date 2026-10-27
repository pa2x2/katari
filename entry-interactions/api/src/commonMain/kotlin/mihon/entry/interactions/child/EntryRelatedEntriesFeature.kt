package mihon.entry.interactions.child

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry

/** Feature-owned boundary for source-provided related-entry discovery. */
interface EntryRelatedEntriesFeature {
    fun availability(context: EntryRelatedEntriesContext): EntryRelatedEntriesAvailability

    suspend fun load(entryId: Long): EntryRelatedEntriesLoadResult

    suspend fun observeEntries(entryIds: List<Long>): Flow<List<Entry>>
}
