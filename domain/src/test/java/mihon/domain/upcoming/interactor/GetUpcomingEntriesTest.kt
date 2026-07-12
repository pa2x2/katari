package mihon.domain.upcoming.interactor

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class GetUpcomingEntriesTest {

    @Test
    fun `subscribe queries all entry types`() = runTest {
        val repository = FakeEntryRepository()

        GetUpcomingEntries(repository).subscribe()

        repository.upcomingTypes shouldBe EntryType.entries.toSet()
    }

    private class FakeEntryRepository : EntryRepository {
        var upcomingTypes: Set<EntryType> = emptySet()

        override suspend fun getEntryById(id: Long): Entry? = null
        override suspend fun getEntryByIdAsFlow(id: Long): Flow<Entry> = error("Not used")
        override suspend fun getEntryByUrlAndSourceId(
            url: String,
            sourceId: Long,
            type: EntryType,
        ): Entry? = null
        override suspend fun getEntryByUrlAndSourceId(
            url: String,
            sourceId: Long,
            type: EntryType,
            profileId: Long,
        ): Entry? = null
        override fun getEntryByUrlAndSourceIdAsFlow(
            url: String,
            sourceId: Long,
            type: EntryType,
        ): Flow<Entry?> = flowOf(null)
        override fun getEntryByUrlAndSourceIdAsFlow(
            url: String,
            sourceId: Long,
            type: EntryType,
            profileId: Long,
        ): Flow<Entry?> = flowOf(null)
        override suspend fun getFavorites(): List<Entry> = emptyList()
        override suspend fun getNonFavoriteIds(entryIds: List<Long>): List<Long> = emptyList()
        override suspend fun getFavoritesByProfile(profileId: Long): List<Entry> = emptyList()
        override suspend fun getAllEntriesByProfile(profileId: Long): List<Entry> = emptyList()
        override suspend fun getReadEntriesNotInLibrary(): List<Entry> = emptyList()
        override suspend fun getReadEntriesNotInLibraryByProfile(profileId: Long): List<Entry> = emptyList()
        override suspend fun getLibraryEntries(): List<Entry> = emptyList()
        override fun getLibraryEntriesAsFlow(): Flow<List<Entry>> = flowOf(emptyList())
        override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Entry>> = flowOf(emptyList())
        override suspend fun getUpcomingEntries(statuses: Set<Int>, types: Set<EntryType>): Flow<List<Entry>> {
            upcomingTypes = types
            return flowOf(emptyList())
        }
        override suspend fun resetViewerFlags(): Boolean = true
        override suspend fun setCategories(entryId: Long, categoryIds: List<Long>) = Unit
        override suspend fun updateDisplayName(entryId: Long, displayName: String?): Boolean = true
        override suspend fun insert(entry: Entry): Long = entry.id
        override suspend fun insertOrUpdate(entry: Entry): Entry = entry
        override suspend fun update(entry: Entry): Boolean = true
        override suspend fun updateFromSource(entry: Entry): Boolean = true
        override suspend fun setFavorite(id: Long, favorite: Boolean): Boolean = true
        override suspend fun setViewerFlags(id: Long, viewerFlags: Long): Boolean = true
        override suspend fun setChapterFlags(id: Long, flags: Long): Boolean = true
        override suspend fun setUpdateStrategy(id: Long, strategy: EntryUpdateStrategy): Boolean = true
        override suspend fun delete(id: Long): Boolean = true
        override suspend fun deleteNonFavorite(): Boolean = true
        override suspend fun getCoverHash(entryId: Long, coverLastModified: Long): Long? = null
        override suspend fun upsertCoverHash(entryId: Long, coverLastModified: Long, hash: Long) = Unit
    }
}
