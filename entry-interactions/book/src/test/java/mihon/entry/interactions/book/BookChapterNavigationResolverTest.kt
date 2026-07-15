package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookChapterNavigationResolverTest {
    @Test
    fun `resolves adjacent stored chapters without media catalog siblings`() = runTest {
        val entry = Entry.create().copy(id = 1L, type = EntryType.BOOK)
        val chapters = listOf(chapter(1L, 1.0), chapter(2L, 2.0), chapter(3L, 3.0))
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitChapters(entry.id) } returns chapters
        }

        val result = BookChapterNavigationResolver(getEntryWithChapters).resolve(entry, chapters[1])

        assertEquals(1L, result?.previous?.id)
        assertEquals(2L, result?.current?.id)
        assertEquals(3L, result?.next?.id)
    }

    @Test
    fun `returns no adjacent chapters when selected child is absent`() = runTest {
        val entry = Entry.create().copy(id = 1L, type = EntryType.BOOK)
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitChapters(entry.id) } returns listOf(chapter(1L, 1.0))
        }

        val result = BookChapterNavigationResolver(getEntryWithChapters).resolve(entry, chapter(9L, 9.0))

        assertNull(result)
    }

    private fun chapter(id: Long, number: Double): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = 1L,
        url = "/chapter/$id",
        name = "Chapter $id",
        chapterNumber = number,
        sourceOrder = id,
    )
}
