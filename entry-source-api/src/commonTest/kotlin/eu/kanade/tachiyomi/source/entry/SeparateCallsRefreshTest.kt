package eu.kanade.tachiyomi.source.entry

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class SeparateCallsRefreshTest {

    @Test
    fun `a source built against SDK 2_6 is refreshed through its own calls`() = runTest {
        val source = Sdk26Source()
        val stored = listOf(chapter("/1"))

        val refresh = source.refresh(entry(), stored, fetchDetails = true, fetchChapters = true)

        assertEquals("Details", refresh.entry.title)
        assertEquals(listOf("/1", "/2"), refresh.chapters.map { it.url })
        assertSame(stored, source.existingChapters)
        // As when the host made both calls itself: the chapters see the memo the details set, under the host's url
        assertEquals("/entry", source.chapterEntry.url)
        assertEquals(Sdk26Source.DETAILS_MEMO, source.chapterEntry.memo)
    }

    @Test
    fun `a source implementing neither refresh nor the separate calls fails instead of recursing`() = runTest {
        val source = BareSource()

        assertFailsWith<IllegalStateException> {
            source.refresh(entry(), emptyList(), fetchDetails = true, fetchChapters = true)
        }
        assertFailsWith<IllegalStateException> {
            @Suppress("DEPRECATION")
            source.getContentDetails(entry())
        }
    }
}

private fun entry() = SEntry.create().apply {
    url = "/entry"
    title = "Stored"
}

private fun chapter(url: String) = SEntryChapter.create().apply {
    this.url = url
    name = url
}

private open class BareSource : UnifiedSource {
    override val id = 1L
    override val name = "Source"

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> = error("Unused")
    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> = error("Unused")
    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> = error("Unused")
    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia = error("Unused")
}

// Built the way sources compiled against SDK 2.6 are, which is what the deprecations warn new sources away from
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
private class Sdk26Source : BareSource(), IncrementalChapterSource {
    lateinit var chapterEntry: SEntry
    lateinit var existingChapters: List<SEntryChapter>

    override suspend fun getContentDetails(entry: SEntry): SEntry = SEntry.create().apply {
        url = ""
        title = "Details"
        memo = DETAILS_MEMO
    }

    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> = error("Incremental sources aren't asked")

    override suspend fun getChapterList(entry: SEntry, existingChapters: List<SEntryChapter>): List<SEntryChapter> {
        chapterEntry = entry
        this.existingChapters = existingChapters
        return existingChapters + chapter("/2")
    }

    companion object {
        val DETAILS_MEMO = buildJsonObject { put("id", JsonPrimitive(7)) }
    }
}
