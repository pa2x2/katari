package mihon.entry.interactions.book.epub

import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReadiumEpubNavigationTest {

    @Test
    fun `resolves toc section within a shared reading-order resource`() {
        val navigation = listOf(
            item("Chapter 9", "book.xhtml", "chapter-9"),
            item("Chapter 10", "book.xhtml", "chapter-10"),
            item("Chapter 22", "book.xhtml", "chapter-22"),
        ).flattenNavigation()
        val positions = navigation.associate { row ->
            row.item.target.navigationKey() to ReadiumNavigationPosition(
                progression = when (row.item.title) {
                    "Chapter 9" -> 0.1
                    "Chapter 10" -> 0.2
                    else -> 0.8
                },
                pageIndex = null,
            )
        }

        val result = resolveSectionMetrics(
            navigation = navigation,
            locator = BookLocator(resourceId = "book.xhtml", progression = 0.25),
            resolvedPositions = positions,
        )

        assertEquals(1, result?.index)
        assertEquals(0.2, result?.startProgression)
        assertEquals(0.8, result?.endProgression)
    }

    @Test
    fun `resource without a resolvable toc target has no guessed boundary`() {
        val navigation = listOf(item("Chapter", "book.xhtml", "missing")).flattenNavigation()

        val result = resolveSectionMetrics(
            navigation = navigation,
            locator = BookLocator(resourceId = "book.xhtml", progression = 0.5),
            resolvedPositions = emptyMap(),
        )

        assertNull(result)
    }

    @Test
    fun `paginated section follows the visible page instead of its starting progression`() {
        val navigation = listOf(
            item("Chapter 9", "book.xhtml", "chapter-9"),
            item("Chapter 10", "book.xhtml", "chapter-10"),
            item("Chapter 11", "book.xhtml", "chapter-11"),
        ).flattenNavigation()
        val positions = mapOf(
            navigation[0].item.target.navigationKey() to ReadiumNavigationPosition(0.1, pageIndex = 1),
            navigation[1].item.target.navigationKey() to ReadiumNavigationPosition(0.24, pageIndex = 4),
            navigation[2].item.target.navigationKey() to ReadiumNavigationPosition(0.5, pageIndex = 8),
        )

        val result = resolvePaginatedSectionMetrics(
            navigation = navigation,
            locator = BookLocator(resourceId = "book.xhtml", progression = 0.2),
            resolvedPositions = positions,
            currentPageIndex = 4,
            totalPages = 16,
        )

        assertEquals(1, result?.index)
        assertEquals(4, result?.startPageIndex)
        assertEquals(8, result?.endPageIndex)
    }

    @Test
    fun `paginated section keeps a visible one-page range`() {
        val navigation = listOf(
            item("Chapter 131", "book.xhtml", "chapter-131"),
            item("Chapter 132", "book.xhtml", "chapter-132"),
        ).flattenNavigation()
        val positions = mapOf(
            navigation[0].item.target.navigationKey() to ReadiumNavigationPosition(0.8, pageIndex = 12),
            navigation[1].item.target.navigationKey() to ReadiumNavigationPosition(0.86, pageIndex = 13),
        )

        val result = resolvePaginatedSectionMetrics(
            navigation = navigation,
            locator = BookLocator(resourceId = "book.xhtml", progression = 0.78),
            resolvedPositions = positions,
            currentPageIndex = 12,
            totalPages = 16,
        )

        assertEquals(12, result?.startPageIndex)
        assertEquals(13, result?.endPageIndex)
    }

    private fun item(title: String, resourceId: String, fragment: String) = BookNavigationItem(
        title = title,
        target = BookLocator(resourceId = resourceId, fragments = listOf(fragment)),
    )
}
