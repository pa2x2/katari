package mihon.entry.interactions.book.document.reader.navigation

import mihon.book.api.BookLocator
import mihon.entry.interactions.book.document.preparation.preparedDocumentPublication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BookDocumentNavigationDestinationTest {
    @Test
    fun `missing explicit fragments fail instead of navigating to a resource start`() {
        val publication = preparedDocumentPublication("text" to "<p id='exists'>Actual target.</p>")
        assertNull(publication.resolveNavigationDestination(BookLocator("missing"), false))
        assertNull(publication.resolveNavigationDestination(BookLocator("text", fragments = listOf("missing")), false))
        val start = publication.resolveNavigationDestination(BookLocator("text"), false)!!
        assertEquals(publication.documents.single().positionAtProgression(0f), start.position)
    }
}
