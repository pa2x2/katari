package mihon.entry.interactions.manga.media

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.toReaderChapter
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.manga.media.session.MangaMediaSessionProcessor
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.media.session.EntryMediaSessionResult
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository

class MangaImmersiveProcessorTest {

    @Test
    fun `load failure after session creation recycles the session`() = runTest {
        val chapter = EntryChapter.create().copy(id = 20L, entryId = 10L, url = "/chapter")
        val entry = Entry.create().copy(id = 10L, source = 1L, type = EntryType.MANGA)
        val pageLoader = mockk<PageLoader>(relaxed = true)
        val readerChapter = ReaderChapter(chapter.toReaderChapter(), entry).apply {
            ref()
            this.pageLoader = pageLoader
            state = ReaderChapter.State.Loaded(listOf(ReaderPage(0).also { it.chapter = this }))
        }
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { get(any(), any(), any()) } throws IllegalStateException("progress unavailable")
        }
        val processor = MangaImmersiveProcessor(
            entryProgressRepository = progressRepository,
            mediaSession = MangaMediaSessionProcessor(EntryMediaSessionEventSink { EntryMediaSessionResult.Handled }),
            loadPageSession = { _, _, _ -> readerChapter },
        )

        shouldThrow<IllegalStateException> {
            processor.load(mockk(relaxed = true), entry, chapter, mockk(relaxed = true))
        }

        verify(exactly = 1) { pageLoader.recycle() }
    }
}
