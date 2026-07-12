package mihon.feature.migration.list.search

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SmartSourceSearchEngineTest {

    @Test
    fun `regular search only returns candidates matching requested entry type`() = runTest {
        val source = FakeEntryCatalogSource(
            searchItems = listOf(
                sourceEntry(url = "/same", title = "Same", type = EntryType.MANGA),
                sourceEntry(url = "/same", title = "Same", type = EntryType.ANIME),
            ),
        )
        val engine = SmartSourceSearchEngine(extraSearchParams = null)

        engine.regularSearch(source, "Same", EntryType.ANIME)?.type shouldBe EntryType.ANIME
        engine.regularSearch(source, "Same", EntryType.MANGA)?.type shouldBe EntryType.MANGA
    }
}

private class FakeEntryCatalogSource(
    override val id: Long = 1L,
    override val name: String = "Mixed Source",
    override val lang: String = "en",
    override val supportsLatest: Boolean = true,
    private val searchItems: List<SEntry> = emptyList(),
) : EntryCatalogueSource {

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> = EntryPageResult(emptyList(), false)

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> = EntryPageResult(emptyList(), false)

    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> = EntryPageResult(searchItems, false)

    override suspend fun getContentDetails(entry: SEntry): SEntry = entry

    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> = emptyList()

    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia = error("Not used")
}

private fun sourceEntry(url: String, title: String, type: EntryType): SEntry {
    return SEntry.create().also {
        it.url = url
        it.title = title
        it.type = type
    }
}
