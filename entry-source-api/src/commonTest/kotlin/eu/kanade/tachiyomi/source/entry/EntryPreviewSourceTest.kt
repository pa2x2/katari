package eu.kanade.tachiyomi.source.entry

import kotlin.test.Test
import kotlin.test.assertEquals

class EntryPreviewSourceTest {

    @Test
    fun `preview capability receives Entry-era model and preserves image metadata`() = kotlinx.coroutines.test.runTest {
        val source = TestPreviewSource()
        val entry = SEntry.create().also {
            it.url = "/anime"
            it.title = "Anime"
            it.type = EntryType.ANIME
        }

        val previews = source.getEntryPreview(entry)

        assertEquals(entry, source.requestedEntry)
        assertEquals(
            EntryPreviewImage(
                index = 7,
                imageUrl = "https://example.org/preview.jpg",
                title = "Key visual",
                url = "https://example.org/anime/visual",
            ),
            previews.single(),
        )
    }

    private class TestPreviewSource : EntryPreviewSource {
        override val id = 1L
        override val name = "Preview source"
        var requestedEntry: SEntry? = null

        override suspend fun getEntryPreview(entry: SEntry): List<EntryPreviewImage> {
            requestedEntry = entry
            return listOf(
                EntryPreviewImage(
                    index = 7,
                    imageUrl = "https://example.org/preview.jpg",
                    title = "Key visual",
                    url = "https://example.org/anime/visual",
                ),
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
}
