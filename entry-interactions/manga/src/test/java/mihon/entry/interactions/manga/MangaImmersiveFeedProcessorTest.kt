package mihon.entry.interactions.manga

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
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryImmersiveFeedHandle
import mihon.entry.interactions.EntryImmersiveFeedProgress
import mihon.entry.interactions.EntryReaderIncognitoState
import mihon.entry.interactions.EntryReaderTracking
import okhttp3.Request
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.repository.HistoryRepository

class MangaImmersiveFeedProcessorTest {
    @Test
    fun `loads direct image requests without reader preview cache`() = runTest {
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
        val handle = MangaImmersiveFeedProcessor().load(
            context = mockk<Context>(relaxed = true),
            entry = Entry.create().copy(id = 10L, type = EntryType.MANGA),
            chapter = EntryChapter.create().copy(id = 20L, entryId = 10L),
            source = source,
        ) as EntryImmersiveFeedHandle.ImagePages
        val media = handle.delegate as MangaImmersiveFeedMedia

        media.pages shouldHaveSize 1
        media.pages.single().index shouldBe 0
        media.pages.single().imageUrl shouldBe "https://example.invalid/page.jpg"
        media.pages.single().headers["Referer"] shouldBe "https://example.invalid/"
        media.initialPageIndex shouldBe 0
    }

    @Test
    fun `persists page progress and reading time`() = runTest {
        val chapter = EntryChapter.create().copy(id = 20L, entryId = 10L)
        val repository = mockk<EntryChapterRepository> {
            coEvery { getChapterById(20L) } returns chapter
            coEvery { update(any()) } returns true
        }
        val history = mockk<HistoryRepository> {
            coEvery { upsertHistory(any()) } returns Unit
        }
        val processor = MangaImmersiveFeedProcessor(
            entryChapterRepository = repository,
            historyRepository = history,
            readerIncognitoState = mockk {
                every { isIncognito(1L) } returns false
            },
        )
        val handle = imageHandle(chapterId = 20L)

        processor.persistProgress(
            handle,
            EntryImmersiveFeedProgress.ImagePage(pageIndex = 2, pageCount = 5, sessionDurationMs = 400L),
        )

        val updated = slot<EntryChapter>()
        coVerify { repository.update(capture(updated)) }
        updated.captured.lastPageRead shouldBe 2L
        updated.captured.read shouldBe false
        val historyUpdate = slot<HistoryUpdate>()
        coVerify { history.upsertHistory(capture(historyUpdate)) }
        historyUpdate.captured.sessionReadDuration shouldBe 400L
    }

    @Test
    fun `final page marks chapter read and syncs tracking once`() = runTest {
        val chapter = EntryChapter.create().copy(id = 20L, entryId = 10L, chapterNumber = 3.0)
        val repository = mockk<EntryChapterRepository> {
            coEvery { getChapterById(20L) } returns chapter
            coEvery { update(any()) } returns true
        }
        val tracking = mockk<EntryReaderTracking> {
            coEvery { updateChapterRead(any(), any(), any()) } returns Unit
        }
        val processor = MangaImmersiveFeedProcessor(
            entryChapterRepository = repository,
            readerIncognitoState = mockk {
                every { isIncognito(1L) } returns false
            },
            readerTracking = tracking,
        )

        processor.persistProgress(
            imageHandle(chapterId = 20L, chapterNumber = 3.0),
            EntryImmersiveFeedProgress.ImagePage(pageIndex = 4, pageCount = 5, sessionDurationMs = 0L),
        )

        val updated = slot<EntryChapter>()
        coVerify { repository.update(capture(updated)) }
        updated.captured.read shouldBe true
        coVerify(exactly = 1) { tracking.updateChapterRead(any(), 10L, 3.0) }
    }

    @Test
    fun `incognito suppresses immersive reading progress`() = runTest {
        val repository = mockk<EntryChapterRepository>(relaxed = true)
        val processor = MangaImmersiveFeedProcessor(
            entryChapterRepository = repository,
            readerIncognitoState = mockk<EntryReaderIncognitoState> {
                every { isIncognito(1L) } returns true
            },
        )

        processor.persistProgress(
            imageHandle(chapterId = 20L),
            EntryImmersiveFeedProgress.ImagePage(pageIndex = 1, pageCount = 5, sessionDurationMs = 100L),
        )

        coVerify(exactly = 0) { repository.getChapterById(any()) }
        coVerify(exactly = 0) { repository.update(any()) }
    }

    private fun imageHandle(
        chapterId: Long,
        chapterNumber: Double = 1.0,
    ): EntryImmersiveFeedHandle.ImagePages {
        return EntryImmersiveFeedHandle.ImagePages(
            entryType = EntryType.MANGA,
            chapterId = chapterId,
            delegate = MangaImmersiveFeedMedia(
                pages = listOf(
                    MangaImmersiveFeedPage(
                        0,
                        "https://example.invalid/page.jpg",
                        okhttp3.Headers.Builder().build(),
                    ),
                ),
                initialPageIndex = 0,
                entryId = 10L,
                sourceId = 1L,
                chapterNumber = chapterNumber,
                context = mockk(relaxed = true),
            ),
        )
    }
}
