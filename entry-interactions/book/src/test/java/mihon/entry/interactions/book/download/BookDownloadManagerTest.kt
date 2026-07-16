package mihon.entry.interactions.book.download

import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.book.download.model.BookDownload
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertSame

class BookDownloadManagerTest {
    @Test
    fun `queued books preserve resolved next-item reading order`() {
        val entry = Entry.create().copy(id = 1L)
        val chapters = listOf(
            chapter(id = 11L, sourceOrder = 1L),
            chapter(id = 12L, sourceOrder = 2L),
            chapter(id = 13L, sourceOrder = 3L),
        )

        val queued = chapters.toQueuedBookDownloads(entry)

        assertEquals(listOf(11L, 12L, 13L), queued.map { it.chapter.id })
    }

    @Test
    fun `queue changes made during restoration win without duplicating children`() {
        val restoredFirst = download(1L)
        val restoredReplaced = download(2L)
        val currentReplacement = download(2L)
        val currentThird = download(3L)

        val merged = mergeRestoredBookDownloads(
            restored = listOf(restoredFirst, restoredReplaced),
            current = listOf(currentReplacement, currentThird),
        )

        assertEquals(listOf(1L, 2L, 3L), merged.map { it.chapter.id })
        assertSame(currentReplacement, merged[1])
    }

    private fun download(chapterId: Long): BookDownload = mockk {
        every { chapter.id } returns chapterId
    }

    private fun chapter(id: Long, sourceOrder: Long): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = 1L,
        sourceOrder = sourceOrder,
        url = "/chapter/$id",
        name = "Chapter $id",
    )
}
