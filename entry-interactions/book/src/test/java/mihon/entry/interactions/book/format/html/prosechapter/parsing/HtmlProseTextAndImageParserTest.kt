package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HtmlProseTextAndImageParserTest {
    @Test
    fun `illustrated chapter headings preserve image text and leading navigation anchors`() {
        val document = HtmlProseDocumentParser().parse(
            "chapter",
            null,
            HtmlProseSanitizer.sanitize(
                "<h2 id='title'><a id='chapter-one'></a><img src='chapter.jpg' alt=''/><br/>Chapter I.</h2>"
                    .encodeToByteArray(),
            ),
        )
        val image = document.blocks[0]
        val heading = document.blocks[1]
        assertEquals("chapter.jpg", assertIs<BookDocumentBlockContent.Figure>(image.content).image.resourceId)
        assertEquals(BookDocumentBlockKind.HEADING, heading.role.kind)
        assertEquals(2, heading.role.level)
        assertEquals("Chapter I.", heading.plainText)
        assertEquals(image.id, document.anchors.getValue("title").blockId)
        assertEquals(image.id, document.anchors.getValue("chapter-one").blockId)
    }
}
