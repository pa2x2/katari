package eu.kanade.tachiyomi.source.adapter

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.IncrementalChapterSource
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.supportedEntryTypes
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import rx.Observable
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

class LegacyMangaSourceAdapterTest {

    @Test
    fun `precompiled upstream 1_4 fixture links and runs against current runtime`() = runTest {
        val fixture = Class.forName("legacy.fixture.Legacy14Fixture")
            .getDeclaredConstructor()
            .newInstance() as Source
        val factory = Class.forName("legacy.fixture.Legacy14Fixture\$Factory")
            .getDeclaredConstructor()
            .newInstance() as SourceFactory

        factory.createSources().single().javaClass shouldBe fixture.javaClass

        val adapted = fixture.asUnifiedSource()
        adapted.getPopularContent(1).items.single().type shouldBe EntryType.MANGA

        val details = invokeLegacyBridge(fixture, "callLegacyMangaDetails") as SManga
        details.title shouldBe "Legacy manga details"

        val chapters = invokeLegacyBridge(fixture, "callLegacyChapterList") as List<*>
        chapters shouldHaveSize 1
    }

    @Test
    fun `legacy Rx source is adapted through upstream compatibility bridges`() = runTest {
        val source = LegacyRxCatalogueSource()
        val adapted = source.asUnifiedSource()
        adapted.supportedEntryTypes() shouldBe setOf(EntryType.MANGA)

        val catalogEntry = adapted.getPopularContent(1).items.single()
        catalogEntry.type shouldBe EntryType.MANGA
        catalogEntry.url shouldBe "/manga"

        val details = adapted.getContentDetails(catalogEntry)
        details.type shouldBe EntryType.MANGA
        details.title shouldBe "Legacy manga details"

        val chapters = adapted.getChapterList(details)
        chapters shouldHaveSize 1
        chapters.single().url shouldBe "/chapter-1"

        val media = adapted.getMedia(chapters.single(), PlaybackSelection()) as EntryMedia.ImagePages
        media.pages shouldHaveSize 1
        media.pages.single().imageUrl shouldBe "https://example.invalid/page.jpg"
    }

    @Test
    fun `legacy conversion rejects non manga entry types`() = runTest {
        val adapted = LegacyRxCatalogueSource().asUnifiedSource()
        val anime = SEntry.create().apply {
            url = "/anime"
            title = "Anime"
            type = EntryType.ANIME
        }

        (runCatching { adapted.getContentDetails(anime) }.exceptionOrNull() is IllegalArgumentException) shouldBe true
    }

    @Test
    fun `legacy chapter refresh receives existing chapters`() = runTest {
        val source = CapturingLegacySource()
        val adapted = source.asUnifiedSource() as IncrementalChapterSource
        val entry = SEntry.create().apply {
            url = "/manga"
            title = "Legacy manga"
            type = EntryType.MANGA
        }
        val existing = SEntryChapter.create().apply {
            url = "/existing"
            name = "Chapter 1"
            chapterNumber = 1.0
        }

        adapted.getChapterList(entry, listOf(existing))

        source.receivedChapters.single().url shouldBe "/existing"
        source.receivedChapters.single().chapter_number shouldBe 1.0f
    }
}

private suspend fun invokeLegacyBridge(instance: Any, methodName: String): Any? =
    suspendCoroutineUninterceptedOrReturn { continuation ->
        instance.javaClass
            .getMethod(methodName, kotlin.coroutines.Continuation::class.java)
            .invoke(instance, continuation)
    }

private open class LegacyRxCatalogueSource : CatalogueSource {
    override val id: Long = 1L
    override val name: String = "Legacy source"
    override val lang: String = "en"
    override val supportsLatest: Boolean = true

    override fun getFilterList(): FilterList = FilterList()

    @Deprecated("Legacy Rx compatibility API")
    override fun fetchPopularManga(
        page: Int,
    ): Observable<MangasPage> = popularMangaPage()

    @Deprecated("Legacy Rx compatibility API")
    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> = popularMangaPage()

    @Deprecated("Legacy Rx compatibility API")
    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<MangasPage> = popularMangaPage()

    @Deprecated("Legacy Rx compatibility API")
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = Observable.just(
        manga().apply {
            title = "Legacy manga details"
        },
    )

    @Deprecated("Legacy Rx compatibility API")
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = Observable.just(listOf(chapter()))

    @Deprecated("Legacy Rx compatibility API")
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> = Observable.just(
        listOf(Page(index = 0, imageUrl = "https://example.invalid/page.jpg")),
    )

    private fun popularMangaPage(): Observable<MangasPage> = Observable.just(MangasPage(listOf(manga()), false))

    private fun manga(): SManga = SManga.create().apply {
        url = "/manga"
        title = "Legacy manga"
        initialized = true
    }

    private fun chapter(): SChapter = SChapter.create().apply {
        url = "/chapter-1"
        name = "Chapter 1"
    }
}

private class CapturingLegacySource : LegacyRxCatalogueSource() {
    var receivedChapters: List<SChapter> = emptyList()

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        receivedChapters = chapters
        return super.getMangaUpdate(manga, chapters, fetchDetails, fetchChapters)
    }
}
