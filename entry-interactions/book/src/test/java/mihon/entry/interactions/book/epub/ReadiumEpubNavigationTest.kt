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
        val progressions = navigation.associate { row ->
            row.item.target.navigationKey() to when (row.item.title) {
                "Chapter 9" -> 0.1
                "Chapter 10" -> 0.2
                else -> 0.8
            }
        }

        val result = resolveSectionMetrics(
            navigation = navigation,
            locator = BookLocator(resourceId = "book.xhtml", progression = 0.25),
            resolvedProgressions = progressions,
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
            resolvedProgressions = emptyMap(),
        )

        assertNull(result)
    }

    private fun item(title: String, resourceId: String, fragment: String) = BookNavigationItem(
        title = title,
        target = BookLocator(resourceId = resourceId, fragments = listOf(fragment)),
    )
}
