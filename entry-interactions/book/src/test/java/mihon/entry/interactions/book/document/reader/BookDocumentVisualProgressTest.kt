package mihon.entry.interactions.book.document.reader

import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentVisualProgressTest : BookDocumentViewerFixture() {
    @Test
    fun `short chapter advances through its transition before resetting at the next chapter start`() {
        val current = section("current", listOf("a".repeat(100)))
        val next = section("next", listOf("b".repeat(1_000)))
        val items = buildBookDocumentViewerItems(
            window = EntryChildWindow("current", null, "next"),
            loaded = mapOf("current" to current, "next" to next),
            keyOf = { it },
        )
        val currentBlock = items.block("current")
        val transition = items.transition("current", "next")
        val nextBlock = items.block("next")
        val tracker = BookDocumentVisualProgressTracker()

        val start = progress(
            items,
            tracker,
            layout(items, currentBlock, 0, 100),
            layout(items, transition, 100, 200),
            layout(items, nextBlock, 300, 1_000),
        )
        val transitionStart = progress(
            items,
            tracker,
            layout(items, transition, 0, 200),
            layout(items, nextBlock, 200, 1_000),
        )
        val transitionEnd = progress(
            items,
            tracker,
            layout(items, transition, -199, 200),
            layout(items, nextBlock, 1, 1_000),
        )
        val nextStart = progress(
            items,
            tracker,
            layout(items, nextBlock, 0, 1_000),
        )

        assertEquals("current", start.section.owner)
        assertEquals(0f, start.progression)
        assertEquals(1f / 3f, transitionStart.progression, 0.001f)
        assertTrue(transitionEnd.progression > 0.99f)
        assertEquals("next", nextStart.section.owner)
        assertEquals(0f, nextStart.progression)
    }

    @Test
    fun `visual progress keeps advancing while a transition produces no resume location`() {
        val current = section("current", listOf("a".repeat(2_000)))
        val next = section("next", listOf("b".repeat(2_000)))
        val items = buildBookDocumentViewerItems(
            window = EntryChildWindow("current", null, "next"),
            loaded = mapOf("current" to current, "next" to next),
            keyOf = { it },
        )
        val currentBlock = items.block("current")
        val transition = items.transition("current", "next")
        val nextBlock = items.block("next")
        val tracker = BookDocumentVisualProgressTracker()
        progress(
            items,
            tracker,
            layout(items, currentBlock, -1_200, 2_000),
            layout(items, transition, 800, 200),
        )
        val boundaryLayouts = listOf(
            layout(items, currentBlock, -1_700, 2_000),
            layout(items, transition, 300, 200),
            layout(items, nextBlock, 500, 2_000),
        )

        val location = bookDocumentViewerLocation(items, boundaryLayouts, 0, 800)
        val visualProgress = requireNotNull(
            bookDocumentVisualProgress(
                items = items,
                visibleItems = boundaryLayouts,
                viewportStartOffset = 0,
                viewportEndOffset = 800,
                canScrollForward = true,
                tracker = tracker,
            ),
        )

        assertNull(location)
        assertEquals("current", visualProgress.section.owner)
        assertEquals(1_700f / 2_200f, visualProgress.progression, 0.001f)
    }

    @Test
    fun `terminal transition reaches completion at the hard scroll boundary`() {
        val current = section("current", listOf("a".repeat(100)))
        val items = buildBookDocumentViewerItems(
            window = EntryChildWindow("current", null, null),
            loaded = mapOf("current" to current),
            keyOf = { it },
        )
        val currentBlock = items.block("current")
        val terminal = items.filterIsInstance<BookDocumentViewerItem.Transition<String>>()
            .single {
                it.transition.from == "current" &&
                    it.transition.to == null &&
                    it.transition.direction == EntryChildDirection.NEXT
            }

        val progress = requireNotNull(
            bookDocumentVisualProgress(
                items = items,
                visibleItems = listOf(
                    layout(items, currentBlock, 0, 100),
                    layout(items, terminal, 100, 200),
                ),
                viewportStartOffset = 0,
                viewportEndOffset = 800,
                canScrollForward = false,
                tracker = BookDocumentVisualProgressTracker(),
            ),
        )

        assertEquals(1f, progress.progression)
    }

    private fun progress(
        items: BookDocumentViewerDataset<String>,
        tracker: BookDocumentVisualProgressTracker,
        vararg layouts: BookDocumentVisibleItemLayout,
    ): BookDocumentViewerVisualProgress<String> = requireNotNull(
        bookDocumentVisualProgress(
            items = items,
            visibleItems = layouts.toList(),
            viewportStartOffset = 0,
            viewportEndOffset = 800,
            canScrollForward = true,
            tracker = tracker,
        ),
    )

    private fun BookDocumentViewerDataset<String>.block(owner: String) =
        filterIsInstance<BookDocumentViewerItem.Block<String>>().single { it.section.owner == owner }

    private fun BookDocumentViewerDataset<String>.transition(from: String, to: String) =
        filterIsInstance<BookDocumentViewerItem.Transition<String>>()
            .single { it.transition.from == from && it.transition.to == to }

    private fun layout(
        items: BookDocumentViewerDataset<String>,
        item: BookDocumentViewerItem<String>,
        offset: Int,
        size: Int,
    ) = BookDocumentVisibleItemLayout(
        index = items.indexOf(item),
        key = item.key,
        offset = offset,
        size = size,
    )
}
