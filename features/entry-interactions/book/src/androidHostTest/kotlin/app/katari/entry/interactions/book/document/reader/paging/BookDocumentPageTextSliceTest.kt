package app.katari.entry.interactions.book.document.reader.paging

import app.katari.book.api.document.BookDocumentBlock
import app.katari.book.api.document.BookDocumentBlockContent
import app.katari.book.api.document.BookDocumentBlockId
import app.katari.book.api.document.BookDocumentBlockKind
import app.katari.book.api.document.BookDocumentBlockRole
import app.katari.book.api.document.BookDocumentFlowStyle
import app.katari.book.api.document.BookDocumentInlineStyle
import app.katari.book.api.document.BookDocumentInlineStyleRange
import app.katari.book.api.document.BookDocumentLink
import app.katari.book.api.document.BookDocumentLinkTarget
import app.katari.book.api.document.BookDocumentRichText
import app.katari.book.api.document.BookDocumentStyle
import app.katari.book.api.document.BookDocumentTextDirection
import app.katari.book.api.document.BookDocumentTextRange
import org.junit.Test
import kotlin.test.assertEquals

class BookDocumentPageTextSliceTest {
    @Test
    fun continued_paragraph_keeps_link_style_and_language_ranges_without_repeating_the_indent() {
        val text = "before linked words after"
        val link = BookDocumentLink(7, 19, BookDocumentLinkTarget.Anchor("note"))
        val emphasis = BookDocumentInlineStyleRange(7, 19, BookDocumentInlineStyle(bold = true))
        val block = BookDocumentBlock(
            id = BookDocumentBlockId("paragraph"),
            role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
            content = BookDocumentBlockContent.Text(
                BookDocumentRichText(text, BookDocumentTextRange(0, text.length), listOf(link), listOf(emphasis)),
            ),
            plainText = text,
            sourceFragments = listOf("paragraph-anchor"),
            style = BookDocumentStyle(
                flow = BookDocumentFlowStyle(
                    firstLineIndentEm = 2f,
                    spacingBeforeEm = 1f,
                    lineHeightScale = 1.8f,
                    direction = BookDocumentTextDirection.RIGHT_TO_LEFT,
                    languageTag = "ar",
                ),
            ),
            logicalStart = 5,
            logicalEndExclusive = 5 + text.length,
        )
        val continued = block.pageTextSlice("head $text", 10, text.length)
        assertEquals("ked words after", (continued.content as BookDocumentBlockContent.Text).value.text)
        assertEquals(listOf(link.copy(start = 0, endExclusive = 9)), continued.links)
        assertEquals(listOf(emphasis.copy(start = 0, endExclusive = 9)), continued.inlineStyles)
        assertEquals(15, continued.logicalStart)
        assertEquals(block.id, continued.id)
        assertEquals(block.style.flow.copy(firstLineIndentEm = 0f, spacingBeforeEm = 0f), continued.style.flow)
    }
}
