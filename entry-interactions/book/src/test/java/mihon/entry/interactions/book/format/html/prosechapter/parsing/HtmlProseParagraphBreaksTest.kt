package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HtmlProseParagraphBreaksTest {
    @Test
    fun `chapters that separate paragraphs with blank lines keep them as paragraphs`() {
        // The shape web novel sites serve: loose text in one container, paragraphs split by `<br><br>`, padded with
        // extra breaks and `&nbsp;` lines, and a single `<br>` inside a paragraph for a line of a letter.
        val chapter = """
            <div id="chr-content">
            Chapter 41: The Northern Gate<br><br>
            The gate had not opened in a hundred years.<br><br><br>
            &nbsp;<br>
            “Open it,” she said.<br>“Now.”<br><br>
            <p>He did not move.<br><br>Not yet.</p>
            <h3>Part<br><br>Two</h3>
            </div>
        """.trimIndent()

        val document = HtmlProseDocumentParser().parse(
            "chapter",
            null,
            HtmlProseSanitizer.sanitize(chapter.encodeToByteArray()),
        )

        assertEquals(
            listOf(
                "Chapter 41: The Northern Gate",
                "The gate had not opened in a hundred years.",
                "“Open it,” she said.\n“Now.”",
                "He did not move.",
                "Not yet.",
                "Part\nTwo",
            ),
            document.blocks.map { it.plainText },
        )
    }
}
