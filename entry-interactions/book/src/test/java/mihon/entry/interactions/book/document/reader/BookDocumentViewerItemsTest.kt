package mihon.entry.interactions.book.document.reader

import android.text.SpannableString
import mihon.entry.interactions.book.document.model.BookDocument
import mihon.entry.interactions.book.document.model.BookDocumentBlock
import mihon.entry.interactions.book.document.model.BookDocumentBlockId
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class BookDocumentViewerItemsTest {
    @Test
    fun `crossing a section preserves the shared boundary key while blocks move`() {
        val first = section("first", listOf("One", "Two"))
        val second = section("second", listOf("Three", "Four"))
        val third = section("third", listOf("Five", "Six"))
        val loaded = listOf(first, second, third).associateBy { it.owner }
        val beforeCrossing = buildBookDocumentViewerItems(
            window = EntryChildWindow("second", "first", "third"),
            loaded = loaded,
            keyOf = { it },
        )
        val afterCrossing = buildBookDocumentViewerItems(
            window = EntryChildWindow("third", "second", null),
            loaded = loaded,
            keyOf = { it },
        )

        val forwardBoundary = beforeCrossing
            .filterIsInstance<BookDocumentViewerItem.Transition<String>>()
            .single { it.transition.from == "second" && it.transition.to == "third" }
        val backwardBoundary = afterCrossing
            .filterIsInstance<BookDocumentViewerItem.Transition<String>>()
            .single { it.transition.from == "third" && it.transition.to == "second" }

        assertEquals(forwardBoundary.key, backwardBoundary.key)
    }

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
        assertEquals(section.document.blocks[1].block.id, location.position.blockId)
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
        assertEquals(section.document.blocks[0].block.id, location.position.blockId)
    }

    @Test
    fun `transition nearest the reading anchor produces no content location`() {
        val section = section("current", listOf("Text"))
        val transition = EntryChildWindow("current", null, "next").nextTransition()
        val items = listOf(
            BookDocumentViewerItem.Block(section, section.document.blocks.single()),
            BookDocumentViewerItem.Transition(transition, "boundary"),
        )

        val location = bookDocumentViewerLocation(
            items = items,
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(index = 0, key = items[0].key, offset = -600, size = 700),
                BookDocumentVisibleItemLayout(index = 1, key = items[1].key, offset = 100, size = 700),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertEquals(null, location)
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

    private fun section(owner: String, texts: List<String>): BookDocumentSection<String> {
        var offset = 0
        val blocks = texts.mapIndexed { index, text ->
            if (index > 0) offset += 2
            val start = offset
            offset += text.length
            BookDocumentBlock(
                id = BookDocumentBlockId("block-$index"),
                role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
                plainText = text,
                sourceFragments = emptyList(),
                logicalStart = start,
                logicalEndExclusive = offset,
            )
        }
        val document = BookDocument(
            resourceId = "shared-resource",
            revision = "r1",
            blocks = blocks,
            anchors = emptyMap(),
            logicalExtent = offset,
        )
        val prepared = PreparedBookDocument(
            document = document,
            blocks = blocks.map { PreparedBookDocumentBlock(it, SpannableString(it.plainText)) },
            combinedText = SpannableString(texts.joinToString("\n\n")),
        )
        return BookDocumentSection(
            key = owner,
            owner = owner,
            document = prepared,
            initialPosition = BookDocumentPosition(blocks.first().id, 0),
        )
    }
}
