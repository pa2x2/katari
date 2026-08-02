package mihon.entry.interactions.manga.page

import android.content.Context
import eu.kanade.tachiyomi.source.model.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.domain.chapter.model.Chapter
import java.io.File

class MangaPageStoreTest {
    @TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun `concurrent consumers share one image fetch and cache entry`() = runTest {
        val cache = pageCache()
        val firstFetchStarted = CompletableDeferred<Unit>()
        val allowFirstFetch = CompletableDeferred<Unit>()
        var fetchCount = 0

        try {
            val results = listOf(
                async {
                    cache.getOrPutImage(IMAGE_URL, force = false) {
                        fetchCount++
                        firstFetchStarted.complete(Unit)
                        allowFirstFetch.await()
                        imageResponse("shared image")
                    }
                },
                async {
                    firstFetchStarted.await()
                    cache.getOrPutImage(IMAGE_URL, force = false) {
                        fetchCount++
                        imageResponse("duplicate image")
                    }
                },
            )

            firstFetchStarted.await()
            allowFirstFetch.complete(Unit)

            results.awaitAll().map(File::getCanonicalPath).distinct() shouldContainExactly
                listOf(cache.getImageFile(IMAGE_URL).canonicalPath)
            cache.getImageFile(IMAGE_URL).readText() shouldBe "shared image"
            fetchCount shouldBe 1
        } finally {
            cache.close()
        }
    }

    @Test
    fun `concurrent sessions share one page list fetch`() = runTest {
        val cache = pageCache()
        val chapter = Chapter.create().copy(mangaId = 10L, url = "/chapter")
        val firstFetchStarted = CompletableDeferred<Unit>()
        val allowFirstFetch = CompletableDeferred<Unit>()
        var fetchCount = 0

        try {
            val results = listOf(
                async {
                    cache.getOrPutPageList(chapter) {
                        fetchCount++
                        firstFetchStarted.complete(Unit)
                        allowFirstFetch.await()
                        listOf(Page(index = 0, url = "/page/0"))
                    }
                },
                async {
                    firstFetchStarted.await()
                    cache.getOrPutPageList(chapter) {
                        fetchCount++
                        listOf(Page(index = 1, url = "/duplicate"))
                    }
                },
            )

            firstFetchStarted.await()
            allowFirstFetch.complete(Unit)

            results.awaitAll().map { pages -> pages.single().url } shouldContainExactly
                listOf("/page/0", "/page/0")
            fetchCount shouldBe 1
        } finally {
            cache.close()
        }
    }

    private fun pageCache(): MangaPageStore {
        val context = mockk<Context> {
            every { cacheDir } returns temporaryDirectory
        }
        return MangaPageStore(context, Json)
    }

    private fun imageResponse(body: String): Response {
        return Response.Builder()
            .request(Request.Builder().url(IMAGE_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody())
            .build()
    }
}

private const val IMAGE_URL = "https://example.invalid/page.jpg"
