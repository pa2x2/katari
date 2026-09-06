package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HtmlProseQuoteParserTest {
    @Test
    fun `quoted contents retain independent paragraphs and link destinations`() {
        // Gutenberg Moby Dick uses blockquote div p for its 135 chapter links.
        val entries = (1..135).joinToString("") { "<p><a href='#chapter$it'>CHAPTER $it.</a></p>" }
        val document = parse("<blockquote id='contents'><div>$entries</div></blockquote><p id='chapter1'>Loomings.</p>")
        val contents = document.blocks.dropLast(1)
        assertEquals((1..135).map { "CHAPTER $it." }, contents.map { it.plainText })
        contents.forEachIndexed { index, block ->
            assertEquals(BookDocumentBlockKind.QUOTE, block.role.kind)
            assertEquals(BookDocumentLinkTarget.Anchor("chapter${index + 1}"), block.links.single().target)
        }
        assertEquals(contents.first().id, document.anchors.getValue("contents").blockId)
        assertEquals(document.blocks.last().id, document.anchors.getValue("chapter1").blockId)
    }

    @Test
    fun `quotes preserve nested media tables headings and paragraph styling`() {
        val document = parse(
            """<blockquote><h2>Extracts</h2><p><i>First</i></p><blockquote><p>Nested</p></blockquote>
                <img src='whale.png' alt='Whale'><table><tr><td>CETUS</td><td>Latin</td></tr></table></blockquote>""",
        )
        assertEquals(
            listOf(
                BookDocumentBlockKind.HEADING,
                BookDocumentBlockKind.QUOTE,
                BookDocumentBlockKind.QUOTE,
                BookDocumentBlockKind.FIGURE,
                BookDocumentBlockKind.TABLE,
            ),
            document.blocks.map { it.role.kind },
        )
        assertEquals(true, document.blocks[1].inlineStyles.single().style.italic)
        assertIs<BookDocumentBlockContent.Figure>(document.blocks[3].content)
    }

    private fun parse(html: String) = HtmlProseDocumentParser().parse(
        "chapter",
        null,
        HtmlProseSanitizer.sanitize(html.encodeToByteArray()),
    )
}
