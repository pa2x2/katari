package mihon.entry.interactions.book.document.reader.paging

import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockId
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentFlowStyle
import mihon.book.api.document.BookDocumentInlineStyle
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentLink
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentTextDirection
import mihon.book.api.document.BookDocumentTextRange
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
            style = BookDocumentStyle().withFlow(
                BookDocumentFlowStyle(
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
