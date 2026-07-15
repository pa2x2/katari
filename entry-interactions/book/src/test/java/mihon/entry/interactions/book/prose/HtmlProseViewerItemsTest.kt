package mihon.entry.interactions.book.prose

import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun `scrolling window keeps the same chapter transition order`() {
        val previous = loaded(chapter(1L))
        val current = loaded(chapter(2L))
        val next = loaded(chapter(3L))
        val items = buildScrollingItems(
            EntryChildWindow(current.chapter, previous.chapter, next.chapter),
            listOf(previous, current, next).associateBy { it.chapter.id },
        )

        assertEquals(5, items.size)
        assertIs<ProseScrollItem.Chapter>(items[0])
        assertIs<ProseScrollItem.Transition>(items[1])
        assertIs<ProseScrollItem.Chapter>(items[2])
        assertIs<ProseScrollItem.Transition>(items[3])
        assertIs<ProseScrollItem.Chapter>(items[4])
    }

    private fun chapter(id: Long) = EntryChapter.create().copy(id = id, entryId = 9L, name = "Chapter $id")

    private fun loaded(chapter: EntryChapter) = HtmlProseLoadedChapter(chapter, "chapter-${chapter.id}", "Text", 0f)

    private fun page(chapter: EntryChapter) = HtmlProsePage(chapter, 0, 1, "Text")
}
