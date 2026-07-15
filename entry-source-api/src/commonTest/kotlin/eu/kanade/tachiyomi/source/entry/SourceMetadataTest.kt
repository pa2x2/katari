package eu.kanade.tachiyomi.source.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourceMetadataTest {

    @Test
    fun `source metadata advertises supported entry types`() {
        val supportedTypes = setOf(EntryType.MANGA, EntryType.ANIME, EntryType.BOOK)
        val source = TestSource(supportedTypes)

        assertEquals(supportedTypes, source.supportedEntryTypes())
    }

    @Test
    fun `empty source metadata is treated as unavailable`() {
        assertNull(TestSource(emptySet()).supportedEntryTypes())
    }

    private class TestSource(
        override val supportedEntryTypes: Set<EntryType>,
    ) : UnifiedSource, SourceMetadata {
        override val id = 1L
        override val name = "Metadata source"

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
