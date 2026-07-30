package mihon.entry.interactions.book.prose

import android.text.SpannableString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class ProsePagerStateTest {
    @Test
    fun `pagination uses the same independently rounded margins as the rendered page`() {
        val density = Density(2.625f)
        val containerWidth = with(density) { 1080.toDp() }

        assertEquals(974, paginatedContentExtentPx(containerWidth, 20.dp, density))
    }

    @Test
    fun `paginated window places transitions between chapter pages`() {
        val previous = chapter(1L)
        val current = chapter(2L)
        val next = chapter(3L)
        val window = EntryChildWindow(current, previous, next)
        val items = buildPaginatedItems(
            window,
            mapOf(
                previous.id to listOf(page(previous)),
                current.id to listOf(page(current)),
                next.id to listOf(page(next)),
            ),
        )

        assertEquals(5, items.size)
        assertEquals(previous.id, assertIs<ProsePagerItem.Page>(items[0]).page.chapter.id)
        assertEquals(
            EntryChildDirection.PREVIOUS,
            assertIs<ProsePagerItem.Transition>(items[1]).transition.direction,
        )
        assertEquals(current.id, assertIs<ProsePagerItem.Page>(items[2]).page.chapter.id)
        assertEquals(
            EntryChildDirection.NEXT,
            assertIs<ProsePagerItem.Transition>(items[3]).transition.direction,
        )
        assertEquals(next.id, assertIs<ProsePagerItem.Page>(items[4]).page.chapter.id)
    }

    @Test
    fun `unloaded paginated neighbors contribute only transition pages`() {
        val previous = chapter(1L)
        val current = chapter(2L)
        val next = chapter(3L)

        val items = buildPaginatedItems(
            EntryChildWindow(current, previous, next),
            mapOf(current.id to listOf(page(current))),
        )

        assertEquals(3, items.size)
        assertIs<ProsePagerItem.Transition>(items[0])
        assertEquals(current.id, assertIs<ProsePagerItem.Page>(items[1]).page.chapter.id)
        assertIs<ProsePagerItem.Transition>(items[2])
    }

    @Test
    fun `inserting previous pages preserves the settled transition key`() {
        val previous = chapter(1L)
        val current = chapter(2L)
        val window = EntryChildWindow(current, previous, null)
        val beforeLoading = buildPaginatedItems(
            window,
            mapOf(current.id to listOf(page(current))),
        )
        val afterLoading = buildPaginatedItems(
            window,
            mapOf(
                previous.id to listOf(page(previous, index = 0, total = 2), page(previous, index = 1, total = 2)),
                current.id to listOf(page(current)),
            ),
        )

        assertEquals(
            2,
            prosePagerDatasetAnchor(
                previousItemKeys = beforeLoading.map(ProsePagerItem::key),
                items = afterLoading,
                settledPage = 0,
            ),
        )
    }

    @Test
    fun `paginated mode starts from the live reader progression`() {
        val previous = chapter(1L)
        val current = chapter(2L)
        val items = listOf(
            ProsePagerItem.Page(page(previous)),
            ProsePagerItem.Page(page(current, index = 0, total = 5)),
            ProsePagerItem.Page(page(current, index = 1, total = 5)),
            ProsePagerItem.Page(page(current, index = 2, total = 5)),
            ProsePagerItem.Page(page(current, index = 3, total = 5)),
            ProsePagerItem.Page(page(current, index = 4, total = 5)),
        )

        assertEquals(4, initialPaginatedItemIndex(items, current.id, progression = 0.6f))
    }

    @Test
    fun `paginated mode restores the page containing a scrolling position`() {
        val current = chapter(2L)
        val items = listOf(
            ProsePagerItem.Page(page(current, index = 0, total = 3)),
            ProsePagerItem.Page(page(current, index = 1, total = 3)),
            ProsePagerItem.Page(page(current, index = 2, total = 3)),
        )

        assertEquals(
            1,
            initialPaginatedItemIndex(
                items = items,
                chapterId = current.id,
                progression = 0.38f,
                sourceOffset = 114,
            ),
        )
    }

    @Test
    fun `structured block scroll maps to a durable document offset`() {
        assertEquals(0, structuredBlockPositionOffset(100, scrollValue = 0, maxScrollValue = 800))
        assertEquals(50, structuredBlockPositionOffset(100, scrollValue = 400, maxScrollValue = 800))
        assertEquals(100, structuredBlockPositionOffset(100, scrollValue = 800, maxScrollValue = 800))
        assertEquals(400, structuredBlockScrollValue(50, blockLength = 100, maxScrollValue = 800))
    }

    @Test
    fun `structured block without inner scrolling records only fully visible content as complete`() {
        assertEquals(100, structuredBlockPositionOffset(100, scrollValue = 0, maxScrollValue = 0))
        assertEquals(
            0,
            structuredBlockPositionOffset(
                blockLength = 100,
                scrollValue = 0,
                maxScrollValue = 0,
                contentFullyVisible = false,
            ),
        )
        assertEquals(0, structuredBlockScrollValue(50, blockLength = 100, maxScrollValue = 0))
    }

    private fun chapter(id: Long) = EntryChapter.create().copy(id = id, entryId = 9L, name = "Chapter $id")

    private fun page(chapter: EntryChapter, index: Int = 0, total: Int = 1) =
        HtmlProsePage(
            chapter = chapter,
            index = index,
            total = total,
            text = SpannableString("Text"),
            progression = if (total <= 1) 1f else index.toFloat() / (total - 1),
            sourceStart = index * 100,
            sourceEndExclusive = (index + 1) * 100,
        )
}
