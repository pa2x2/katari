package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentSideBySideLayout
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HtmlProseSideBySideParserTest {
    @Test
    fun `a flex status window keeps its cards side by side while a phone media rule stacks a grid`() {
        val document = HtmlProseDocumentParser().parse(
            "chapter",
            null,
            HtmlProseSanitizer.sanitize(
                """
                <style>
                .stats { display: flex; gap: 1em; }
                .card { flex: 1; border: 1px solid #888; padding: 4px; }
                .gallery { display: grid; grid-template-columns: repeat(3, 1fr); }
                @media (max-width: 600px) { .gallery { grid-template-columns: 1fr; } }
                </style>
                <p>Status</p>
                <div class="stats"><div class="card" id="str"><p>STR</p><p>12</p></div><div class="card"><p>AGI <a id="agi"></a>15</p></div></div>
                <div class="gallery"><p>One</p><p>Two</p></div>
                """.trimIndent().encodeToByteArray(),
            ),
        )

        assertEquals(listOf("Status", "STR\n12\nAGI 15", "One", "Two"), document.blocks.map { it.plainText })
        val row = document.blocks[1]
        val content = assertIs<BookDocumentBlockContent.SideBySide>(row.content)
        assertEquals(1f, assertIs<BookDocumentSideBySideLayout.Flex>(content.layout).columnGapEm)
        assertEquals(listOf(1f, 1f), content.items.map { it.sizing.grow })
        // Each card's border is its own box around all of its blocks.
        content.items.forEach { item ->
            assertEquals(
                listOf(0 to item.body.blocks.lastIndex),
                item.body.boxes.map {
                    it.firstBlockIndex to
                        it.lastBlockIndex
                },
            )
        }
        val rowText = document.content.text.substring(row.logicalStart, row.logicalEndExclusive)
        assertEquals("STR\n\n12\n\nAGI 15\n\n", rowText)
        assertEquals(row.id, document.content.anchors.getValue("str").blockId)
        assertEquals(rowText.indexOf("AGI") + "AGI".length, document.content.anchors.getValue("agi").offsetWithinBlock)
    }
}
