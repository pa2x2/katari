package mihon.entry.interactions.book.download

import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.book.download.model.BookDownload
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class BookDownloadManagerTest {
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
}
