package mihon.entry.interactions.manga.media

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.toReaderChapter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
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
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository

class MangaImmersiveProcessorTest {
    @Test
    fun `load uses the shared reader page session`() = runTest {
        val entry = Entry.create().copy(id = 10L, type = EntryType.MANGA)
        val chapter = EntryChapter.create().copy(id = 20L, entryId = 10L)
        val readerChapter = readerChapter(entry, chapter, pageCount = 1)
        val context = mockk<Context>(relaxed = true)
        val source = mockk<UnifiedSource>(relaxed = true)
        var receivedRequest: Triple<Context, Entry, EntryChapter>? = null
        val processor = MangaImmersiveProcessor(
            mediaSession = noOpMediaSession(),
            loadPageSession = { receivedContext, receivedEntry, receivedChapter ->
                receivedRequest = Triple(receivedContext, receivedEntry, receivedChapter)
                readerChapter
            },
        )

        val handle = processor.load(context, entry, chapter, source) as EntryImmersiveHandle.ImagePages
        val media = handle.delegate as MangaImmersiveMedia

        receivedRequest shouldBe Triple(context, entry, chapter)
        media.readerChapter shouldBe readerChapter
        media.pages shouldHaveSize 1
        media.pages.single().index shouldBe 0
        media.initialPageIndex shouldBe 0
    }

    @Test
    fun `load restores generic page position`() = runTest {
        val entry = Entry.create().copy(id = 10L, type = EntryType.MANGA)
        val chapter = EntryChapter.create().copy(id = 20L, entryId = 10L, url = "/chapter")
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
            loadPageSession = { _, _, _ -> readerChapter(entry, chapter, pageCount = 5) },
        ).load(
            context = mockk(relaxed = true),
            entry = entry,
            chapter = chapter,
            source = mockk(relaxed = true),
        ) as EntryImmersiveHandle.ImagePages

        (handle.delegate as MangaImmersiveMedia).initialPageIndex shouldBe 3
    }

    @Test
    fun `release recycles the shared reader page session`() {
        val chapter = EntryChapter.create().copy(id = 20L, entryId = 10L)
        val entry = Entry.create().copy(id = 10L, source = 1L, type = EntryType.MANGA)
        val pageLoader = mockk<PageLoader>(relaxed = true)
        val readerChapter = readerChapter(entry, chapter, pageCount = 1, pageLoader = pageLoader)
        val processor = MangaImmersiveProcessor(mediaSession = noOpMediaSession())

        processor.release(imageHandle(entry, chapter, readerChapter))

        verify(exactly = 1) { pageLoader.recycle() }
    }

    @Test
    fun `load failure after session creation recycles the session`() = runTest {
        val chapter = EntryChapter.create().copy(id = 20L, entryId = 10L, url = "/chapter")
        val entry = Entry.create().copy(id = 10L, source = 1L, type = EntryType.MANGA)
        val pageLoader = mockk<PageLoader>(relaxed = true)
        val readerChapter = readerChapter(entry, chapter, pageCount = 1, pageLoader = pageLoader)
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { get(any(), any(), any()) } throws IllegalStateException("progress unavailable")
        }
        val processor = MangaImmersiveProcessor(
            entryProgressRepository = progressRepository,
            mediaSession = noOpMediaSession(),
            loadPageSession = { _, _, _ -> readerChapter },
        )

        shouldThrow<IllegalStateException> {
            processor.load(mockk(relaxed = true), entry, chapter, mockk(relaxed = true))
        }

        verify(exactly = 1) { pageLoader.recycle() }
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

    private fun imageHandle(child: EntryChapter): EntryImmersiveHandle.ImagePages {
        val entry = Entry.create().copy(id = 10L, source = 1L, type = EntryType.MANGA)
        return imageHandle(entry, child, readerChapter(entry, child, pageCount = 1))
    }

    private fun imageHandle(
        entry: Entry,
        child: EntryChapter,
        readerChapter: ReaderChapter,
    ): EntryImmersiveHandle.ImagePages {
        return EntryImmersiveHandle.ImagePages(
            entryType = EntryType.MANGA,
            chapterId = child.id,
            delegate = MangaImmersiveMedia(
                readerChapter = readerChapter,
                initialPageIndex = 0,
                entry = entry,
                child = child,
            ),
        )
    }

    private fun readerChapter(
        entry: Entry,
        child: EntryChapter,
        pageCount: Int,
        pageLoader: PageLoader = mockk(relaxed = true),
    ): ReaderChapter {
        return ReaderChapter(child.toReaderChapter(), entry).apply {
            ref()
            this.pageLoader = pageLoader
            state = ReaderChapter.State.Loaded(
                List(pageCount) { index -> ReaderPage(index).also { it.chapter = this } },
            )
        }
    }
}
