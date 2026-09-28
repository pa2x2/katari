package mihon.entry.interactions.book.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class BookDownloadProcessorTest {
    @Test
    fun `merged downloads queue each chapter under its owning entry and start them together`() = runTest {
        val visible = entry(id = 1L, source = 10L)
        val member = entry(id = 2L, source = 20L)
        val visibleChapter = chapter(id = 11L, entryId = visible.id)
        val memberChapter = chapter(id = 21L, entryId = member.id)
        val manager = mockk<BookDownloadManager>(relaxed = true) {
            every { cacheChanges } returns flowOf(Unit)
            every { queueState } returns MutableStateFlow(emptyList())
            every { isRunning } returns MutableStateFlow(false)
        }
        val processor = BookDownloadProcessor(
            BookDownloadProcessorDependencies(
                manager = manager,
                cache = mockk(relaxed = true) {
                    every { isInitializing } returns MutableStateFlow(false)
                },
                sourceManager = mockk(),
                entryRepository = mockk {
                    coEvery { getEntryById(member.id) } returns member
                },
                getEntryWithChapters = mockk(),
            ),
        )

        processor.download(visible, listOf(visibleChapter, memberChapter), startNow = true)

        coVerify(exactly = 1) { manager.queueBooks(visible, listOf(visibleChapter), autoStart = false) }
        coVerify(exactly = 1) { manager.queueBooks(member, listOf(memberChapter), autoStart = false) }
        verify(exactly = 1) { manager.startDownloadsNow(listOf(11L, 21L)) }
    }

    private fun entry(id: Long, source: Long): Entry = Entry.create().copy(
        id = id,
        profileId = 7L,
        source = source,
        url = "/book/$id",
        title = "Book $id",
        type = EntryType.BOOK,
    )

    private fun chapter(id: Long, entryId: Long): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = entryId,
        url = "/chapter/$id",
        name = "Chapter $id",
    )
}
