package mihon.entry.interactions.book.document.reader.navigation

import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.book.document.reader.paging.BookDocumentPage
import mihon.entry.interactions.book.document.reader.paging.BookDocumentPageFragment
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class BookDocumentSeekSnapshotTest {
    @Test
    fun `percentage seeking produces exact local passages and reaches both ends`() {
        val section = seekSection()
        val document = section.document.document
        val snapshot = BookDocumentSeekSnapshot(section, section.initialPosition, emptyList())
        val middle = snapshot.targetAt(50f)
        assertTrue(middle.restorePosition)
        assertEquals(document.resourceId, middle.locator?.resourceId)
        assertEquals(document.positionAtProgression(0f), snapshot.positionAt(0f))
        assertEquals(document.positionAtProgression(1f), snapshot.positionAt(100f))
        assertEquals(snapshot.positionAt(50f), document.resolvePosition(requireNotNull(middle.locator)))
        assertNotEquals(snapshot.previewAt(0f), snapshot.previewAt(100f))
    }

    @Test
    fun `scroll percentage describes the navigable viewport range and reaches one hundred at the end`() {
        val section = seekSection()
        val document = section.document.document
        val snapshot = BookDocumentSeekSnapshot(
            section,
            document.positionAtProgression(.45f),
            emptyList(),
            viewportEndProgression = .55f,
        )
        assertEquals(50f, snapshot.value, .2f)
        assertEquals(document.positionAtProgression(.45f), snapshot.positionAt(50f))
        val end = snapshot.copy(position = document.positionAtProgression(.9f), viewportEndProgression = 1f)
        assertEquals(100f, end.value)
    }

    @Test
    fun `page seeking is confined to the visible EPUB section despite prefetched neighbours`() {
        val section = seekSection("current")
        val neighbour = seekSection("next")
        val state = BookDocumentSeekState()
        val pages = (section.viewerBlocks + neighbour.viewerBlocks).map {
            BookDocumentPage(listOf(BookDocumentPageFragment(it)))
        }
        state.updatePages(pages)
        state.observe(BookDocumentViewerLocation(section, section.initialPosition, 0f))
        state.setVisible(true)
        val snapshot = requireNotNull(state.snapshot)
        assertEquals(1f..2f, snapshot.range)
        val destination = snapshot.targetAt(2f)
        assertEquals("current", destination.locator?.resourceId)
        assertEquals(section.document.blocks[1].id, snapshot.positionAt(2f).blockId)
        assertEquals(section.owner.id, destination.chapter.id)
    }
}
