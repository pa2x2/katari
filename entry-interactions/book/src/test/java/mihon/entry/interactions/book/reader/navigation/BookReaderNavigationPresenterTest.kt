package mihon.entry.interactions.book.reader.navigation

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.child.EntryChildProgressLabel
import mihon.entry.interactions.child.EntryChildProgressRequest
import mihon.entry.interactions.child.EntryChildProgressResult
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR
import kotlin.test.assertEquals

class BookReaderNavigationPresenterTest {
    @Test
    fun `refreshes status in stable reader order and reuses child progress presentation`() = runTest {
        val entry = Entry.create().copy(id = 1L, type = EntryType.BOOK)
        val first = chapter(id = 1L)
        val second = chapter(id = 2L)
        val refreshed = listOf(
            first.copy(read = true),
            second.copy(bookmark = true),
        )
        val progressLabel = EntryChildProgressLabel(MR.strings.label_started)
        val progressRequest = slot<EntryChildProgressRequest>()
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            every { subscribe(entry) } returns flowOf(entry to refreshed)
        }
        val childListFeature = mockk<EntryChildListFeature> {
            every { progressLabels(capture(progressRequest)) } returns EntryChildProgressResult.Available(
                flowOf(mapOf(second.id to progressLabel)),
            )
        }

        val result = BookReaderNavigationPresenter(getEntryWithChapters, childListFeature)
            .observe(entry, readingOrder = listOf(second, first))
            .first()

        assertEquals(listOf(second.id, first.id), result.chapters.map(EntryChapter::id))
        assertEquals(true, result.chapters[0].bookmark)
        assertEquals(true, result.chapters[1].read)
        assertEquals(mapOf(second.id to progressLabel), result.progressLabels)
        assertEquals(result.chapters, progressRequest.captured.chapters)
        verify(exactly = 1) { childListFeature.progressLabels(any()) }
    }

    private fun chapter(id: Long): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = 1L,
        url = "/chapter/$id",
        name = "Chapter $id",
        chapterNumber = id.toDouble(),
        sourceOrder = id,
    )
}
