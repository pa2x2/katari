package tachiyomi.domain.entry.repository

import tachiyomi.domain.entry.model.Entry

interface EntrySourceSyncRepository {
    suspend fun updateFromSourceSync(
        entry: Entry,
        profileId: Long,
        updateDateAdded: Boolean,
    ): Entry?
}
