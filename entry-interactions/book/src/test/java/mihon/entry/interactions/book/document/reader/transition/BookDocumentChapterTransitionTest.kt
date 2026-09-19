package mihon.entry.interactions.book.document.reader.transition

import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.viewer.EntryChildTransition
import org.junit.Test
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BookDocumentChapterTransitionTest {
    private val current = EntryChapter.create().copy(id = 1)
    private val next = EntryChapter.create().copy(id = 2)

    private fun numberedChapter(id: Long, number: Double): EntryChapter =
        EntryChapter.create().copy(id = id, chapterNumber = number)

    @Test
    fun `hidden mode renders compact populated boundaries unless the destination failed`() {
        val boundary = EntryChildTransition.Next(current, next)
        assertTrue(rendersCompactHiddenBoundary(ChapterTransitionMode.HIDDEN, boundary, null, 0))
        assertTrue(
            rendersCompactHiddenBoundary(
                ChapterTransitionMode.HIDDEN,
                boundary,
                BookDocumentChapterLoadState.Loading,
                0,
            ),
        )
    }

    @Test
    fun `hidden mode keeps the card for terminal boundaries`() {
        val boundary = EntryChildTransition.Next(current, null)
        assertFalse(rendersCompactHiddenBoundary(ChapterTransitionMode.HIDDEN, boundary, null, 0))
    }

    @Test
    fun `hidden mode keeps the card for failed destination loads`() {
        val boundary = EntryChildTransition.Next(current, next)
        assertFalse(
            rendersCompactHiddenBoundary(
                ChapterTransitionMode.HIDDEN,
                boundary,
                BookDocumentChapterLoadState.Failed("unavailable"),
                0,
            ),
        )
    }

    @Test
    fun `hidden mode keeps the card across a chapter gap even while the destination prepares`() {
        val boundary = EntryChildTransition.Next(numberedChapter(1, 1.0), numberedChapter(3, 3.0))
        assertFalse(rendersCompactHiddenBoundary(ChapterTransitionMode.HIDDEN, boundary, null, 1))
        assertFalse(
            rendersCompactHiddenBoundary(
                ChapterTransitionMode.HIDDEN,
                boundary,
                BookDocumentChapterLoadState.Loading,
                1,
            ),
        )
    }

    @Test
    fun `hidden mode renders compact boundaries across duplicate chapter numbers`() {
        val boundary = EntryChildTransition.Next(numberedChapter(1, 1.0), numberedChapter(2, 1.0))
        assertTrue(rendersCompactHiddenBoundary(ChapterTransitionMode.HIDDEN, boundary, null, -1))
    }

    @Test
    fun `non-hidden modes never render compact boundaries`() {
        val boundary = EntryChildTransition.Next(current, next)
        listOf(ChapterTransitionMode.ALWAYS, ChapterTransitionMode.WHEN_NEEDED).forEach { displayMode ->
            assertFalse(rendersCompactHiddenBoundary(displayMode, boundary, null, 0))
            assertFalse(
                rendersCompactHiddenBoundary(displayMode, boundary, BookDocumentChapterLoadState.Loading, 0),
            )
        }
    }
}
