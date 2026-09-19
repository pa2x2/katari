package mihon.entry.interactions.book.document.reader.paging

import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.book.document.reader.BookDocumentViewerFixture
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.viewer.EntryChildTransition
import org.junit.Test
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals

internal class BookDocumentPaginationWindowTest : BookDocumentViewerFixture() {
    private fun boundaryItem(from: EntryChapter, to: EntryChapter?) = BookDocumentViewerItem.Transition(
        EntryChildTransition.Next(from, to),
        "boundary:${from.id}->${to?.id ?: "terminal"}",
    )

    private fun numberedChapter(id: Long, number: Double): EntryChapter =
        chapter(id).copy(chapterNumber = number)

    private fun contentItem(chapter: EntryChapter, text: String): BookDocumentViewerItem.Block<EntryChapter> {
        val section = chapterSection(chapter, listOf(text))
        return BookDocumentViewerItem.Block(section, section.document.blocks.single())
    }

    private fun describe(item: BookDocumentViewerItem<EntryChapter>): String = when (item) {
        is BookDocumentViewerItem.Block -> "content:${item.section.owner.id}"
        is BookDocumentViewerItem.Transition ->
            "boundary:${item.transition.from.id}->${item.transition.to?.id ?: "terminal"}"
    }

    @Test
    fun `a hidden boundary with a prepared destination does not spend a page between the chapters`() {
        val first = chapter(1)
        val second = chapter(2)
        val items = listOf(
            contentItem(first, "one"),
            boundaryItem(first, second),
            contentItem(second, "two"),
        )

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.HIDDEN,
            setOf(first.id, second.id),
            { null },
        )

        assertEquals(
            listOf("content:1", "content:2"),
            filtered.map(::describe),
        )
    }

    @Test
    fun `a hidden boundary keeps its page across a chapter gap`() {
        val first = numberedChapter(1, 1.0)
        val third = numberedChapter(3, 3.0)
        val boundary = boundaryItem(first, third)
        val items = listOf(contentItem(first, "one"), boundary, contentItem(third, "three"))

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.HIDDEN,
            setOf(first.id, third.id),
            { null },
        )

        assertEquals(listOf("content:1", "boundary:1->3", "content:3"), filtered.map(::describe))
    }

    @Test
    fun `a hidden boundary keeps its page while its destination prepares`() {
        val first = chapter(1)
        val second = chapter(2)
        val boundary = boundaryItem(first, second)
        val items = listOf(contentItem(first, "one"), boundary, contentItem(second, "two"))

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.HIDDEN,
            setOf(first.id),
            { chapterId -> if (chapterId == second.id) BookDocumentChapterLoadState.Loading else null },
        )

        assertEquals(listOf("content:1", "boundary:1->2", "content:2"), filtered.map(::describe))
    }

    @Test
    fun `a hidden boundary keeps its page when its destination is unrequested`() {
        val first = chapter(1)
        val second = chapter(2)
        val boundary = boundaryItem(first, second)
        val items = listOf(contentItem(first, "one"), boundary, contentItem(second, "two"))

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.HIDDEN,
            setOf(first.id),
            { null },
        )

        assertEquals(listOf("content:1", "boundary:1->2", "content:2"), filtered.map(::describe))
    }

    @Test
    fun `a hidden boundary keeps its page when its destination failed to prepare`() {
        val first = chapter(1)
        val second = chapter(2)
        val boundary = boundaryItem(first, second)
        val items = listOf(contentItem(first, "one"), boundary, contentItem(second, "two"))

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.HIDDEN,
            setOf(first.id),
            { chapterId -> if (chapterId == second.id) BookDocumentChapterLoadState.Failed("broken") else null },
        )

        assertEquals(listOf("content:1", "boundary:1->2", "content:2"), filtered.map(::describe))
    }

    @Test
    fun `a terminal hidden boundary keeps its page`() {
        val last = chapter(2)
        val boundary = boundaryItem(last, null)
        val items = listOf(contentItem(last, "end"), boundary)

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.HIDDEN,
            setOf(last.id),
            { null },
        )

        assertEquals(listOf("content:2", "boundary:2->terminal"), filtered.map(::describe))
    }

    @Test
    fun `always mode never drops prepared boundary pages`() {
        val first = chapter(1)
        val second = chapter(2)
        val boundary = boundaryItem(first, second)
        val items = listOf(contentItem(first, "one"), boundary, contentItem(second, "two"))

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.ALWAYS,
            setOf(first.id, second.id),
            { null },
        )
        assertEquals(listOf("content:1", "boundary:1->2", "content:2"), filtered.map(::describe))
    }

    @Test
    fun `a when-needed boundary with a prepared contiguous destination does not spend a page`() {
        val first = numberedChapter(1, 1.0)
        val second = numberedChapter(2, 2.0)
        val items = listOf(
            contentItem(first, "one"),
            boundaryItem(first, second),
            contentItem(second, "two"),
        )

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.WHEN_NEEDED,
            setOf(first.id, second.id),
            { null },
        )

        assertEquals(listOf("content:1", "content:2"), filtered.map(::describe))
    }

    @Test
    fun `a when-needed previous boundary with a prepared contiguous destination does not spend a page`() {
        val first = numberedChapter(1, 1.0)
        val second = numberedChapter(2, 2.0)
        val boundary = BookDocumentViewerItem.Transition(
            EntryChildTransition.Prev(second, first),
            "boundary:2->1",
        )
        val items = listOf(contentItem(first, "one"), boundary, contentItem(second, "two"))

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.WHEN_NEEDED,
            setOf(first.id, second.id),
            { null },
        )

        assertEquals(listOf("content:1", "content:2"), filtered.map(::describe))
    }

    @Test
    fun `a when-needed boundary keeps its page when its destination is unprepared`() {
        val first = numberedChapter(1, 1.0)
        val second = numberedChapter(2, 2.0)
        val boundary = boundaryItem(first, second)
        val items = listOf(contentItem(first, "one"), boundary, contentItem(second, "two"))

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.WHEN_NEEDED,
            setOf(first.id),
            { null },
        )

        assertEquals(listOf("content:1", "boundary:1->2", "content:2"), filtered.map(::describe))
    }

    @Test
    fun `a when-needed boundary keeps its page when its destination failed`() {
        val first = numberedChapter(1, 1.0)
        val second = numberedChapter(2, 2.0)
        val boundary = boundaryItem(first, second)
        val items = listOf(contentItem(first, "one"), boundary, contentItem(second, "two"))

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.WHEN_NEEDED,
            setOf(first.id, second.id),
            { chapterId -> if (chapterId == second.id) BookDocumentChapterLoadState.Failed("broken") else null },
        )

        assertEquals(listOf("content:1", "boundary:1->2", "content:2"), filtered.map(::describe))
    }

    @Test
    fun `a when-needed boundary keeps its page across a chapter gap`() {
        val first = numberedChapter(1, 1.0)
        val third = numberedChapter(3, 3.0)
        val boundary = boundaryItem(first, third)
        val items = listOf(contentItem(first, "one"), boundary, contentItem(third, "three"))

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.WHEN_NEEDED,
            setOf(first.id, third.id),
            { null },
        )

        assertEquals(listOf("content:1", "boundary:1->3", "content:3"), filtered.map(::describe))
    }

    @Test
    fun `a terminal when-needed boundary keeps its page`() {
        val last = numberedChapter(2, 2.0)
        val boundary = boundaryItem(last, null)
        val items = listOf(contentItem(last, "end"), boundary)

        val filtered = items.withoutResolvedBoundaries(
            ChapterTransitionMode.WHEN_NEEDED,
            setOf(last.id),
            { null },
        )

        assertEquals(listOf("content:2", "boundary:2->terminal"), filtered.map(::describe))
    }
}
