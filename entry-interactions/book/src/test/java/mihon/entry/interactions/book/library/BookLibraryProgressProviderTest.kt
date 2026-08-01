package mihon.entry.interactions.book.library

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.book.state.BOOK_PROGRESS_LOCATOR_KIND
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository
import kotlin.test.assertEquals

internal class BookLibraryProgressProviderTest {
    @Test
    fun `continue progress uses current chapter progression instead of whole book progression`() = runTest {
        val chapter = chapter()
        val progress = progress(
            chapter = chapter,
            locator = EntryProgressLocator(
                kind = BOOK_PROGRESS_LOCATOR_KIND,
                progression = 0.75,
                totalProgression = 0.01,
            ),
        )
        val provider = provider(progress)

        val evidence = provider.evidence(entry(), listOf(chapter))

        assertEquals(0.75f, evidence.inProgressFraction)
    }

    @Test
    fun `continue progress retains total progression fallback for legacy book locators`() = runTest {
        val chapter = chapter()
        val progress = progress(
            chapter = chapter,
            locator = EntryProgressLocator(
                kind = BOOK_PROGRESS_LOCATOR_KIND,
                totalProgression = 0.4,
            ),
        )
        val provider = provider(progress)

        val evidence = provider.evidence(entry(), listOf(chapter))

        assertEquals(0.4f, evidence.inProgressFraction)
    }

    private fun provider(progress: EntryProgressState): BookLibraryProgressProvider {
        val repository = mockk<EntryProgressRepository> {
            coEvery { getByEntryId(progress.entryId) } returns listOf(progress)
        }
        return BookLibraryProgressProvider(repository)
    }

    private fun entry(): Entry = Entry.create().copy(
        id = 1L,
        source = 2L,
        url = "/book",
        title = "Book",
        type = EntryType.BOOK,
    )

    private fun chapter(): EntryChapter = EntryChapter.create().copy(
        id = 10L,
        entryId = 1L,
        url = "/chapter",
        name = "Chapter",
    )

    private fun progress(
        chapter: EntryChapter,
        locator: EntryProgressLocator,
    ): EntryProgressState = EntryProgressState(
        entryId = chapter.entryId,
        chapterId = chapter.id,
        resourceKey = "chapter",
        locator = locator,
        locatorUpdatedAt = 100L,
    )
}
