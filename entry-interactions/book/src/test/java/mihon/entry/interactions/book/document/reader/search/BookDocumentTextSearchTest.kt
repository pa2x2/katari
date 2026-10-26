package mihon.entry.interactions.book.document.reader.search

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class BookDocumentTextSearchTest {
    @Test
    fun `matches ignoring case and accents cover the source text, including accents written as separate marks`() {
        // "Café" with its accent as a combining mark, then capitals with a precomposed É, then a no-break space.
        val text = "Café au lait, CAFÉ NOIR"

        val matches = bookDocumentTextMatches(text, "cafe")
        val spaced = bookDocumentTextMatches(text, "café noir")

        assertEquals(listOf("Café", "CAFÉ"), matches.map { text.substring(it.start, it.endExclusive) })
        assertEquals(listOf("CAFÉ NOIR"), spaced.map { text.substring(it.start, it.endExclusive) })
    }
}
