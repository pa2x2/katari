package eu.kanade.tachiyomi.source.entry

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RelatedEntriesSourceTest {

    @Test
    fun `related entries capability accepts mixed entry types`() = runTest {
        val source = TestRelatedEntriesSource()
        val entry = entry("/book", "Book", EntryType.BOOK)

        val relatedEntries = source.getRelatedEntries(entry)

        assertEquals(entry, source.requestedEntry)
        assertEquals(listOf(EntryType.MANGA, EntryType.ANIME), relatedEntries.map(SEntry::type))
    }

    private class TestRelatedEntriesSource : RelatedEntriesSource {
        override val id = 1L
        override val name = "Related entries source"
        var requestedEntry: SEntry? = null

        override suspend fun getRelatedEntries(entry: SEntry): List<SEntry> {
            requestedEntry = entry
            return listOf(
                entry("/manga", "Manga", EntryType.MANGA),
                entry("/anime", "Anime", EntryType.ANIME),
            )
        }

        override suspend fun getPopularContent(page: Int) = EntryPageResult<SEntry>(emptyList(), false)
        override suspend fun getLatestUpdates(page: Int) = EntryPageResult<SEntry>(emptyList(), false)
        override suspend fun getSearchContent(page: Int, query: String, filters: EntryFilterList) =
            EntryPageResult<SEntry>(emptyList(), false)
        override suspend fun getContentDetails(entry: SEntry) = entry
        override suspend fun getChapterList(entry: SEntry) = emptyList<SEntryChapter>()
        override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection) =
            EntryMedia.Playback(PlaybackDescriptor())
    }

    private companion object {
        fun entry(url: String, title: String, type: EntryType): SEntry = SEntry.create().apply {
            this.url = url
            this.title = title
            this.type = type
        }
    }
}
