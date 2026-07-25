package mihon.entry.interactions.book.prose

import android.text.SpannableString
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class HtmlProseViewerItemsTest {
    @Test
    fun `paginated window places transitions between chapter pages`() {
        val previous = chapter(1L)
        val current = chapter(2L)
        val next = chapter(3L)
        val window = EntryChildWindow(current, previous, next)
        val items = buildPaginatedItems(
            window,
            mapOf(
                previous.id to listOf(page(previous)),
                current.id to listOf(page(current)),
                next.id to listOf(page(next)),
            ),
        )

        assertEquals(5, items.size)
        assertEquals(previous.id, assertIs<ProsePagerItem.Page>(items[0]).page.chapter.id)
        assertEquals(
            EntryChildDirection.PREVIOUS,
            assertIs<ProsePagerItem.Transition>(items[1]).transition.direction,
        )
        assertEquals(current.id, assertIs<ProsePagerItem.Page>(items[2]).page.chapter.id)
        assertEquals(
            EntryChildDirection.NEXT,
            assertIs<ProsePagerItem.Transition>(items[3]).transition.direction,
        )
        assertEquals(next.id, assertIs<ProsePagerItem.Page>(items[4]).page.chapter.id)
    }

    @Test
    fun `paginated mode starts from the live reader progression`() {
        val previous = chapter(1L)
        val current = chapter(2L)
        val items = listOf(
            ProsePagerItem.Page(page(previous)),
            ProsePagerItem.Page(page(current, index = 0, total = 5)),
            ProsePagerItem.Page(page(current, index = 1, total = 5)),
            ProsePagerItem.Page(page(current, index = 2, total = 5)),
            ProsePagerItem.Page(page(current, index = 3, total = 5)),
            ProsePagerItem.Page(page(current, index = 4, total = 5)),
        )

        assertEquals(4, initialPaginatedItemIndex(items, current.id, progression = 0.6f))
    }

    private fun chapter(id: Long) = EntryChapter.create().copy(id = id, entryId = 9L, name = "Chapter $id")

    private fun page(chapter: EntryChapter, index: Int = 0, total: Int = 1) =
        HtmlProsePage(
            chapter = chapter,
            index = index,
            total = total,
            text = SpannableString("Text"),
            progression = if (total <= 1) 1f else index.toFloat() / (total - 1),
        )
}
