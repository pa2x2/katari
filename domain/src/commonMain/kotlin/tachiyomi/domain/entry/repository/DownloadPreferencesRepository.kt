package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.DownloadPreferences

interface DownloadPreferencesRepository {

    suspend fun getByEntryId(entryId: Long): DownloadPreferences?

    /** The preferences of those of [entryIds] that have any, by entry id. */
    suspend fun getByEntryIds(entryIds: Collection<Long>): Map<Long, DownloadPreferences>

    fun getByEntryIdAsFlow(entryId: Long): Flow<DownloadPreferences?>

    suspend fun upsert(preferences: DownloadPreferences)
}
