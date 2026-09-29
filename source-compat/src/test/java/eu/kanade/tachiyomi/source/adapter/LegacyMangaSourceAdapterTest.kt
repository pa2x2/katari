package eu.kanade.tachiyomi.source.adapter

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.RelatedEntriesSource
import eu.kanade.tachiyomi.source.entry.ResumableEntryImageSource
import eu.kanade.tachiyomi.source.entry.SEntry
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

private class DisabledRelatedHttpSource : DirectRelatedHttpSource() {
    override val disableRelatedMangas = true
}

private class ResumableLegacyHttpSource(
    override val baseUrl: String,
) : InheritedRelatedHttpSource() {
    override val client = OkHttpClient()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
}
