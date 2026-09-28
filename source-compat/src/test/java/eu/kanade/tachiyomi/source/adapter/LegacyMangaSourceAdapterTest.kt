package eu.kanade.tachiyomi.source.adapter

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.RelatedEntriesSource
import eu.kanade.tachiyomi.source.entry.ResumableEntryImageSource
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.supportedEntryTypes
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import rx.Observable
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import eu.kanade.tachiyomi.source.UnmeteredSource as LegacyUnmeteredSource
import eu.kanade.tachiyomi.source.entry.UnmeteredSource as EntryUnmeteredSource

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
    fun `only a directly implemented and enabled related manga method exposes related entries`() = runTest {
        (InheritedRelatedHttpSource().asUnifiedSource() is RelatedEntriesSource) shouldBe false
        (DisabledRelatedHttpSource().asUnifiedSource() is RelatedEntriesSource) shouldBe false

        val adapted = DirectRelatedHttpSource().asUnifiedSource() as RelatedEntriesSource
        val entry = SEntry.create().apply {
            url = "/manga"
            title = "Legacy manga"
            type = EntryType.MANGA
        }

        val related = adapted.getRelatedEntries(entry).single()

        related.url shouldBe "/related"
        related.type shouldBe EntryType.MANGA
    }

    @Test
    fun `legacy unmetered marker survives adaptation without losing other contracts`() {
        val adapted = LegacyUnmeteredHttpSource().asUnifiedSource()

        (adapted is EntryUnmeteredSource) shouldBe true
        (adapted is EntryImageSource) shouldBe true
        (InheritedRelatedHttpSource().asUnifiedSource() is EntryUnmeteredSource) shouldBe false
    }

    @Test
    fun `legacy image adapter forwards partial file size`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .code(206)
                    .body("remaining")
                    .build(),
            )
            server.start()
            val adapted = ResumableLegacyHttpSource(server.url("/").toString()).asUnifiedSource()
                as ResumableEntryImageSource

            adapted.getImage(
                page = EntryImagePage(
                    index = 0,
                    imageUrl = server.url("/page.jpg").toString(),
                ),
                progress = null,
                existingSize = 23L,
            ).close()

            server.takeRequest().headers["Range"] shouldBe "bytes=23-"
        }
    }
}

private suspend fun invokeLegacyBridge(instance: Any, methodName: String): Any? =
    suspendCoroutineUninterceptedOrReturn { continuation ->
        instance.javaClass
            .getMethod(methodName, kotlin.coroutines.Continuation::class.java)
            .invoke(instance, continuation)
    }

private class LegacyRxCatalogueSource : CatalogueSource {
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

private open class InheritedRelatedHttpSource : HttpSource() {
    override val name = "Related source"
    override val lang = "en"
    override val baseUrl = "https://example.invalid"
    override val supportsLatest = false
}

private open class DirectRelatedHttpSource : InheritedRelatedHttpSource() {
    override suspend fun fetchRelatedMangaList(manga: SManga): List<SManga> {
        return listOf(
            SManga.create().apply {
                url = "/related"
                title = "Related manga"
            },
        )
    }
}

private class LegacyUnmeteredHttpSource : InheritedRelatedHttpSource(), LegacyUnmeteredSource

private class DisabledRelatedHttpSource : DirectRelatedHttpSource() {
    override val disableRelatedMangas = true
}

private class ResumableLegacyHttpSource(
    override val baseUrl: String,
) : InheritedRelatedHttpSource() {
    override val client = OkHttpClient()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
}
