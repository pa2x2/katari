package tachiyomi.domain.entry.repository

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry

interface EntryRepository {

    suspend fun getEntryById(id: Long): Entry?

    suspend fun getEntryByIdAsFlow(id: Long): Flow<Entry>

    suspend fun getEntryByUrlAndSourceId(
        url: String,
        sourceId: Long,
        type: EntryType,
    ): Entry?

    suspend fun getEntryByUrlAndSourceId(
        url: String,
        sourceId: Long,
        type: EntryType,
        profileId: Long,
    ): Entry?

    fun getEntryByUrlAndSourceIdAsFlow(
        url: String,
        sourceId: Long,
        type: EntryType,
    ): Flow<Entry?>

    fun getEntryByUrlAndSourceIdAsFlow(
        url: String,
        sourceId: Long,
        type: EntryType,
        profileId: Long,
    ): Flow<Entry?>

    suspend fun getFavorites(): List<Entry>

    suspend fun getNonFavoriteIds(entryIds: List<Long>): List<Long>

    suspend fun getFavoritesByProfile(profileId: Long): List<Entry>

    suspend fun getAllEntriesByProfile(profileId: Long): List<Entry>

    suspend fun getReadEntriesNotInLibrary(): List<Entry>

    suspend fun getReadEntriesNotInLibraryByProfile(profileId: Long): List<Entry>

    suspend fun getLibraryEntries(): List<Entry>

    fun getLibraryEntriesAsFlow(): Flow<List<Entry>>

    suspend fun getLibraryLastRead(): Map<Long, Long> = emptyMap()

    fun getFavoritesBySourceId(sourceId: Long): Flow<List<Entry>>

    suspend fun getUpcomingEntries(
        statuses: Set<Int>,
        types: Set<EntryType>,
    ): Flow<List<Entry>>

    suspend fun resetViewerFlags(): Boolean

    suspend fun setCategories(entryId: Long, categoryIds: List<Long>)

    suspend fun updateDisplayName(entryId: Long, displayName: String?): Boolean

    suspend fun insert(entry: Entry): Long

    suspend fun insertOrUpdate(entry: Entry): Entry

    suspend fun update(entry: Entry): Boolean

    suspend fun updateFromSource(entry: Entry): Boolean

    suspend fun setViewerFlags(id: Long, viewerFlags: Long): Boolean

    suspend fun setChapterFlags(id: Long, flags: Long): Boolean

    suspend fun setUpdateStrategy(id: Long, strategy: EntryUpdateStrategy): Boolean

    suspend fun delete(id: Long): Boolean

    suspend fun deleteNonFavorite(): Boolean

    suspend fun getCoverHash(entryId: Long, coverLastModified: Long): Long?

    suspend fun upsertCoverHash(entryId: Long, coverLastModified: Long, hash: Long)
}
