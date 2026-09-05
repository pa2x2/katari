package mihon.entry.interactions.book.format.html.prosechapter.sanitization

import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class HtmlProseReferenceLinksTest {
    @Test
    fun `Gutenberg paired note IDs become previews while ordinary chapter links remain navigation`() {
        // The Brothers Karamazov (Gutenberg 28054) uses these paired IDs without epub:type=noteref.
        val html = """
            <p>I am strong,<a href="notes.xhtml#fn-1" id="fnref-1"><sup>[1]</sup></a></p>
            <p><a href="notes.xhtml#contents" id="fnref-2">Contents</a></p>
        """.trimIndent()
        val body = HtmlProseSanitizer.sanitize(
            html.encodeToByteArray(),
            HtmlProseSanitizationPolicy(resolveLink = { href ->
                BookDocumentLinkTarget.Resource(href.substringBefore('#'), href.substringAfter('#'))
            }),
        )
        val document = HtmlProseDocumentParser().parse("chapter", null, body)
        assertEquals(
            listOf(
                BookDocumentLinkTarget.Reference("notes.xhtml", "fn-1"),
                BookDocumentLinkTarget.Resource("notes.xhtml", "contents"),
            ),
            document.blocks.flatMap { it.links }.map { it.target },
        )
    }
}
