package mihon.entry.interactions.book.document.reader.paging

import mihon.entry.interactions.book.document.reader.BookDocumentChapterEnd
import mihon.entry.interactions.book.document.reader.BookDocumentViewerFixture
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentPageProgressTest : BookDocumentViewerFixture() {
    @Test
    fun `only the page rendering the tail of the chapter's final block is chapter-end evidence`() {
        val current = chapter(1)
        val section = chapterSection(current, listOf("One", "Two"))
        val other = chapterSection(chapter(9), listOf("Elsewhere"))
        val end = BookDocumentChapterEnd(
            chapter = current,
            finalSectionKey = section.key,
            finalBlockId = section.document.blocks.last().id,
        )
        val lastBlock = section.viewerBlocks.last()
        val partialPage = BookDocumentPage(
            listOf(BookDocumentPageFragment(lastBlock, start = 0, end = 1, firstOnPage = true, lastOnPage = false)),
        )
        val tailPage = BookDocumentPage(
            listOf(BookDocumentPageFragment(lastBlock, start = 1, end = lastBlock.content.logicalLength)),
        )
        val transitionPage = BookDocumentPage(
            listOf(
                BookDocumentPageFragment(
                    BookDocumentViewerItem.Transition(
                        EntryChildWindow(current, next = chapter(2)).nextTransition(),
                        "boundary",
                    ),
                ),
            ),
            scrollable = true,
        )
        val otherSectionPage = BookDocumentPage(listOf(BookDocumentPageFragment(other.viewerBlocks.single())))

        assertTrue(tailPage.endsChapterContent(end))
        assertFalse(partialPage.endsChapterContent(end))
        assertFalse(transitionPage.endsChapterContent(end))
        assertFalse(otherSectionPage.endsChapterContent(end))
    }
}
