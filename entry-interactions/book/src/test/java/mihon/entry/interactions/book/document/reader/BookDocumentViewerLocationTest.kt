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
    fun `visible block maps viewport position into document logical progression`() {
        val section = section("current", listOf("a".repeat(100), "b".repeat(100)))
        val item = BookDocumentViewerItem.Block(section, section.document.blocks[1])

        val location = bookDocumentViewerLocation(
            items = listOf(item),
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(index = 0, key = item.key, offset = 0, size = 800),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertNotNull(location)
        assertEquals(section.document.blocks[1].id, location.position.blockId)
        assertEquals(50, location.position.offsetWithinBlock)
        assertEquals(152f / 202f, location.progression)
    }

    @Test
    fun `progress follows the block containing the viewport anchor rather than a nearby block center`() {
        val section = section("current", listOf("a".repeat(100), "b".repeat(100)))
        val items = section.document.blocks.map { BookDocumentViewerItem.Block(section, it) }

        val location = bookDocumentViewerLocation(
            items = items,
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(index = 0, key = items[0].key, offset = -1_400, size = 2_000),
                BookDocumentVisibleItemLayout(index = 1, key = items[1].key, offset = 600, size = 100),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertNotNull(location)
        assertEquals(section.document.blocks[0].id, location.position.blockId)
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
        val position = second.initialPosition

        val index = items.indexOfPosition(second.key, position)

        val item = assertIs<BookDocumentViewerItem.Block<String>>(items[index])
        assertEquals("second", item.section.key)
    }

    @Test
    fun `chapter start aligned to viewport top reports exact zero`() {
        val section = section("current", listOf("a".repeat(10), "b".repeat(100)))
        val firstItem = BookDocumentViewerItem.Block(section, section.document.blocks.first())
        val secondItem = BookDocumentViewerItem.Block(section, section.document.blocks.last())

        assertEquals(
            0,
            bookDocumentScrollOffset(
                document = section.document,
                position = section.initialPosition,
                itemSize = 100,
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            ),
        )
        val location = bookDocumentViewerLocation(
            items = listOf(firstItem, secondItem),
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(index = 0, key = firstItem.key, offset = 0, size = 100),
                BookDocumentVisibleItemLayout(index = 1, key = secondItem.key, offset = 100, size = 1_000),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertNotNull(location)
        assertEquals(0, location.position.offsetWithinBlock)
        assertEquals(0f, location.progression)
    }
}
