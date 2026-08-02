package mihon.entry.interactions.manga.media

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.manga.media.session.MangaMediaSessionProcessor
import mihon.entry.interactions.manga.state.mangaProgressState
import mihon.entry.interactions.manga.state.pageIndex
import mihon.entry.interactions.media.EntryImmersiveHandle
import mihon.entry.interactions.media.EntryImmersiveProgress
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.media.session.EntryMediaSessionResult
import okhttp3.Request
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository

class MangaImmersiveProcessorTest {
    @Test
    fun `resolves and caches an image URL only when its page is requested`() = runTest {
        val page = EntryImagePage(index = 7, url = "/page")
        val source = mockk<EntryImageSource> {
            every { id } returns 1L
            every { name } returns "Source"
            coEvery { getMedia(any(), any()) } returns EntryMedia.ImagePages(listOf(page))
            coEvery { getImageUrl(page) } returns "https://example.invalid/page.jpg"
            every { imageRequest(page, "https://example.invalid/page.jpg") } returns
                Request.Builder()
                    .url("https://example.invalid/page.jpg")
                    .header("Referer", "https://example.invalid/")
                    .build()
        }
        val handle = MangaImmersiveProcessor(mediaSession = noOpMediaSession()).load(
            context = mockk<Context>(relaxed = true),
            entry = Entry.create().copy(id = 10L, type = EntryType.MANGA),
            chapter = EntryChapter.create().copy(id = 20L, entryId = 10L),
            source = source,
        ) as EntryImmersiveHandle.ImagePages
        val media = handle.delegate as MangaImmersiveMedia

        media.pages shouldHaveSize 1
        media.pages.single().index shouldBe 0
        media.initialPageIndex shouldBe 0
        coVerify(exactly = 0) { source.getImageUrl(any()) }
        verify(exactly = 0) { source.imageRequest(any(), any()) }

        val firstRequest = media.pages.single().resolveRequest()
        val secondRequest = media.pages.single().resolveRequest()

        firstRequest.imageUrl shouldBe "https://example.invalid/page.jpg"
        secondRequest shouldBe firstRequest
        coVerify(exactly = 1) { source.getImageUrl(page) }
        verify(exactly = 0) { source.imageRequest(any(), any()) }
    }

    @Test
    fun `load restores generic page position`() = runTest {
        val pages = (0..4).map { index -> EntryImagePage(index = index, url = "/page/$index") }
        val source = mockk<EntryImageSource> {
            every { id } returns 1L
            every { name } returns "Source"
            coEvery { getMedia(any(), any()) } returns EntryMedia.ImagePages(pages)
            coEvery { getImageUrl(any()) } answers { "https://example.invalid/${firstArg<EntryImagePage>().index}" }
            every { imageRequest(any(), any()) } answers {
                Request.Builder().url(secondArg<String>()).build()
            }
        }
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { get(10L, "", "/chapter") } returns mangaProgressState(
                entryId = 10L,
                chapterId = 20L,
                resourceKey = "/chapter",
                pageIndex = 3L,
                pageCount = 5L,
                completed = false,
                locatorUpdatedAt = 1L,
                completionUpdatedAt = 0L,
            )
        }

        val handle = MangaImmersiveProcessor(
            entryProgressRepository = progressRepository,
            mediaSession = noOpMediaSession(),
        ).load(
            context = mockk(relaxed = true),
            entry = Entry.create().copy(id = 10L, type = EntryType.MANGA),
            chapter = EntryChapter.create().copy(id = 20L, entryId = 10L, url = "/chapter"),
            source = source,
        ) as EntryImmersiveHandle.ImagePages

        (handle.delegate as MangaImmersiveMedia).initialPageIndex shouldBe 3
    }

    @Test
    fun `reports page progress and reading time`() = runTest {
        val chapter = EntryChapter.create().copy(id = 20L, entryId = 10L, url = "/chapter/20")
        val events = mutableListOf<EntryMediaSessionEvent>()
        val mediaSession = MangaMediaSessionProcessor(
            EntryMediaSessionEventSink {
                events += it
                EntryMediaSessionResult.Handled
            },
        )
        val processor = MangaImmersiveProcessor(mediaSession = mediaSession)
        val handle = imageHandle(chapter)

        processor.persistProgress(
            handle,
            EntryImmersiveProgress.ImagePage(pageIndex = 2, pageCount = 5, sessionDurationMs = 400L),
        )

        val event = events.single() as EntryMediaSessionEvent.Progressed
        event.progress.pageIndex shouldBe 2L
        event.progress.completed shouldBe false
        event.activity?.durationMillis shouldBe 400L
    }

    @Test
    fun `final page reports completed progress`() = runTest {
        val chapter = EntryChapter.create().copy(
            id = 20L,
            entryId = 10L,
            url = "/chapter/20",
            chapterNumber = 3.0,
        )
        val events = mutableListOf<EntryMediaSessionEvent>()
        val processor = MangaImmersiveProcessor(
            mediaSession = MangaMediaSessionProcessor(
                EntryMediaSessionEventSink {
                    events += it
                    EntryMediaSessionResult.Handled
                },
            ),
        )

        processor.persistProgress(
            imageHandle(chapter),
            EntryImmersiveProgress.ImagePage(pageIndex = 4, pageCount = 5, sessionDurationMs = 0L),
        )

        val event = events.single() as EntryMediaSessionEvent.Progressed
        event.progress.completed shouldBe true
    }

    private fun noOpMediaSession() = MangaMediaSessionProcessor(
        EntryMediaSessionEventSink {
            EntryMediaSessionResult.Handled
        },
    )

    private fun imageHandle(
        child: EntryChapter,
    ): EntryImmersiveHandle.ImagePages {
        val entry = Entry.create().copy(id = 10L, source = 1L, type = EntryType.MANGA)
        return EntryImmersiveHandle.ImagePages(
            entryType = EntryType.MANGA,
            chapterId = child.id,
            delegate = MangaImmersiveMedia(
                pages = emptyList(),
                initialPageIndex = 0,
                entry = entry,
                child = child,
            ),
        )
    }
}
