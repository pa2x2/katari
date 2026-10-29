package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentViewerRestorationTest : BookDocumentViewerFixture() {
    @Test
    fun `restoring a block position returns it to the viewport reading anchor`() {
        val section = section("current", listOf("a".repeat(100)))
        val item = BookDocumentViewerItem.Block(section, section.document.blocks.single())
        for (offsetWithinBlock in listOf(20, 70)) {
            val scrollOffset = bookDocumentScrollOffset(
                document = section.document,
                position = BookDocumentPosition(item.content.id, offsetWithinBlock),
                itemSize = 1_000,
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            )

            val restored = bookDocumentViewerLocation(
                items = listOf(item),
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = item.key, offset = -scrollOffset, size = 1_000),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            )

            assertEquals(offsetWithinBlock, assertNotNull(restored).position.offsetWithinBlock)
        }
    }

    @Test
    fun `loading a reached previous transition preserves its stable pixel anchor`() {
        val previous = section("previous", listOf("One", "Two", "Three"))
        val current = section("current", listOf("Four", "Five"))
        val window = EntryChildWindow("current", "previous", null)
        val beforeLoading = buildBookDocumentViewerItems(
            window = window,
            loaded = mapOf("current" to current),
            keyOf = { it },
        )
        val afterLoading = buildBookDocumentViewerItems(
            window = window,
            loaded = mapOf("previous" to previous, "current" to current),
            keyOf = { it },
        )
        val transition = assertIs<BookDocumentViewerItem.Transition<String>>(beforeLoading.first())
        val currentBlock = assertIs<BookDocumentViewerItem.Block<String>>(beforeLoading[1])

        val anchor = bookDocumentViewerDatasetAnchor(
            items = afterLoading,
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(
                    index = 0,
                    key = transition.key,
                    offset = 300,
                    size = 200,
                ),
                BookDocumentVisibleItemLayout(
                    index = 1,
                    key = currentBlock.key,
                    offset = 500,
                    size = 800,
                ),
            ),
            viewportStartOffset = 0,
        )

        assertNotNull(anchor)
        assertEquals(afterLoading.indexOfFirst { it.key == transition.key }, anchor.index)
        assertEquals(-300, anchor.scrollOffset)
    }
}
