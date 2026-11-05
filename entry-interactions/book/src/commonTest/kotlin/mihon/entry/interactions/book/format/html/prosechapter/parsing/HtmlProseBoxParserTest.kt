package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBorderStyle
import mihon.book.api.document.BookDocumentInsets
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HtmlProseBoxParserTest {
    @Test
    fun `nested boxes cover their own blocks past skipped empty ones and keep their margins off the paragraphs`() {
        val document = HtmlProseDocumentParser().parse(
            "chapter",
            null,
            HtmlProseSanitizer.sanitize(
                """
                <style>
                .window { background: #102030; border: 2px solid #ff751a; margin: 1em 0; padding: 8px; }
                .stats { border-left: 3px double red; }
                </style>
                <p>Before</p>
                <div class="window">
                  <p> </p>
                  <p>Name: Kai</p>
                  <div class="stats"><p>HP 10</p><p>MP 4</p></div>
                </div>
                <p style="margin-top: 2em">After</p>
                """.trimIndent().encodeToByteArray(),
            ),
        )

        assertEquals(listOf("Before", "Name: Kai", "HP 10", "MP 4", "After"), document.blocks.map { it.plainText })
        assertEquals(listOf(1..3, 2..3), document.content.boxes.map { it.firstBlockIndex..it.lastBlockIndex })
        val (window, stats) = document.content.boxes.map { it.style }
        assertEquals(0xFF102030L, window.backgroundArgb)
        assertEquals(BookDocumentInsets(top = 1f, bottom = 1f), window.marginEm)
        assertEquals(BookDocumentInsets(0.5f, 0.5f, 0.5f, 0.5f), window.paddingEm)
        assertEquals(BookDocumentBorderStyle.DOUBLE, stats.borders.left?.style)
        assertEquals(null, stats.borders.top)
        assertEquals(
            listOf(
                BookDocumentBlockKind.PARAGRAPH,
                BookDocumentBlockKind.CALLOUT,
                BookDocumentBlockKind.CALLOUT,
                BookDocumentBlockKind.CALLOUT,
                BookDocumentBlockKind.PARAGRAPH,
            ),
            document.blocks.map { it.role.kind },
        )
        assertEquals(listOf(0f, 0f, 0f, 0f, 2f), document.blocks.map { it.style.spacingBeforeEm })
    }
}
