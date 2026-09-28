package mihon.entry.interactions.book.state

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BookConsumptionProcessorTest {
    @Test
    fun `mark unread resets a partial book locator`() = runTest {
        val chapter = EntryChapter.create().copy(id = 10L, entryId = 1L, url = "/chapter/1", name = "Chapter 1")
        val current = EntryProgressState(
            entryId = 1L,
            chapterId = chapter.id,
            contentKey = "volume-1",
            resourceKey = "chapter-1",
            locator = EntryProgressLocator(
                kind = BOOK_PROGRESS_LOCATOR_KIND,
                progression = 0.4,
                totalProgression = 0.2,
            ),
            completed = false,
            locatorUpdatedAt = 50L,
            completionUpdatedAt = 60L,
        )
        val captured = slot<EntryProgressState>()
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { getByEntryId(chapter.entryId) } returns listOf(current)
            coEvery { mergeAndSyncChild(capture(captured)) } answers { captured.captured }
        }
        val updatedChapters = slot<List<EntryChapter>>()
        val chapterRepository = mockk<EntryChapterRepository> {
            coEvery { updateAll(capture(updatedChapters)) } returns true
        }
        val processor = BookConsumptionProcessor(
            entryProgressRepository = progressRepository,
            entryChapterRepository = chapterRepository,
            now = { 100L },
        )

        processor.setConsumed(
            Entry.create().copy(id = 1L, source = 9L, url = "/book", title = "Book", type = EntryType.BOOK),
            listOf(chapter),
            consumed = false,
        )

        assertFalse(captured.captured.completed)
        assertEquals(EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND), captured.captured.locator)
        assertEquals(100L, captured.captured.locatorUpdatedAt)
        assertEquals(100L, captured.captured.completionUpdatedAt)
        assertFalse(updatedChapters.captured.single().read)
    }
}
