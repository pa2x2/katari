package mihon.entry.interactions.book.document.reader.transition

import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.viewer.EntryChildTransition
import org.junit.Test
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BookDocumentWhenNeededBoundaryTest {
    private fun chapter(id: Long, number: Double = -1.0): EntryChapter =
        EntryChapter.create().copy(id = id, name = "Chapter $id", chapterNumber = number)

    @Test
    fun `when-needed joins a prepared contiguous next boundary`() {
        val current = chapter(1, 1.0)
        val next = chapter(2, 2.0)
        val boundary = EntryChildTransition.Next(current, next)

        assertTrue(
            isSeamlessWhenNeededBoundary(
                ChapterTransitionMode.WHEN_NEEDED,
                boundary,
                true,
                null,
            ),
        )
    }

    @Test
    fun `when-needed joins a prepared contiguous previous boundary`() {
        val current = chapter(2, 2.0)
        val previous = chapter(1, 1.0)
        val boundary = EntryChildTransition.Prev(current, previous)

        assertTrue(
            isSeamlessWhenNeededBoundary(
                ChapterTransitionMode.WHEN_NEEDED,
                boundary,
                true,
                null,
            ),
        )
    }

    @Test
    fun `when-needed keeps an unprepared boundary to request its destination`() {
        val current = chapter(1, 1.0)
        val next = chapter(2, 2.0)
        val boundary = EntryChildTransition.Next(current, next)

        assertFalse(
            isSeamlessWhenNeededBoundary(
                ChapterTransitionMode.WHEN_NEEDED,
                boundary,
                false,
                null,
            ),
        )
        assertFalse(
            isSeamlessWhenNeededBoundary(
                ChapterTransitionMode.WHEN_NEEDED,
                boundary,
                false,
                BookDocumentChapterLoadState.Loading,
            ),
        )
    }

    @Test
    fun `when-needed keeps a failed destination to offer a retry`() {
        val current = chapter(1, 1.0)
        val next = chapter(2, 2.0)
        val boundary = EntryChildTransition.Next(current, next)

        assertFalse(
            isSeamlessWhenNeededBoundary(
                ChapterTransitionMode.WHEN_NEEDED,
                boundary,
                true,
                BookDocumentChapterLoadState.Failed("unavailable"),
            ),
        )
    }

    @Test
    fun `when-needed keeps terminal boundaries`() {
        val current = chapter(1, 1.0)
        val boundary = EntryChildTransition.Next(current, null)

        assertFalse(
            isSeamlessWhenNeededBoundary(
                ChapterTransitionMode.WHEN_NEEDED,
                boundary,
                false,
                null,
            ),
        )
    }

    @Test
    fun `when-needed keeps chapter gaps even when prepared`() {
        val current = chapter(1, 1.0)
        val next = chapter(3, 3.0)
        val boundary = EntryChildTransition.Next(current, next)

        assertFalse(
            isSeamlessWhenNeededBoundary(
                ChapterTransitionMode.WHEN_NEEDED,
                boundary,
                true,
                null,
            ),
        )
    }

    @Test
    fun `when-needed keeps previous chapter gaps even when prepared`() {
        val current = chapter(3, 3.0)
        val previous = chapter(1, 1.0)
        val boundary = EntryChildTransition.Prev(current, previous)

        assertFalse(
            isSeamlessWhenNeededBoundary(
                ChapterTransitionMode.WHEN_NEEDED,
                boundary,
                true,
                null,
            ),
        )
    }

    @Test
    fun `other display modes never join boundaries seamlessly`() {
        val current = chapter(1, 1.0)
        val next = chapter(2, 2.0)
        val boundary = EntryChildTransition.Next(current, next)

        listOf(ChapterTransitionMode.ALWAYS, ChapterTransitionMode.HIDDEN).forEach { displayMode ->
            assertFalse(
                isSeamlessWhenNeededBoundary(displayMode, boundary, true, null),
            )
        }
    }
}
