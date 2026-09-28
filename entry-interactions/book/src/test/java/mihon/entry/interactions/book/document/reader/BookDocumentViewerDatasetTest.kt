package mihon.entry.interactions.book.document.reader

import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentViewerDatasetTest : BookDocumentViewerFixture() {
    @Test
    fun `chapter activation keeps shared boundary keys and adopts only adjacent loaded sections`() {
        val first = section("first", listOf("One", "Two"))
        val second = section("second", listOf("Three", "Four"))
        val third = section("third", listOf("Five", "Six"))
        val fourth = section("fourth", listOf("Seven"))
        val loaded = listOf(first, second, third, fourth).associateBy { it.owner }
        val whileReadingSecond = buildBookDocumentViewerItems(
            window = EntryChildWindow("second", "first", "third"),
            loaded = loaded.filterKeys { it != "fourth" },
            keyOf = { it },
        )
        val afterActivatingThird = buildBookDocumentViewerItems(
            window = EntryChildWindow("third", "second", null),
            loaded = loaded.filterKeys { it != "fourth" },
            keyOf = { it },
        )
        val afterActivatingFirst = buildBookDocumentViewerItems(
            window = EntryChildWindow("first", null, "second"),
            loaded = loaded.filterKeys { it != "fourth" },
            keyOf = { it },
        )
        val rebasedElsewhere = buildBookDocumentViewerItems(
            window = EntryChildWindow("fourth", "second", null),
            loaded = loaded.filterKeys { it == "second" || it == "fourth" },
            keyOf = { it },
        )

        val forwardBoundary = whileReadingSecond
            .filterIsInstance<BookDocumentViewerItem.Transition<String>>()
            .single { it.transition.from == "second" && it.transition.to == "third" }
        val backwardBoundary = afterActivatingThird
            .filterIsInstance<BookDocumentViewerItem.Transition<String>>()
            .single { it.transition.from == "third" && it.transition.to == "second" }
        assertEquals(forwardBoundary.key, backwardBoundary.key)
        assertTrue(whileReadingSecond.advancesToLoadedNext(afterActivatingThird))
        assertTrue(whileReadingSecond.retreatsToLoadedPrevious(afterActivatingFirst))
        assertFalse(whileReadingSecond.advancesToLoadedNext(rebasedElsewhere))
    }

    @Test
    fun `a tail expansion keeps the window boundaries while sections change`() {
        val first = section("first", listOf("One"))
        val second = section("second", listOf("Two"))
        val third = section("third", listOf("Three"))
        val fourth = section("fourth", listOf("Four"))
        val loaded = listOf(first, second, third, fourth).associateBy { it.owner }
        val window = EntryChildWindow("second", "first", "third")

        val before = buildBookDocumentViewerItems(window, loaded.filterKeys { it != "third" }, keyOf = { it })
        val after = buildBookDocumentViewerItems(window, loaded, keyOf = { it })
        assertTrue(before.isStablePrefixOf(after))

        val rebased = buildBookDocumentViewerItems(
            EntryChildWindow("second", "first", "fourth"),
            loaded.filterKeys { it != "third" },
            keyOf = { it },
        )
        assertFalse(before.isStablePrefixOf(rebased))
    }

    @Test
    fun `visible transitions never produce or complete a content location`() {
        val section = section("current", listOf("a".repeat(100)))
        val block = BookDocumentViewerItem.Block(section, section.document.blocks.single())
        val next = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", null, "next").nextTransition(),
            "next-boundary",
        )
        val terminal = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", null, null).nextTransition(),
            "terminal-boundary",
        )
        val previous = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", null, "next").previousTransition(),
            "previous-boundary",
        )

        assertNull(
            bookDocumentViewerLocation(
                items = listOf(block, next),
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = block.key, offset = -600, size = 700),
                    BookDocumentVisibleItemLayout(index = 1, key = next.key, offset = 100, size = 700),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            ),
        )
        val beforeTerminal = assertNotNull(
            bookDocumentViewerLocation(
                items = listOf(block, terminal),
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = block.key, offset = -50, size = 700),
                    BookDocumentVisibleItemLayout(index = 1, key = terminal.key, offset = 650, size = 600),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            ),
        )
        assertTrue(beforeTerminal.progression < 1f)
        val afterPrevious = assertNotNull(
            bookDocumentViewerLocation(
                items = listOf(previous, block),
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = 0, key = previous.key, offset = -100, size = 200),
                    BookDocumentVisibleItemLayout(index = 1, key = block.key, offset = 100, size = 700),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            ),
        )
        assertTrue(afterPrevious.progression < 1f)
    }

    @Test
    fun `a transition requests only an unloaded destination at the reading anchor`() {
        val current = section("current", listOf("One", "Two"))
        val next = section("next", listOf("Three", "Four"))
        val unloaded = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", null, "next").nextTransition(),
            "boundary",
        )
        fun reached(items: List<BookDocumentViewerItem<String>>, index: Int, offset: Int, size: Int) =
            bookDocumentViewerTransitionAtAnchor(
                items = items,
                visibleItems = listOf(
                    BookDocumentVisibleItemLayout(index = index, key = items[index].key, offset = offset, size = size),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            )

        assertEquals("next", reached(listOf(unloaded), 0, offset = 100, size = 600)?.to)
        assertNull(reached(listOf(unloaded), 0, offset = 650, size = 200))

        val loadedItems = buildBookDocumentViewerItems(
            window = EntryChildWindow("current", null, "next"),
            loaded = mapOf("current" to current, "next" to next),
            keyOf = { it },
        )
        val loadedTransition = loadedItems.indexOfFirst {
            it is BookDocumentViewerItem.Transition && it.transition.to == "next"
        }
        assertNull(reached(loadedItems, loadedTransition, offset = 100, size = 600))
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
        val section = section("current", listOf("Text"))
        val block = BookDocumentViewerItem.Block(section, section.document.blocks.single())
        val terminal = BookDocumentViewerItem.Transition(
            EntryChildWindow("current", "previous", null).nextTransition(),
            "terminal-boundary",
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
        // A non-scrollable final chapter still reaches its unloaded previous boundary.
        assertEquals(
            "previous",
            bookDocumentViewerTransitionAtAnchor(
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
            )?.to,
        )
    }
}
