package eu.kanade.tachiyomi.source.failure

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryRefresh
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import java.io.IOException

class ExtensionSourceContainmentTest {

    private val source = containExtensionCode(BrokenSource()) as EntryCatalogueSource

    @Test
    fun `a member that can't link fails that call instead of ending the app`() {
        shouldThrow<ExtensionCodeException> { source.getFilterList() }
            .cause.shouldBeInstanceOf<NoClassDefFoundError>()
    }

    @Test
    fun `a suspending member that can't link after it suspended fails that call instead of ending the app`() = runTest {
        shouldThrow<ExtensionCodeException> { source.getPopularContent(1) }
            .cause.shouldBeInstanceOf<AbstractMethodError>()
    }

    @Test
    fun `a checked exception a suspending member throws before suspending reaches the caller as it was thrown`() =
        runTest {
            shouldThrow<IOException> { source.getLatestUpdates(1) }.message shouldBe "offline"
        }

    @Test
    fun `a contained source equals itself, so keys made of it still match`() {
        source.equals(source) shouldBe true
    }
}

private class BrokenSource : EntryCatalogueSource {
    override val id = 1L
    override val name = "Broken"
    override val lang = "en"
    override val supportsLatest = true

    override fun getFilterList(): EntryFilterList = throw NoClassDefFoundError("eu/kanade/tachiyomi/Removed")

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> {
        yield()
        throw AbstractMethodError("getPopularContent")
    }

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> = throw IOException("offline")

    override suspend fun getSearchContent(page: Int, query: String, filters: EntryFilterList) =
        EntryPageResult<SEntry>(emptyList(), hasNextPage = false)

    override suspend fun refresh(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ) = EntryRefresh(entry, existingChapters)

    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia =
        EntryMedia.ImagePages(emptyList())
}
