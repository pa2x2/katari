package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class HtmlProseDocumentParserTest {
    @Test
    fun `hostile chapters beyond the canonical grid or text extent are rejected`() {
        val wideTable = "<table><tr>${(1..25).joinToString("") { "<td>$it</td>" }}</tr></table>"
        val longText = "<p>${"a".repeat(HtmlProseChapterContract.MAX_CANONICAL_UTF16)}</p>"

        listOf(wideTable, longText).forEach { html ->
            assertFailsWith<HtmlProseLimitExceededException> {
                HtmlProseDocumentParser().parse("chapter", null, HtmlProseSanitizer.sanitize(html.encodeToByteArray()))
            }
        }
    }
}
