package mihon.entry.interactions.book.document.reader

import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentViewerLocationTest : BookDocumentViewerFixture() {
    @Test
    fun `the block containing the viewport anchor maps into document logical progression`() {
        val section = section("current", listOf("a".repeat(100), "b".repeat(100)))
        val items = section.document.blocks.map { BookDocumentViewerItem.Block(section, it) }

        val second = assertNotNull(
            bookDocumentViewerLocation(
                items = listOf(items[1]),
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = items[1].key, offset = 0, size = 800),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            ),
        )
        assertEquals(section.document.blocks[1].id, second.position.blockId)
        assertEquals(50, second.position.offsetWithinBlock)
        assertEquals(152f / 202f, second.progression)

        val tallFirst = assertNotNull(
            bookDocumentViewerLocation(
                items = items,
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = items[0].key, offset = -1_400, size = 2_000),
                    BookDocumentVisibleItemLayout(index = 1, key = items[1].key, offset = 600, size = 100),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            ),
        )
        assertEquals(section.document.blocks[0].id, tallFirst.position.blockId)

        val atStart = assertNotNull(
            bookDocumentViewerLocation(
                items = items,
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = items[0].key, offset = 0, size = 100),
                    BookDocumentVisibleItemLayout(index = 1, key = items[1].key, offset = 100, size = 1_000),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            ),
        )
        assertEquals(0, atStart.position.offsetWithinBlock)
        assertEquals(0f, atStart.progression)
    }

    @Test
    fun `location follows visible key when section crossing shifts item indexes`() {
        val first = section("first", listOf("One", "Two"))
        val second = section("second", listOf("Three", "Four"))
        val third = section("third", listOf("Five", "Six"))
        val fourth = section("fourth", listOf("Seven", "Eight"))
        val loaded = listOf(first, second, third, fourth).associateBy { it.owner }
        val beforeCrossing = buildBookDocumentViewerItems(
            window = EntryChildWindow("second", "first", "third"),
            loaded = loaded,
            keyOf = { it },
        )
        val afterCrossing = buildBookDocumentViewerItems(
            window = EntryChildWindow("third", "second", "fourth"),
            loaded = loaded,
            keyOf = { it },
        )
        val visible = beforeCrossing
            .filterIsInstance<BookDocumentViewerItem.Block<String>>()
            .last { it.section.owner == "second" }
        val staleIndex = beforeCrossing.indexOf(visible)
        assertEquals(
            "third",
            assertIs<BookDocumentViewerItem.Block<String>>(afterCrossing[staleIndex]).section.owner,
        )

        val location = bookDocumentViewerLocation(
            items = afterCrossing,
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(
                    index = staleIndex,
                    key = visible.key,
                    offset = 0,
                    size = 800,
                ),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertNotNull(location)
        assertEquals("second", location.section.owner)
    }

    @Test
    fun `position lookup disambiguates identical resource blocks in adjacent source children`() {
        val first = section("first", listOf("Same resource content"))
        val second = section("second", listOf("Same resource content"))
        val items = buildBookDocumentViewerItems(
            window = EntryChildWindow("second", "first", null),
            loaded = mapOf("first" to first, "second" to second),
            keyOf = { it },
        )

        val index = items.indexOfPosition(second.key, second.initialPosition)

        val item = assertIs<BookDocumentViewerItem.Block<String>>(items[index])
        assertEquals("second", item.section.key)
    }
}
