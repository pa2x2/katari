package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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

    private fun parse(html: String) = HtmlProseDocumentParser().parse(
        "chapter",
        null,
        HtmlProseSanitizer.sanitize(html.encodeToByteArray()),
    )
}
