package mihon.entry.interactions.book.document.reader

import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentChapterEndTest : BookDocumentViewerFixture() {
    @Test
    fun `a chapter followed by another one ends where the bottom of its final block scrolls into view`() {
        val current = chapter(1)
        val next = chapter(2)
        val loaded = mapOf(
            current.id to chapterSection(current, listOf("One", "Two")),
            next.id to chapterSection(next, listOf("Three")),
        ).mapValues { (_, section) -> BookDocumentPublicationSections(listOf(section), section.key) }
        val window = EntryChildWindow(current, next = next)
        val items = buildBookDocumentPublicationViewerItems(window, loaded, { it.id })
        val finalBlock = loaded.getValue(current.id).sections.single().viewerBlocks.last()
        val finalIndex = items.indexOf(finalBlock)
        fun layoutEndingAt(bottom: Int) = listOf(
            BookDocumentVisibleItemLayout(finalIndex, finalBlock.key, offset = bottom - 300, size = 300),
            BookDocumentVisibleItemLayout(finalIndex + 1, items[finalIndex + 1].key, offset = bottom, size = 200),
        )

        val end = assertNotNull(bookDocumentChapterEnd(window, loaded))

        assertTrue(end.isVisibleIn(items, layoutEndingAt(bottom = 700), viewportEndOffset = 800))
        assertFalse(end.isVisibleIn(items, layoutEndingAt(bottom = 900), viewportEndOffset = 800))
    }
}
