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
