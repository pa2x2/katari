package mihon.entry.interactions.book.document.reader.navigation

import mihon.book.api.BookLocator
import mihon.book.api.document.locatorAt
import mihon.entry.interactions.book.document.preparation.preparedDocumentPublication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BookDocumentNavigationDestinationTest {
    @Test
    fun `missing fragments fail while precise locators restore their exact offset only on request`() {
        val publication =
            preparedDocumentPublication("text" to "<p id='passage'>A long enough passage to seek within.</p>")
        val document = publication.documents.single()
        assertNull(publication.resolveNavigationDestination(BookLocator("missing"), false))
        assertNull(publication.resolveNavigationDestination(BookLocator("text", fragments = listOf("missing")), false))
        assertEquals(
            document.positionAtProgression(0f),
            publication.resolveNavigationDestination(BookLocator("text"), false)?.position,
        )

        val position = document.positionAtProgression(.6f)
        val locator = document.locatorAt(position)
        assertEquals(
            position,
            publication.resolveNavigationDestination(locator, false, restorePosition = true)?.position,
        )
        assertEquals(
            document.positionAtProgression(0f),
            publication.resolveNavigationDestination(locator, false)?.position,
        )
    }
}
