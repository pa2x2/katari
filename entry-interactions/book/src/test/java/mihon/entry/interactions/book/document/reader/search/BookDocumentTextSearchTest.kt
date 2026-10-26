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

    @Test
    fun `an excerpt shows a figure's placeholder in the chapter text as a space, not a box`() {
        val text = "The storm broke.\n\n\uFFFC\n\nShe ran for the bridge."
        val match = bookDocumentTextMatches(text, "bridge").single()

        val excerpt = bookDocumentSearchExcerpt(text, match)

        assertEquals("The storm broke.     She ran for the bridge.", excerpt.text)
        assertEquals("bridge", excerpt.text.substring(excerpt.matchStart, excerpt.matchEnd))
    }
}
