package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class BookDocumentBidiSelectionTest {
    @Test
    fun `inline direction preserves link ranges and copied text without inserting controls into the model`() {
        val html = "<p>Before \u2066authored\u2069 " +
            "<a href='#note'><span dir='rtl'>(123)</span></a> after.</p>"
        val document = HtmlProseDocumentParser().parse(
            "page",
            null,
            HtmlProseSanitizer.sanitize(
                html.encodeToByteArray(),
            ),
        )
        val block = document.blocks.single()
        val text = document.content.text.trimEnd('\n')
        val presentation = bookDocumentTextPresentation(text, block.inlineStyles)
        val annotated = presentation.toSelectableAnnotatedString(
            fonts = emptyMap(),
            links = block.links,
            inlineStyles = block.inlineStyles,
            token = "text",
            baseFontSize = 18f,
            linkColor = Color.Blue,
            onLinkClick = {},
        )
        val link = annotated.getLinkAnnotations(0, annotated.length).single()
        assertEquals("(123)", annotated.subSequence(link.start, link.end).text)
        assertTrue('\u2067' in annotated.text)
        val selected = projectBookDocumentSelection(
            "owner",
            listOf(annotated),
            mapOf(
                "text" to BookDocumentSelectableLeaf(
                    token = "text",
                    chapterId = 1,
                    fullText = presentation.text,
                    separatorAfter = "",
                    insertedBidiOffsets = presentation.insertedOffsets,
                ),
            ),
            emptyMap(),
            Offset.Zero,
        )!!
        assertEquals(text, selected.text)
        assertEquals(text, selected.languageContextText)
        assertEquals("Before \u2066authored\u2069 (123) after.", text)
    }
}
