package mihon.entry.interactions.book.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository

class BookDownloadCleanupTest {
    @Test
    fun `manual cleanup follows its preference and category exclusions`() = runTest {
        val entry = entry()
        val chapter = chapter(1L, 1.0)
        val fixture = fixture(listOf(chapter))

        fixture.cleanup.afterMarkedConsumed(entry, listOf(chapter))
        coVerify(exactly = 0) { fixture.manager.delete(any(), any()) }

        fixture.preferences.removeAfterMarkedAsRead.set(true)
        fixture.cleanup.afterMarkedConsumed(entry, listOf(chapter))
        coVerify(exactly = 1) { fixture.manager.delete(entry, listOf(chapter)) }

        fixture.preferences.removeExcludeCategories.set(setOf("7"))
        fixture.cleanup.afterMarkedConsumed(entry, listOf(chapter))
        coVerify(exactly = 1) { fixture.manager.delete(any(), any()) }
    }

    @Test
    fun `automatic cleanup deletes the configured item in reading order`() = runTest {
        val first = chapter(1L, 1.0)
        val second = chapter(2L, 2.0)
        val current = chapter(3L, 3.0)
        val fixture = fixture(listOf(current, first, second))

        fixture.preferences.removeAfterReadSlots.set(0)
        fixture.cleanup.afterReaderCompleted(entry(), current)
        coVerify(exactly = 1) { fixture.manager.delete(entry(), listOf(current)) }

        fixture.preferences.removeAfterReadSlots.set(1)
        fixture.cleanup.afterReaderCompleted(entry(), current)
        coVerify(exactly = 1) { fixture.manager.delete(entry(), listOf(second)) }
    }

    @Test
    fun `automatic cleanup is disabled by its setting and category exclusions`() = runTest {
        val current = chapter(1L, 1.0)
        val fixture = fixture(listOf(current))

        fixture.cleanup.afterReaderCompleted(entry(), current)
        fixture.preferences.removeAfterReadSlots.set(0)
        fixture.preferences.removeExcludeCategories.set(setOf("7"))
        fixture.cleanup.afterReaderCompleted(entry(), current)

        coVerify(exactly = 0) { fixture.manager.delete(any(), any()) }
    }

    private fun fixture(chapters: List<EntryChapter>): Fixture {
        val entry = entry()
        val preferences = DownloadPreferences(InMemoryPreferenceStore())
        val manager = mockk<BookDownloadManager>(relaxed = true)
        val cleanup = BookDownloadCleanup(
            downloadPreferences = preferences,
            getCategories = mockk<GetCategories> {
                coEvery { await(entry.id) } returns listOf(Category(7L, "Library", 0L, 0L))
            },
            getEntryWithChapters = mockk<GetEntryWithChapters> {
                coEvery { awaitChapters(entry.id, any()) } returns chapters
            },
            entryRepository = mockk<EntryRepository> {
                coEvery { getEntryById(entry.id) } returns entry
            },
            downloadManager = manager,
        )
        return Fixture(preferences, manager, cleanup)
    }

    private fun entry(): Entry = Entry.create().copy(
        id = 1L,
        source = 9L,
        url = "/book",
        title = "Book",
        type = EntryType.BOOK,
    )

    private fun chapter(id: Long, number: Double): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = 1L,
        url = "/chapter/$id",
        name = "Chapter $id",
        chapterNumber = number,
        sourceOrder = id,
    )

    private data class Fixture(
        val preferences: DownloadPreferences,
        val manager: BookDownloadManager,
        val cleanup: BookDownloadCleanup,
    )
}
