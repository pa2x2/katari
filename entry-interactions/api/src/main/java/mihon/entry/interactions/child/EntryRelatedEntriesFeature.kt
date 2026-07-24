package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry

/** Feature-owned boundary for source-provided related-entry discovery. */
interface EntryRelatedEntriesFeature {
    fun availability(context: EntryRelatedEntriesContext): EntryRelatedEntriesAvailability

    suspend fun load(entryId: Long): EntryRelatedEntriesLoadResult

    fun observeEntry(entry: Entry): Flow<Entry>
}
