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

    @Test
    fun `scrolling progression maps to the scrollable part of the chapter`() {
        assertEquals(600, scrollOffsetForProgression(itemSize = 2000, viewportSize = 800, progression = 0.5f))
        assertEquals(0, scrollOffsetForProgression(itemSize = 600, viewportSize = 800, progression = 0.5f))
        assertEquals(1200, scrollOffsetForProgression(itemSize = 2000, viewportSize = 800, progression = 2f))
    }

    @Test
    fun `scrolling location snapshots live chapter progress`() {
        val chapter = loaded(chapter(2L))
        val items = listOf(ProseScrollItem.Chapter(chapter))

        val first = scrollingProseLocation(
            items = items,
            visibleItems = listOf(ProseVisibleItemLayout(index = 0, offset = -400, size = 2400)),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )
        val second = scrollingProseLocation(
            items = items,
            visibleItems = listOf(ProseVisibleItemLayout(index = 0, offset = -800, size = 2400)),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertEquals(0.25f, first?.progression)
        assertEquals(0.5f, second?.progression)
    }

    @Test
    fun `scrolling location does not attribute a transition to an adjacent chapter`() {
        val chapter = loaded(chapter(2L))
        val items = listOf(
            ProseScrollItem.Chapter(chapter),
            ProseScrollItem.Transition(EntryChildWindow(chapter.chapter, null, chapter(3L)).nextTransition()),
        )

        val location = scrollingProseLocation(
            items = items,
            visibleItems = listOf(
                ProseVisibleItemLayout(index = 0, offset = -1400, size = 2000),
                ProseVisibleItemLayout(index = 1, offset = 600, size = 800),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertEquals(null, location)
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

    private fun loaded(chapter: EntryChapter) = HtmlProseLoadedChapter(chapter, "chapter-${chapter.id}", "Text", 0f)

    private fun page(chapter: EntryChapter, index: Int = 0, total: Int = 1) =
        HtmlProsePage(chapter, index, total, "Text")
}
