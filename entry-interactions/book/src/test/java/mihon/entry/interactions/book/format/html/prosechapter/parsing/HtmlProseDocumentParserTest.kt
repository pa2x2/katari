package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HtmlProseDocumentParserTest {
    @Test
    fun `semantic prose is assembled into canonical ranges anchors and resources`() {
        val document = parse(
            """
            <article id="chapter">
              <h1 id="title">Chapter One</h1>
              <p>Read <strong>carefully</strong> and <a href="#note">continue</a>.</p>
              <ol start="2"><li>Second<ul><li id="nested">Nested</li></ul></li></ol>
              <figure id="map"><img src="images/map.png" alt="Map"><figcaption>The route</figcaption></figure>
              <table><tr><th scope="col">Name</th><th>Value</th></tr><tr><td>A</td><td>1</td></tr></table>
              <details id="note" open><summary>Note</summary><p>Extra context</p></details>
            </article>
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                BookDocumentBlockKind.HEADING,
                BookDocumentBlockKind.PARAGRAPH,
                BookDocumentBlockKind.LIST,
                BookDocumentBlockKind.FIGURE,
                BookDocumentBlockKind.TABLE,
                BookDocumentBlockKind.DISCLOSURE,
            ),
            document.blocks.map { it.role.kind },
        )
        assertNotNull(document.anchors["chapter"])
        assertNotNull(document.anchors["title"])
        assertNotNull(document.anchors["nested"])
        assertNotNull(document.anchors["note"])
        val paragraph = document.blocks[1]
        assertTrue(paragraph.inlineStyles.any { it.style.bold })
        assertIs<BookDocumentLinkTarget.Anchor>(paragraph.links.single().target)

        val list = assertIs<BookDocumentBlockContent.ListBlock>(document.blocks[2].content)
        assertEquals(listOf(0, 1), list.items.map { it.depth })
        assertEquals(listOf("2.", "•"), list.items.map { it.marker })

        val figure = assertIs<BookDocumentBlockContent.Figure>(document.blocks[3].content)
        assertEquals("images/map.png", figure.image.resourceId)
        assertEquals(setOf("images/map.png"), document.resourceIds)

        val table = assertIs<BookDocumentBlockContent.Table>(document.blocks[4].content)
        assertEquals(2, table.columnCount)
        val disclosure = assertIs<BookDocumentBlockContent.Disclosure>(document.blocks[5].content)
        assertTrue(disclosure.initiallyExpanded)
        assertEquals("Extra context", disclosure.body.blocks.single().plainText)
    }

    @Test
    fun `table width beyond the canonical grid is rejected`() {
        val cells = (1..25).joinToString("") { "<td>$it</td>" }
        val body = HtmlProseSanitizer.sanitize("<table><tr>$cells</tr></table>".encodeToByteArray())

        assertFailsWith<HtmlProseLimitExceededException> {
            HtmlProseDocumentParser().parse("chapter", null, body)
        }
    }

    @Test
    fun `paragraph images remain publication scoped semantic figures`() {
        val document = parse("<p>Before<img src='image.webp' alt='Scene'>After</p>")

        assertEquals(
            listOf(BookDocumentBlockKind.PARAGRAPH, BookDocumentBlockKind.FIGURE, BookDocumentBlockKind.PARAGRAPH),
            document.blocks.map { it.role.kind },
        )
        assertEquals(listOf("Before", "Scene", "After"), document.blocks.map { it.plainText })
        assertEquals(setOf("image.webp"), document.resourceIds)
    }

    @Test
    fun `canonical text is rejected before it exceeds the accepted extent`() {
        val text = "a".repeat(HtmlProseChapterContract.MAX_CANONICAL_UTF16)
        val body = HtmlProseSanitizer.sanitize("<p>$text</p>".encodeToByteArray())

        assertFailsWith<HtmlProseLimitExceededException> {
            HtmlProseDocumentParser().parse("chapter", null, body)
        }
    }

    private fun parse(html: String) = HtmlProseDocumentParser().parse(
        resourceId = "chapter",
        revision = "revision",
        body = HtmlProseSanitizer.sanitize(html.encodeToByteArray()),
    )
}
