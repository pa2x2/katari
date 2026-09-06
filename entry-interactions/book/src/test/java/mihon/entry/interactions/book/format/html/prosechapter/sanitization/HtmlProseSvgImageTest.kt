package mihon.entry.interactions.book.format.html.prosechapter.sanitization

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HtmlProseSvgImageTest {
    @Test
    fun `Gutenberg SVG cover resolves its image through publication policy and preserves its anchor`() {
        val body =
            sanitize(
                """<svg id="cover" viewBox="0 0 780 1227" width="100%" height="100%">
            <title>Cover</title><image width="780" height="1227" xlink:href="cover.jpg"/></svg>""",
            )
        val image = body.selectFirst("img")!!
        assertEquals("OPS/cover.jpg", image.attr("src"))
        assertEquals("Cover", image.attr("alt"))
        assertEquals("cover", image.id())
    }

    @Test
    fun `vector artwork and transformed images keep their SVG representation`() {
        for (content in listOf(
            "<rect width='780' height='1227'/>",
            "<image width='780' height='1227' href='cover.jpg' transform='rotate(90)'/>",
            "<image width='100' height='100' href='cover.jpg'/>",
        )) {
            val body = sanitize("<svg viewBox='0 0 780 1227'>$content</svg>")
            assertEquals("derived.svg", body.selectFirst("img")!!.attr("src"))
        }
    }

    private fun sanitize(svg: String) = HtmlProseSanitizer.sanitize(
        "<html><body>$svg</body></html>".encodeToByteArray(),
        HtmlProseSanitizationPolicy(
            xmlSyntax = true,
            resolveImageResource = { if (it == "cover.jpg") "OPS/cover.jpg" else null },
            resolveInlineImage = { "derived.svg" },
        ),
    )
}
