package mihon.entry.interactions.book.document.reader.paging

import mihon.entry.interactions.book.document.reader.BookDocumentChapterEnd
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.BookDocumentViewerFixture
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.reader.BookReaderProgress
import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentPageProgressTest : BookDocumentViewerFixture() {
    @Test
    fun `content pages report their position within their own section`() {
        val section = chapterSection(chapter(1), listOf("One", "Two", "Three"))

        assertEquals(BookReaderProgress.Page(1, 3), contentPages(section).pageProgress(0))
        assertEquals(BookReaderProgress.Page(2, 3), contentPages(section).pageProgress(1))
        assertEquals(BookReaderProgress.Page(3, 3), contentPages(section).pageProgress(2))
    }

    @Test
    fun `a next transition page inherits the position it leads from`() {
        val current = chapter(1)
        val next = chapter(2)
        val currentSection = chapterSection(current, listOf("One", "Two"))
        val nextSection = chapterSection(next, listOf("Three"))

        val pages = contentPages(currentSection) +
            transitionPage(EntryChildWindow(current, next = next).nextTransition()) +
            contentPages(nextSection)

        assertEquals(BookReaderProgress.Page(2, 2), pages.pageProgress(2))
    }

    @Test
    fun `a previous transition page inherits the position it leads from`() {
        val previous = chapter(1)
        val current = chapter(2)
        val previousSection = chapterSection(previous, listOf("Zero"))
        val currentSection = chapterSection(current, listOf("One", "Two"))

        val pages = contentPages(previousSection) +
            transitionPage(EntryChildWindow(current, previous = previous).previousTransition()) +
            contentPages(currentSection)

        assertEquals(BookReaderProgress.Page(1, 2), pages.pageProgress(1))
    }

    @Test
    fun `a transition page whose origin holds no content reports no progress`() {
        val pages = listOf(
            transitionPage(EntryChildWindow(chapter(1), next = chapter(2)).nextTransition()),
        )

        assertNull(pages.pageProgress(0))
    }

    @Test
    fun `a transition page never consumes a page number of its origin chapter`() {
        val current = chapter(1)
        val next = chapter(2)
        val currentSection = chapterSection(current, listOf("One", "Two"))
        val nextSection = chapterSection(next, listOf("Three", "Four"))

        val pages = contentPages(currentSection) +
            transitionPage(EntryChildWindow(current, next = next).nextTransition()) +
            contentPages(nextSection)

        // The indicator must stay anchored to content page counts of the owning sections.
        assertEquals(BookReaderProgress.Page(2, 2), pages.pageProgress(2))
        assertEquals(BookReaderProgress.Page(1, 2), pages.pageProgress(3))
    }

    @Test
    fun `the page rendering the final block tail is chapter-end evidence`() {
        val current = chapter(1)
        val next = chapter(2)
        val currentSection = chapterSection(current, listOf("One", "Two"))
        val nextSection = chapterSection(next, listOf("Three"))
        val end = BookDocumentChapterEnd(
            chapter = current,
            finalSectionKey = currentSection.key,
            finalBlockId = currentSection.document.blocks.last().id,
        )
        val pages = contentPages(currentSection) +
            transitionPage(EntryChildWindow(current, next = next).nextTransition()) +
            contentPages(nextSection)

        // The chapter's final content page, the trailing transition page and the next chapter.
        assertTrue(pages[1].endsChapterContent(end))
        assertFalse(pages[2].endsChapterContent(end))
        assertFalse(pages[3].endsChapterContent(end))
    }

    @Test
    fun `a page holding only an earlier portion of the final block is not the chapter end`() {
        val section = chapterSection(chapter(1), listOf("One", "Two"))
        val end = BookDocumentChapterEnd(
            chapter = section.owner,
            finalSectionKey = section.key,
            finalBlockId = section.document.blocks.last().id,
        )
        val lastBlock = section.viewerBlocks.last()

        val partialPage = BookDocumentPage(
            listOf(
                BookDocumentPageFragment(lastBlock, start = 0, end = 1, firstOnPage = true, lastOnPage = false),
            ),
        )
        val tailPage = BookDocumentPage(
            listOf(
                BookDocumentPageFragment(lastBlock, start = 1, end = lastBlock.content.logicalLength),
            ),
        )

        assertFalse(partialPage.endsChapterContent(end))
        assertTrue(tailPage.endsChapterContent(end))
    }

    @Test
    fun `evidence ignores a final block of a different section`() {
        val current = chapter(1)
        val other = chapterSection(chapter(9), listOf("Elsewhere"))
        val currentSection = chapterSection(current, listOf("One"))
        val end = BookDocumentChapterEnd(
            chapter = current,
            finalSectionKey = currentSection.key,
            finalBlockId = currentSection.document.blocks.last().id,
        )

        assertFalse(contentPages(other).single().endsChapterContent(end))
    }

    @Test
    fun `a transition page never ends chapter content`() {
        val current = chapter(1)
        val section = chapterSection(current, listOf("One"))
        val end = BookDocumentChapterEnd(
            chapter = current,
            finalSectionKey = section.key,
            finalBlockId = section.document.blocks.last().id,
        )

        assertFalse(
            transitionPage(EntryChildWindow(current, next = chapter(2)).nextTransition()).endsChapterContent(end),
        )
    }

    private fun contentPages(section: BookDocumentSection<EntryChapter>): List<BookDocumentPage> =
        section.viewerBlocks.map { block ->
            BookDocumentPage(
                listOf(
                    BookDocumentPageFragment(block, firstOnPage = true, lastOnPage = true),
                ),
            )
        }

    private fun transitionPage(transition: EntryChildTransition<EntryChapter>) = BookDocumentPage(
        listOf(
            BookDocumentPageFragment(
                BookDocumentViewerItem.Transition(transition, "boundary"),
                firstOnPage = true,
                lastOnPage = true,
            ),
        ),
        scrollable = true,
    )
}
