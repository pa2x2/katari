package mihon.entry.interactions.book.format.html.prosechapter.sanitization

import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class HtmlProseSanitizerTest {
    @Test
    fun `active content is removed and supported CSS is projected by cascade precedence`() {
        val body = HtmlProseSanitizer.sanitize(
            """
            <html><head><style>
              p { color: red; text-align: center }
              .chapter { color: blue }
              #opening { color: #102030 }
            </style></head><body>
              <script>alert('no')</script>
              <p id="opening" class="chapter" style="text-align: right" onclick="evil()">Hello</p>
              <video src="remote.mp4"></video>
            </body></html>
            """.trimIndent().encodeToByteArray(),
        )

        val paragraph = body.selectFirst("p")!!
        assertEquals("#102030", paragraph.attr("data-katari-style-color"))
        assertEquals("right", paragraph.attr("data-katari-style-text-align"))
        assertNull(body.selectFirst("script"))
        assertEquals("", paragraph.attr("onclick"))
        assertEquals("video", body.selectFirst("[data-katari-unsupported]")!!.attr("data-katari-unsupported"))
    }

    @Test
    fun `supported CSS rule count is bounded before application`() {
        val css = (1..257).joinToString("\n") { index -> ".rule$index { color: red }" }

        assertFailsWith<HtmlProseLimitExceededException> {
            HtmlProseSanitizer.sanitize("<style>$css</style><p>Text</p>".encodeToByteArray())
        }
    }
}
