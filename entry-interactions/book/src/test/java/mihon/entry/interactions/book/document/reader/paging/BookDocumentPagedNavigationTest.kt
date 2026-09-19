package mihon.entry.interactions.book.document.reader.paging

import mihon.entry.interactions.book.document.reader.BookDocumentViewerFixture
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.viewer.EntryChildTransition
import org.junit.Test
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BookDocumentPagedNavigationTest : BookDocumentViewerFixture() {
    private fun contentPages(chapter: EntryChapter, texts: List<String>): List<BookDocumentPage> {
        val section = chapterSection(chapter, texts)
        return section.document.blocks.map { block ->
            BookDocumentPage(listOf(BookDocumentPageFragment(BookDocumentViewerItem.Block(section, block))))
        }
    }

    private fun boundaryPage(transition: EntryChildTransition<EntryChapter>): BookDocumentPage = BookDocumentPage(
        listOf(BookDocumentPageFragment(BookDocumentViewerItem.Transition(transition, "boundary-page"))),
    )

    @Test
    fun `a resolved next boundary continues on the first page of its destination`() {
        val first = chapter(1)
        val second = chapter(2)
        val pages = buildList {
            addAll(contentPages(first, listOf("one")))
            add(boundaryPage(EntryChildTransition.Next(first, second)))
            addAll(contentPages(second, listOf("two")))
            add(boundaryPage(EntryChildTransition.Next(second, null)))
        }

        val target = pages.resolvedBoundaryTargetIndex(EntryChildTransition.Next(first, second))

        assertEquals(2, target)
    }

    @Test
    fun `a resolved previous boundary continues on the last page of its destination`() {
        val first = chapter(1)
        val second = chapter(2)
        val pages = buildList {
            addAll(contentPages(first, listOf("one-a", "one-b")))
            add(boundaryPage(EntryChildTransition.Prev(second, first)))
            addAll(contentPages(second, listOf("two")))
        }

        val target = pages.resolvedBoundaryTargetIndex(EntryChildTransition.Prev(second, first))

        assertEquals(1, target)
    }

    @Test
    fun `the destination's own transition pages are not destination content`() {
        val first = chapter(1)
        val second = chapter(2)
        val third = chapter(3)
        val pages = buildList {
            addAll(contentPages(first, listOf("one")))
            add(boundaryPage(EntryChildTransition.Prev(second, first)))
            addAll(contentPages(second, listOf("two")))
            add(boundaryPage(EntryChildTransition.Next(second, third)))
            addAll(contentPages(third, listOf("three")))
        }

        val target = pages.resolvedBoundaryTargetIndex(EntryChildTransition.Prev(third, second))

        assertEquals(2, target)
    }

    @Test
    fun `a destination outside the pages has no boundary target`() {
        val first = chapter(1)
        val pages = contentPages(first, listOf("one"))

        val target = pages.resolvedBoundaryTargetIndex(EntryChildTransition.Next(first, chapter(9)))

        assertNull(target)
    }

    @Test
    fun `terminal boundaries and missing anchors have no destination to continue into`() {
        val first = chapter(1)
        val pages = contentPages(first, listOf("one")) + boundaryPage(EntryChildTransition.Next(first, null))

        assertNull(pages.resolvedBoundaryTargetIndex(EntryChildTransition.Next(first, null)))
        assertNull(pages.resolvedBoundaryTargetIndex(null))
    }
}
