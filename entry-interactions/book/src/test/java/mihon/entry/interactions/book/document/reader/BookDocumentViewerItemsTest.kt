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
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `restoring an early block position returns it to the viewport reading anchor`() {
        assertRestorationRoundTrip(offsetWithinBlock = 20)
    }

    @Test
    fun `restoring a late block position returns it to the viewport reading anchor`() {
        assertRestorationRoundTrip(offsetWithinBlock = 70)
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
    fun `partially visible terminal forward boundary reports exact chapter completion`() {
        val section = section("current", listOf("Text"))
        val block = BookDocumentViewerItem.Block(section, section.document.blocks.single())
        val terminal = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", null, null).nextTransition(),
            "terminal-boundary",
        )

        val location = bookDocumentViewerLocation(
            items = listOf(block, terminal),
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(index = 0, key = block.key, offset = -50, size = 700),
                BookDocumentVisibleItemLayout(index = 1, key = terminal.key, offset = 650, size = 600),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertNotNull(location)
        assertEquals("current", location.section.owner)
        assertEquals(section.document.blocks.single().block.logicalLength, location.position.offsetWithinBlock)
        assertEquals(1f, location.progression)
    }

    @Test
    fun `visible terminal previous boundary does not complete the chapter`() {
        val section = section("current", listOf("a".repeat(100)))
        val block = BookDocumentViewerItem.Block(section, section.document.blocks.single())
        val terminal = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", null, "next").previousTransition(),
            "terminal-boundary",
        )

        val location = bookDocumentViewerLocation(
            items = listOf(terminal, block),
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(index = 0, key = terminal.key, offset = -100, size = 200),
                BookDocumentVisibleItemLayout(index = 1, key = block.key, offset = 100, size = 700),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertNotNull(location)
        assertEquals("current", location.section.owner)
        assertTrue(location.progression < 1f)
    }

    @Test
    fun `unloaded neighbors contribute transitions without loading placeholders`() {
        val current = section("current", listOf("Text"))

        val items = buildBookDocumentViewerItems(
            window = EntryChildWindow("current", "previous", "next"),
            loaded = mapOf("current" to current),
            keyOf = { it },
        )

        assertEquals(3, items.size)
        assertIs<BookDocumentViewerItem.Transition<String>>(items[0])
        assertIs<BookDocumentViewerItem.Block<String>>(items[1])
        assertIs<BookDocumentViewerItem.Transition<String>>(items[2])
    }

    @Test
    fun `transition requests its destination only at the reading anchor`() {
        val transition = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", null, "next").nextTransition(),
            "boundary",
        )

        assertEquals(
            "next",
            bookDocumentViewerTransitionAtAnchor(
                items = listOf(transition),
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = transition.key, offset = 100, size = 600),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            )?.to,
        )
        assertNull(
            bookDocumentViewerTransitionAtAnchor(
                items = listOf(transition),
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = transition.key, offset = 650, size = 200),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            ),
        )
    }

    @Test
    fun `compact transitions activate only after reaching their hard scroll boundary`() {
        val previous = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", "previous", null).previousTransition(),
            "previous-boundary",
        )
        val next = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", null, "next").nextTransition(),
            "next-boundary",
        )

        assertEquals(
            "previous",
            bookDocumentViewerTransitionAtAnchor(
                items = listOf(previous),
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = previous.key, offset = 0, size = 200),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
                canScrollBackward = false,
            )?.to,
        )
        assertEquals(
            "next",
            bookDocumentViewerTransitionAtAnchor(
                items = listOf(next),
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = next.key, offset = 600, size = 200),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
                canScrollForward = false,
            )?.to,
        )
    }

    @Test
    fun `non-scrollable final chapter activates its unloaded previous boundary`() {
        val previous = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", "previous", null).previousTransition(),
            "previous-boundary",
        )
        val section = section("current", listOf("Text"))
        val block = BookDocumentViewerItem.Block(section, section.document.blocks.single())
        val terminal = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", "previous", null).nextTransition(),
            "terminal-boundary",
        )

        val reached = bookDocumentViewerTransitionAtAnchor(
            items = listOf(previous, block, terminal),
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(index = 0, key = previous.key, offset = 0, size = 100),
                BookDocumentVisibleItemLayout(index = 1, key = block.key, offset = 100, size = 200),
                BookDocumentVisibleItemLayout(index = 2, key = terminal.key, offset = 300, size = 500),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
            canScrollBackward = false,
            canScrollForward = false,
        )

        assertEquals("previous", reached?.to)
    }

    @Test
    fun `terminal transition at the reading anchor has no destination`() {
        val transition = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", null, null).nextTransition(),
            "boundary",
        )

        val reached = bookDocumentViewerTransitionAtAnchor(
            items = listOf(transition),
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(index = 0, key = transition.key, offset = 100, size = 600),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertNotNull(reached)
        assertNull(reached.to)
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

    private fun assertRestorationRoundTrip(offsetWithinBlock: Int) {
        val section = section("current", listOf("a".repeat(100)))
        val item = BookDocumentViewerItem.Block(section, section.document.blocks.single())
        val viewportStartOffset = 0
        val viewportEndOffset = 800
        val itemSize = 1_000
        val scrollOffset = bookDocumentScrollOffset(
            document = section.document,
            position = BookDocumentPosition(item.content.block.id, offsetWithinBlock),
            itemSize = itemSize,
            viewportStartOffset = viewportStartOffset,
            viewportEndOffset = viewportEndOffset,
        )

        val restored = bookDocumentViewerLocation(
            items = listOf(item),
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(
                    index = 0,
                    key = item.key,
                    offset = -scrollOffset,
                    size = itemSize,
                ),
            ),
            viewportStartOffset = viewportStartOffset,
            viewportEndOffset = viewportEndOffset,
        )

        assertNotNull(restored)
        assertEquals(offsetWithinBlock, restored.position.offsetWithinBlock)
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
            resourceLoader = null,
        )
    }
}
