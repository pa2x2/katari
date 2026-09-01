package mihon.book.api.document

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class BookDocumentStyleCompatibilityTest {

    @Test
    fun `document flow properties survive copying and serialization`() {
        val style = BookDocumentStyle().withFlow(
            BookDocumentFlowStyle(
                spacingBeforeEm = 1f,
                spacingAfterEm = 2f,
                lineHeightScale = 1.5f,
                firstLineIndentEm = 0.75f,
                direction = BookDocumentTextDirection.RIGHT_TO_LEFT,
                languageTag = "ar",
            ),
        )

        val copied = style.copy(whiteSpace = BookDocumentWhiteSpace.PRE).withFlow(style.flow)
        val restored = Json.decodeFromString<BookDocumentStyle>(Json.encodeToString(copied))

        assertEquals(copied, restored)
        assertEquals(1f, restored.spacingBeforeEm)
        assertEquals(2f, restored.spacingAfterEm)
        assertEquals(BookDocumentTextDirection.RIGHT_TO_LEFT, restored.direction)
        assertEquals("ar", restored.languageTag)
    }

    @Test
    fun `inline text context survives serialization without visual styling`() {
        val style = BookDocumentInlineStyle.withTextContext(
            base = BookDocumentInlineStyle(),
            textContext = BookDocumentTextContext(
                languageTag = "he",
                direction = BookDocumentTextDirection.RIGHT_TO_LEFT,
            ),
        )
        val range = BookDocumentInlineStyleRange(0, 1, style)

        val restored = Json.decodeFromString<BookDocumentInlineStyleRange>(Json.encodeToString(range))

        assertEquals(range, restored)
        assertEquals("he", restored.style.languageTag)
        assertEquals(BookDocumentTextDirection.RIGHT_TO_LEFT, restored.style.direction)
    }
}
