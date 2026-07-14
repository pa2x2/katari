package tachiyomi.domain.source.service

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SourceMetadataResolverTest {

    @Test
    fun `source without metadata remains unknown`() {
        TestSource().resolvedSupportedEntryTypes().shouldBeNull()
    }

    @Test
    fun `source metadata is returned`() {
        MetadataSource().resolvedSupportedEntryTypes() shouldBe setOf(EntryType.MANGA, EntryType.BOOK)
    }
}

private open class TestSource : UnifiedSource {
    override val id = 1L
    override val name = "Test source"

    override suspend fun getPopularContent(page: Int) = EntryPageResult<SEntry>(emptyList(), false)
    override suspend fun getLatestUpdates(page: Int) = EntryPageResult<SEntry>(emptyList(), false)
    override suspend fun getSearchContent(page: Int, query: String, filters: EntryFilterList) =
        EntryPageResult<SEntry>(emptyList(), false)
    override suspend fun getContentDetails(entry: SEntry) = entry
    override suspend fun getChapterList(entry: SEntry) = emptyList<SEntryChapter>()
    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection) =
        EntryMedia.Playback(PlaybackDescriptor())
}

private class MetadataSource : TestSource(), SourceMetadata {
    override val supportedEntryTypes = setOf(EntryType.MANGA, EntryType.BOOK)
}
