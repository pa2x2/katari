package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HtmlProseTableParserTest {
    @Test
    fun `long tables retain their cells links and late row anchor positions`() {
        val rows = (1..400).joinToString("") {
            "<tr id='row-$it'><td><a href='#chapter-$it'>Chapter $it</a></td><td>$it</td></tr>"
        }
        val document = parse("<table>$rows</table><h2 id='chapter-400'>Last chapter</h2>")
        val block = document.blocks.first()
        val table = assertIs<BookDocumentBlockContent.Table>(block.content)
        assertEquals(400, table.rows.size)
        assertEquals(2, table.columnCount)
        assertEquals("400", table.rows.last().cells.last().content.text)
        assertEquals(BookDocumentLinkTarget.Anchor("chapter-400"), block.links.last().target)
        val position = document.anchors.getValue("row-400")
        assertEquals(block.id, position.blockId)
        assertTrue(document.content.text.substring(position.offsetWithinBlock).startsWith("Chapter 400"))
    }

    @Test
    fun `tables still respect the shared document semantic budget`() {
        val rows = "<tr><td>Cell</td></tr>".repeat(HtmlProseChapterContract.MAX_BLOCKS + 1)
        assertFailsWith<HtmlProseLimitExceededException> { parse("<table>$rows</table>") }
    }

    private fun parse(html: String) = HtmlProseDocumentParser().parse(
        "chapter",
        null,
        HtmlProseSanitizer.sanitize(html.encodeToByteArray()),
    )
}
