package mihon.entry.interactions.book.epub

import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookReadingDirection
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReadiumEpubNavigationTest {

    @Test
    fun `progression seek drops precise locator anchors`() {
        val locator = Locator(
            href = checkNotNull(Url("chapter.xhtml#old-anchor")),
            mediaType = MediaType.XHTML,
            title = "Chapter",
            locations = Locator.Locations(
                fragments = listOf("old-anchor"),
                progression = 0.1,
                position = 4,
                totalProgression = 0.2,
                otherLocations = mapOf("cssSelector" to "#old-anchor"),
            ),
            text = Locator.Text(before = "before", highlight = "old text", after = "after"),
        )

        val result = locator.progressionOnly(0.75)

        assertEquals("chapter.xhtml", result.href.toString())
        assertEquals(0.75, result.locations.progression)
        assertTrue(result.locations.fragments.isEmpty())
        assertNull(result.locations.position)
        assertNull(result.locations.totalProgression)
        assertTrue(result.locations.otherLocations.isEmpty())
        assertEquals(Locator.Text(), result.text)
    }

    @Test
    fun `physical page indices are mirrored for effective RTL`() {
        assertEquals(0, physicalPageIndexToLogical(0, 4, BookReadingDirection.LEFT_TO_RIGHT))
        assertEquals(3, physicalPageIndexToLogical(0, 4, BookReadingDirection.RIGHT_TO_LEFT))
        assertEquals(0, physicalPageIndexToLogical(3, 4, BookReadingDirection.RIGHT_TO_LEFT))
        assertEquals(
            BookReadingDirection.RIGHT_TO_LEFT,
            ReadingProgression.RTL.toBookReadingDirection(),
        )
    }

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
