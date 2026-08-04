package mihon.domain.upcoming.interactor

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class GetUpcomingEntriesTest {

    @Test
    fun `subscribe queries all entry types`() = runTest {
        val repository = FakeEntryRepository()

        GetUpcomingEntries(repository).subscribe(
            profileId = 1L,
            excludedCategories = listOf(2L),
            includedCategories = listOf(1L),
            hiddenSources = emptySet(),
        )

        repository.upcomingTypes shouldBe EntryType.entries.toSet()
        repository.excludedCategories shouldBe listOf(2L)
        repository.includedCategories shouldBe listOf(1L)
    }

    @Test
    fun `subscribe excludes entries from hidden sources`() = runTest {
        val visible = Entry.create().copy(id = 1L, source = 10L)
        val hidden = Entry.create().copy(id = 2L, source = 20L)
        val repository = FakeEntryRepository(upcomingEntries = listOf(visible, hidden))

        val result = GetUpcomingEntries(repository).subscribe(
            profileId = 1L,
            excludedCategories = emptyList(),
            includedCategories = emptyList(),
            hiddenSources = setOf(20L),
        )

        result.first() shouldBe listOf(visible)
    }

    private class FakeEntryRepository(
        private val upcomingEntries: List<Entry> = emptyList(),
    ) : EntryRepository {
        var upcomingTypes: Set<EntryType> = emptySet()
        var profileId: Long? = null
        var excludedCategories: List<Long> = emptyList()
        var includedCategories: List<Long> = emptyList()

        override suspend fun getEntryById(id: Long): Entry? = null
        override suspend fun getEntryById(id: Long, profileId: Long): Entry? = null
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
        override suspend fun getNonLibraryEntriesBySources(
            sourceIds: List<Long>,
            keepReadEntries: Boolean,
        ): List<Entry> = emptyList()
        override suspend fun getLibraryEntries(): List<Entry> = emptyList()
        override fun getLibraryEntriesAsFlow(): Flow<List<Entry>> = flowOf(emptyList())
        override fun getLibraryEntriesAsFlow(profileId: Long): Flow<List<Entry>> = flowOf(emptyList())
        override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Entry>> = flowOf(emptyList())
        override suspend fun getUpcomingEntries(
            profileId: Long,
            statuses: Set<Int>,
            types: Set<EntryType>,
            excludedCategories: List<Long>,
            includedCategories: List<Long>,
        ): Flow<List<Entry>> {
            this.profileId = profileId
            upcomingTypes = types
            this.excludedCategories = excludedCategories
            this.includedCategories = includedCategories
            return flowOf(upcomingEntries)
        }
        override suspend fun resetViewerFlags(): Boolean = true
        override suspend fun setCategories(entryId: Long, categoryIds: List<Long>) = Unit
        override suspend fun updateDisplayName(entryId: Long, displayName: String?): Boolean = true
        override suspend fun insert(entry: Entry): Long = entry.id
        override suspend fun insertOrUpdate(entry: Entry): Entry = entry
        override suspend fun insertOrUpdate(entry: Entry, profileId: Long): Entry = entry
        override suspend fun update(entry: Entry): Boolean = true
        override suspend fun update(entry: Entry, profileId: Long): Boolean = true
        override suspend fun updateFromSource(entry: Entry): Boolean = true
        override suspend fun setViewerFlags(id: Long, viewerFlags: Long): Boolean = true
        override suspend fun setChapterFlags(id: Long, flags: Long): Boolean = true
        override suspend fun setUpdateStrategy(id: Long, strategy: EntryUpdateStrategy): Boolean = true
        override suspend fun getCoverHash(entryId: Long, coverLastModified: Long): Long? = null
        override suspend fun upsertCoverHash(entryId: Long, coverLastModified: Long, hash: Long) = Unit
    }
}
