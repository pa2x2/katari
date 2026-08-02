package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mihon.entry.interactions.manga.page.MangaPageStore
import okhttp3.Response
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import java.io.File

class EntryPageLoaderTest {
    @Test
    fun `background preparation downloads the existing five image batch`() = runTest {
        val chapter = ReaderChapter(
            Chapter.create().copy(
                id = 2L,
                mangaId = 1L,
                name = "Next chapter",
                url = "/next",
            ),
        )
        val pages = List(6) { index ->
            ReaderPage(index, imageUrl = "https://example.invalid/$index.jpg").also {
                it.chapter = chapter
            }
        }
        chapter.state = ReaderChapter.State.Loaded(pages)
        val source = mockk<EntryImageSource> {
            coEvery { getImage(any(), any()) } returns mockk<Response>(relaxed = true)
        }
        val cache = mockk<MangaPageStore>(relaxed = true) {
            every { isImageInCache(any()) } returns false
            coEvery { getOrPutImage(any(), any(), any(), any()) } coAnswers {
                arg<suspend () -> Response>(3).invoke()
                mockk<File>(relaxed = true)
            }
        }
        val loader = EntryPageLoader(chapter, source, cache)
        chapter.pageLoader = loader

        loader.preloadPage(pages.first())
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                pages[4].statusFlow.first { it == Page.State.Ready }
            }
        }

        coVerify(exactly = 5) { source.getImage(any<EntryImagePage>(), any()) }
        coVerify(exactly = 0) {
            source.getImage(match<EntryImagePage> { it.index == 5 }, any())
        }

        loader.recycle()
    }

    @Test
    fun `explicit preload window prepares only the supplied pages`() = runTest {
        val chapter = ReaderChapter(
            Chapter.create().copy(id = 2L, mangaId = 1L, name = "Next chapter", url = "/next"),
        )
        val pages = List(12) { index ->
            ReaderPage(index, imageUrl = "https://example.invalid/$index.jpg").also {
                it.chapter = chapter
            }
        }
        chapter.state = ReaderChapter.State.Loaded(pages)
        val source = mockk<EntryImageSource> {
            coEvery { getImage(any(), any()) } returns mockk<Response>(relaxed = true)
        }
        val cache = mockk<MangaPageStore>(relaxed = true) {
            coEvery { getOrPutImage(any(), any(), any(), any()) } coAnswers {
                arg<suspend () -> Response>(3).invoke()
                mockk<File>(relaxed = true)
            }
        }
        val loader = EntryPageLoader(chapter, source, cache)
        chapter.pageLoader = loader

        loader.setPreloadPages(pages.subList(1, 11))
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                pages[10].statusFlow.first { it == Page.State.Ready }
            }
        }

        coVerify(exactly = 10) { source.getImage(any<EntryImagePage>(), any()) }
        coVerify(exactly = 0) {
            source.getImage(match<EntryImagePage> { it.index == 0 || it.index == 11 }, any())
        }

        loader.recycle()
    }
}
