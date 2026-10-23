package tachiyomi.domain.entry.repository

import tachiyomi.domain.entry.model.Entry

interface EntrySourceSyncRepository {
    suspend fun updateFromSourceSync(
        entry: Entry,
        profileId: Long,
        updateDateAdded: Boolean,
    ): Entry?

    /** Whether [entryId] has stored chapters or listed members, after which whether it is a merged entry is fixed. */
    suspend fun hasStoredChildren(entryId: Long): Boolean
}
