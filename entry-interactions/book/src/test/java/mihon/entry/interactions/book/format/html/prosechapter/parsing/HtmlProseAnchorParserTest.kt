package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HtmlProseAnchorParserTest {
    @Test
    fun `empty named anchors survive whitespace and empty containers before headings`() {
        val document = HtmlProseDocumentParser().parse(
            "chapter",
            null,
            HtmlProseSanitizer.sanitize(
                """<p>Previous</p><a id="etymology"></a><div><br><span id="page"></span></div>
                <h2>Etymology</h2><p>Words</p><a name="end"></a>""".encodeToByteArray(),
            ),
        )
        assertEquals(document.blocks[1].id, document.anchors.getValue("etymology").blockId)
        assertEquals(document.blocks[1].id, document.anchors.getValue("page").blockId)
        assertEquals(0, document.anchors.getValue("etymology").offsetWithinBlock)
        assertEquals(document.blocks.last().id, document.anchors.getValue("end").blockId)
        assertEquals(5, document.anchors.getValue("end").offsetWithinBlock)
    }
}
