package mihon.entry.interactions.book.document.reader

import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentChapterEndTest : BookDocumentViewerFixture() {
    @Test
    fun `terminal chapter evidence points at its final content block`() {
        val chapter = chapter(1)
        val first = chapterSection(chapter, listOf("First document"))
        val second = chapterSection(chapter, listOf("Second document")).copy(key = "1:second")

        val end = bookDocumentChapterEnd(
            window = EntryChildWindow(chapter),
            loadedSections = mapOf(
                chapter.id to BookDocumentPublicationSections(
                    sections = listOf(first, second),
                    initialSectionKey = first.key,
                ),
            ),
        )

        assertEquals(chapter, end?.chapter)
        assertEquals("1:second", end?.finalSectionKey)
        assertEquals(second.document.blocks.last().id, end?.finalBlockId)
    }

    @Test
    fun `a chapter with a loaded reading-order successor has no chapter-end evidence`() {
        val current = chapter(1)
        val next = chapter(2)
        val section = chapterSection(current, listOf("Content"))

        assertNull(
            bookDocumentChapterEnd(
                window = EntryChildWindow(current, next = next),
                loadedSections = mapOf(current.id to BookDocumentPublicationSections(listOf(section), section.key)),
            ),
        )
    }

    @Test
    fun `an unloaded terminal chapter has no chapter-end evidence`() {
        assertNull(
            bookDocumentChapterEnd(
                window = EntryChildWindow(chapter(1)),
                loadedSections = emptyMap(),
            ),
        )
    }

    @Test
    fun `terminal evidence follows the terminal chapter when the window rebases`() {
        val previous = chapter(1)
        val terminal = chapter(2)
        val previousSection = chapterSection(previous, listOf("Earlier content"))
        val terminalSection = chapterSection(terminal, listOf("Final content"))
        val loaded = mapOf(
            previous.id to BookDocumentPublicationSections(listOf(previousSection), previousSection.key),
            terminal.id to BookDocumentPublicationSections(listOf(terminalSection), terminalSection.key),
        )

        val whileReading = bookDocumentChapterEnd(EntryChildWindow(previous, next = terminal), loaded)
        assertNull(whileReading)

        val afterCrossing = bookDocumentChapterEnd(EntryChildWindow(terminal, previous = previous), loaded)
        assertEquals(terminal, afterCrossing?.chapter)
        assertEquals(
            terminalSection.document.blocks.last().id,
            afterCrossing?.finalBlockId,
        )
    }
}
