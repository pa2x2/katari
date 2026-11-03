package mihon.entry.interactions.book.format.html.prosechapter.css

import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HtmlProseCssTest {
    @Test
    fun `a novel site's style sheet resolves on each element as a browser resolves it`() {
        // The patterns of translators' system boxes: variables, shorthands, descendant and child selectors,
        // !important against a more specific rule, and a narrow-screen media query.
        val css = """
            :root { --box-bg: #000; --glow: 0 0 10px rgba(36, 79, 255, .8); }
            .red-box { background: var(--box-bg) linear-gradient(#111, #000); border: 2px solid red;
                       border-radius: 8px 4px; padding: 12px 1em; box-shadow: var(--glow); }
            .red-box p { color: hsl(0 0% 100%); margin: 0 auto }
            .red-box > .title { color: #ffa970 !important; font: italic bold 1.2em/1.5 "Oswald", serif; }
            div.red-box p.title { color: blue; }
            p:first-child + p { text-decoration: underline wavy red; }
            .status { border-left: 4px ridge var(--missing, #ff751a); }
            @media (max-width: 600px) { .red-box { padding: 4px; } }
            @media (min-width: 900px) { .red-box { border: none; } }
            a:hover, p::before { color: green; }
        """.trimIndent()
        val document = Jsoup.parse(
            """
            <div class="red-box"><p class="title">[Warning]</p><p>You have entered the Dream Realm.</p></div>
            <p class="status" style="color: var(--undefined); border-left-color: rgb(255 117 26 / 50%)">Status</p>
            """.trimIndent(),
        )
        val cascade = HtmlProseCss.parse(listOf(css), CssViewport(widthPx = 412f, heightPx = 915f))
        document.body().getAllElements().forEach(cascade::applyTo)

        fun style(selector: String) = document.selectFirst(selector)!!.attributes().asList()
            .filter { it.key.startsWith(DOCUMENT_STYLE_ATTRIBUTE_PREFIX) }
            .associate { it.key.removePrefix(DOCUMENT_STYLE_ATTRIBUTE_PREFIX) to it.value }

        val box = style("div")
        assertEquals("#000", box["background-color"])
        assertEquals("linear-gradient(#111, #000)", box["background-image"])
        assertEquals(
            listOf("2px", "solid", "red"),
            listOf(box["border-top-width"], box["border-left-style"], box["border-bottom-color"]),
        )
        assertEquals(listOf("8px", "4px", "8px", "4px"), CORNERS.map { box["border-$it-radius"] })
        assertEquals(listOf("4px", "4px"), listOf(box["padding-top"], box["padding-left"]))
        assertEquals("0 0 10px rgba(36, 79, 255, .8)", box["box-shadow"])

        val title = style("p.title")
        assertEquals("#ffa970", title["color"])
        assertEquals(listOf("italic", "bold", "1.2em", "1.5", "\"Oswald\", serif"), FONT.map { title[it] })
        assertEquals(listOf("0", "auto", "0", "auto"), SIDES.map { title["margin-$it"] })

        val second = style("div > p:nth-child(2)")
        assertEquals("hsl(0 0% 100%)", second["color"])
        assertEquals("underline", second["text-decoration-line"])

        val status = style("p.status")
        assertEquals(null, status["color"])
        assertEquals(listOf("4px", "ridge", "rgb(255 117 26 / 50%)"), BORDER.map { status["border-left-$it"] })

        assertEquals(0xFF000000L, parseCssColor(box["background-color"]))
        assertEquals(0xFFFFFFFFL, parseCssColor(second["color"]))
        assertEquals(0x80FF751AL, parseCssColor(status["border-left-color"]))
        assertEquals(0xCC244FFFL, parseCssColor("rgba(36, 79, 255, .8)"))
        assertEquals(0xFF663399L, parseCssColor("RebeccaPurple"))
    }

    private companion object {
        val SIDES = listOf("top", "right", "bottom", "left")
        val CORNERS = listOf("top-left", "top-right", "bottom-right", "bottom-left")
        val FONT = listOf("font-style", "font-weight", "font-size", "line-height", "font-family")
        val BORDER = listOf("width", "style", "color")
    }
}
