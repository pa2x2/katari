package mihon.entry.interactions.book.document.reader

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
internal class BookDocumentViewerDatasetTest : BookDocumentViewerFixture() {
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
    fun `partially visible terminal forward boundary does not manufacture content completion`() {
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
        assertTrue(location.progression < 1f)
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
    fun `transition with a loaded destination emits no redundant load request`() {
        val current = section("current", listOf("One", "Two"))
        val next = section("next", listOf("Three", "Four"))
        val items = buildBookDocumentViewerItems(
            window = EntryChildWindow("current", null, "next"),
            loaded = mapOf("current" to current, "next" to next),
            keyOf = { it },
        )
        val transitionIndex = items.indexOfFirst {
            it is BookDocumentViewerItem.Transition && it.transition.to == "next"
        }
        val transition = assertIs<BookDocumentViewerItem.Transition<String>>(items[transitionIndex])

        val reached = bookDocumentViewerTransitionAtAnchor(
            items = items,
            visibleItems = listOf(
                BookDocumentVisibleItemLayout(
                    index = transitionIndex,
                    key = transition.key,
                    offset = 100,
                    size = 600,
                ),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
        )

        assertNull(reached)
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
}
