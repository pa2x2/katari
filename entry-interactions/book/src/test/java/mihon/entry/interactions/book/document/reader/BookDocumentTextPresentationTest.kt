package mihon.entry.interactions.book.document.reader

import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import java.text.Bidi
import kotlin.test.assertEquals

internal class BookDocumentTextPresentationTest {
    @Test
    fun `nested inline direction survives an explicit line break`() {
        val html = "<p>Before <span dir='rtl'>(123)<br/>" +
            "<span dir='ltr'>Latin</span> (456)</span> after.</p>"
        val document = HtmlProseDocumentParser().parse(
            "page",
            null,
            HtmlProseSanitizer.sanitize(
                html.encodeToByteArray(),
            ),
        )
        val presentation = bookDocumentTextPresentation(
            document.content.text.trimEnd('\n'),
            document.blocks.single().inlineStyles,
        )
        val bidi = Bidi(presentation.text, Bidi.DIRECTION_LEFT_TO_RIGHT)
        assertEquals(1, bidi.getLevelAt(presentation.text.indexOf("(123)")) % 2)
        assertEquals(1, bidi.getLevelAt(presentation.text.indexOf("(456)")) % 2)
        assertEquals(0, bidi.getLevelAt(presentation.text.indexOf("Latin")) % 2)
    }
}
