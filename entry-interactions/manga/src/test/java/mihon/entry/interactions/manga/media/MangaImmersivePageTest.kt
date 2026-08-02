package mihon.entry.interactions.manga.media

import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.File

class MangaImmersivePageTest {
    @Test
    fun `progress is determinate only when content length is known`() {
        MangaImmersivePageProgress(bytesRead = 50L, contentLength = 100L).fraction
            ?.shouldBeExactly(0.5f)
        MangaImmersivePageProgress(bytesRead = 50L, contentLength = -1L).fraction.shouldBeNull()
        MangaImmersivePageProgress(bytesRead = 150L, contentLength = 100L).fraction
            ?.shouldBeExactly(1f)
    }

    @Test
    fun `cancelled load is not retained and can be retried`() = runTest {
        val firstLoadStarted = CompletableDeferred<Unit>()
        val firstLoadCancelled = CompletableDeferred<Unit>()
        var attempts = 0
        val loadedFile = File.createTempFile("immersive-page", ".image").apply { deleteOnExit() }
        val loadImage: suspend (
            EntryImagePage,
            MangaImmersivePageRequest,
            (MangaImmersivePageProgress) -> Unit,
        ) -> File = { _, _, _ ->
            attempts += 1
            if (attempts == 1) {
                try {
                    firstLoadStarted.complete(Unit)
                    awaitCancellation()
                } finally {
                    firstLoadCancelled.complete(Unit)
                }
            }
            loadedFile
        }
        val sourcePage = EntryImagePage(
            index = 0,
            url = "/page/0",
            imageUrl = "https://example.invalid/page.jpg",
        )
        val page = MangaImmersivePage.unresolved(
            index = 0,
            sourcePage = sourcePage,
            requestResolver = MangaImmersivePageRequestResolver(mockk<EntryImageSource>()),
            loadImage = loadImage,
        )

        val firstLoad = launch { page.loadImage() }
        firstLoadStarted.await()
        firstLoad.cancelAndJoin()
        firstLoadCancelled.await()

        page.loadImage() shouldBe loadedFile
        attempts shouldBe 2
    }
}
