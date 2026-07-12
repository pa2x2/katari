package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.source.CatalogSearchPagingSource
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.service.SourceManager

class EntryCatalogSourceTest {

    @Test
    fun `catalog search preserves mixed entry result types with same url`() = runTest {
        val source = FakeEntryCatalogSource(
            searchItems = listOf(
                sourceEntry(url = "/same", title = "Manga", type = EntryType.MANGA),
                sourceEntry(url = "/same", title = "Anime", type = EntryType.ANIME),
            ),
        )
        val pagingSource = CatalogSearchPagingSource(
            sourceId = source.id,
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            sourceManager = FakeSourceManager(source),
            query = "same",
            filters = EntryFilterList(),
            networkToLocalEntry = NetworkToLocalEntry(InMemoryEntryRepository()),
        )

        val result = pagingSource.load(PagingSource.LoadParams.Refresh(1L, 25, false))

        val page = result as PagingSource.LoadResult.Page<Long, CatalogListItem>
        page.data.map { (it as CatalogListItem.EntryItem).entry.type } shouldContainExactly listOf(
            EntryType.MANGA,
            EntryType.ANIME,
        )
        page.data.map { (it as CatalogListItem.EntryItem).entry.url } shouldContainExactly listOf("/same", "/same")
    }

    @Test
    fun `catalog search keeps local favorite state for existing entries`() = runTest {
        val source = FakeEntryCatalogSource(
            searchItems = listOf(
                sourceEntry(url = "/favorite", title = "Favorite", type = EntryType.MANGA),
            ),
        )
        val localEntry = Entry.create().copy(
            id = 10L,
            source = source.id,
            url = "/favorite",
            title = "Favorite",
            favorite = true,
            type = EntryType.MANGA,
            profileId = 1L,
        )
        val pagingSource = CatalogSearchPagingSource(
            sourceId = source.id,
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            sourceManager = FakeSourceManager(source),
            query = "favorite",
            filters = EntryFilterList(),
            networkToLocalEntry = NetworkToLocalEntry(InMemoryEntryRepository(existingEntries = listOf(localEntry))),
        )

        val result = pagingSource.load(PagingSource.LoadParams.Refresh(1L, 25, false))

        val page = result as PagingSource.LoadResult.Page<Long, CatalogListItem>
        val entry = (page.data.single() as CatalogListItem.EntryItem).entry
        entry.id shouldBe localEntry.id
        entry.favorite shouldBe true
        entry.profileId shouldBe localEntry.profileId
    }

    @Test
    fun `catalog filter loader uses entry source filter list without legacy catalogue source`() = runTest {
        val filters = EntryFilterList(EntryFilter.Header("Entry filter"))
        val source = FakeEntryCatalogSource(filters = filters)
        val loader = CatalogFilterLoader(FakeSourceManager(source))

        loader.hasAsyncFilters(source.id) shouldBe false
        loader.load(source.id).first().name shouldBe "Entry filter"
    }

    @Test
    fun `stub source does not expose canonical entry type`() {
        val stub = StubSource(id = 1L, lang = "all", name = "Mixed Missing Source")
        val unified: UnifiedSource = UnifiedStubSource(stub)

        unified.id shouldBe 1L
        unified.name shouldBe "Mixed Missing Source"
        (unified is EntryCatalogueSource) shouldBe false
    }
}

private class FakeEntryCatalogSource(
    override val id: Long = 1L,
    override val name: String = "Mixed Source",
    override val lang: String = "en",
    override val supportsLatest: Boolean = true,
    private val filters: EntryFilterList = EntryFilterList(),
    private val searchItems: List<SEntry> = emptyList(),
) : EntryCatalogueSource {

    override fun getFilterList(): EntryFilterList = filters

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> = EntryPageResult(emptyList(), false)

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> = EntryPageResult(emptyList(), false)

    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> = EntryPageResult(searchItems, false)

    override suspend fun getContentDetails(entry: SEntry): SEntry = entry

    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> = emptyList()

    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia =
        error("Not used")
}

private class FakeSourceManager(
    private val source: EntryCatalogueSource,
) : SourceManager {

    override val isInitialized = MutableStateFlow(true)
    override val sources: Flow<List<UnifiedSource>> = flowOf(listOf(source))

    override fun get(sourceKey: Long): UnifiedSource? = source.takeIf { it.id == sourceKey }
    override fun getOrStub(sourceKey: Long): UnifiedSource =
        get(sourceKey) ?: UnifiedStubSource(StubSource(sourceKey, "", ""))
    override fun getAll(): List<UnifiedSource> = listOf(source)
    override fun getCatalogueSources(): List<UnifiedSource> = listOf(source)
    override fun getCatalogueSource(sourceKey: Long): EntryCatalogueSource? = source.takeIf { it.id == sourceKey }
    override fun getOnlineSources(): List<UnifiedSource> = emptyList()
    override fun getStubSources(): List<UnifiedSource> = emptyList()
    override fun getDisplayInfo(sourceKey: Long): SourceDisplayInfo {
        val source = get(sourceKey)
        return SourceDisplayInfo(
            id = sourceKey,
            name = source?.name ?: sourceKey.toString(),
            lang = (source as? EntryCatalogueSource)?.lang.orEmpty(),
            isMissing = source == null,
        )
    }
}

private class InMemoryEntryRepository(
    existingEntries: List<Entry> = emptyList(),
) : EntryRepository {
    private var nextId = 1L
    private val entries = existingEntries.associateBy { Triple(it.source, it.url, it.type) }.toMutableMap()

    override suspend fun insertOrUpdate(entry: Entry): Entry {
        return entries.getOrPut(Triple(entry.source, entry.url, entry.type)) {
            entry.copy(id = nextId++, profileId = 1L)
        }
    }
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
    override suspend fun getUpcomingEntries(
        statuses: Set<Int>,
        types: Set<EntryType>,
    ): Flow<List<Entry>> = flowOf(emptyList())
    override suspend fun resetViewerFlags(): Boolean = false
    override suspend fun setCategories(entryId: Long, categoryIds: List<Long>) = Unit
    override suspend fun updateDisplayName(entryId: Long, displayName: String?): Boolean = false
    override suspend fun insert(entry: Entry): Long = nextId++
    override suspend fun update(entry: Entry): Boolean = false
    override suspend fun updateFromSource(entry: Entry): Boolean = false
    override suspend fun setFavorite(id: Long, favorite: Boolean): Boolean = false
    override suspend fun setViewerFlags(id: Long, viewerFlags: Long): Boolean = false
    override suspend fun setChapterFlags(id: Long, flags: Long): Boolean = false
    override suspend fun setUpdateStrategy(id: Long, strategy: EntryUpdateStrategy): Boolean = false
    override suspend fun delete(id: Long): Boolean = false
    override suspend fun deleteNonFavorite(): Boolean = false
    override suspend fun getCoverHash(entryId: Long, coverLastModified: Long): Long? = null
    override suspend fun upsertCoverHash(entryId: Long, coverLastModified: Long, hash: Long) = Unit
}

private fun sourceEntry(url: String, title: String, type: EntryType): SEntry = SEntry.create().also {
    it.url = url
    it.title = title
    it.type = type
}
