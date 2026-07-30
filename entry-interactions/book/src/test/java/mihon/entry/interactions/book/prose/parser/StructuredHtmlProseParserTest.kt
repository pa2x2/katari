package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.text.style.URLSpan
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class StructuredHtmlProseParserTest : HtmlProseDocumentFixture() {
    @Test
    fun `structured blocks preserve readable order while correcting ordered list markers`() {
        val html = """
            <div>Intro line<br>second line</div>
            <h2>Heading</h2>
            <p>Paragraph one.</p>
            <p><br></p>
            <ol start="3">
                <li>Third item</li>
                <li>Fourth item<ul><li>Nested item</li></ul>Tail</li>
            </ol>
            <blockquote>Quoted text</blockquote>
            <p>Final paragraph.</p>
        """.trimIndent()
        val prepared = prepare(html)
        val recombined = prepared.blocks.joinToString(separator = "") { it.renderedText.toString() }

        assertEquals(prepared.combinedText.toString(), recombined)
        assertTrue(recombined.indexOf("Intro line") < recombined.indexOf("Heading"))
        assertTrue(recombined.indexOf("Heading") < recombined.indexOf("Paragraph one."))
        assertTrue(recombined.contains("3. Third item"))
        assertTrue(recombined.contains("4. Fourth item"))
        assertTrue(recombined.contains("• Nested item"))
        prepared.blocks.forEach { preparedBlock ->
            assertEquals(preparedBlock.block.logicalLength, preparedBlock.renderedText.length)
        }
    }

    @Test
    fun `HTML adapter preserves semantic block order and nested list structure`() {
        val prepared = prepare(
            """
                Intro <em>outside a paragraph</em>
                <h2 id="section">Heading</h2>
                <p>Body with <strong>emphasis</strong>.</p>
                <ol>
                    <li>Outer<ul><li>Nested</li></ul></li>
                    <li>Second</li>
                </ol>
                <blockquote><p>Quoted text</p></blockquote>
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                BookDocumentBlockKind.PARAGRAPH,
                BookDocumentBlockKind.HEADING,
                BookDocumentBlockKind.PARAGRAPH,
                BookDocumentBlockKind.LIST,
                BookDocumentBlockKind.QUOTE,
            ),
            prepared.document.blocks.map { it.role.kind },
        )
        assertEquals(2, prepared.document.blocks[1].role.level)
        assertEquals(listOf("section"), prepared.document.blocks[1].sourceFragments)
        assertEquals(true, prepared.document.blocks[3].role.ordered)
        assertTrue(prepared.document.blocks[3].plainText.contains("Outer"))
        assertTrue(prepared.document.blocks[3].plainText.contains("Nested"))
        assertTrue(prepared.document.blocks[3].plainText.contains("Second"))
        assertTrue(
            prepared.blocks[2].renderedText
                .getSpans(0, prepared.blocks[2].renderedText.length, StyleSpan::class.java)
                .any { it.style == Typeface.BOLD },
        )
    }

    @Test
    fun `paragraph images retain their authored position between rich text segments`() {
        val prepared = prepare(
            """
                <p id="mixed">
                    Before <strong>bold</strong>
                    <img src="image-1" alt="First">
                    after <a href="https://example.invalid/">linked text</a>
                    <img src="image-2" alt="Second">
                    tail.
                </p>
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                BookDocumentBlockKind.PARAGRAPH,
                BookDocumentBlockKind.FIGURE,
                BookDocumentBlockKind.PARAGRAPH,
                BookDocumentBlockKind.FIGURE,
                BookDocumentBlockKind.PARAGRAPH,
            ),
            prepared.document.blocks.map { it.role.kind },
        )
        assertTrue(prepared.document.blocks[0].plainText.contains("Before bold"))
        assertEquals("First", prepared.document.blocks[1].plainText)
        assertTrue(prepared.document.blocks[2].plainText.contains("after linked text"))
        assertEquals("Second", prepared.document.blocks[3].plainText)
        assertTrue(prepared.document.blocks[4].plainText.contains("tail."))
        assertTrue("mixed" in prepared.document.blocks.first().sourceFragments)
        assertTrue(prepared.document.blocks.drop(1).none { "mixed" in it.sourceFragments })
        assertTrue(
            prepared.blocks[0].renderedText
                .getSpans(0, prepared.blocks[0].renderedText.length, StyleSpan::class.java)
                .any { it.style == Typeface.BOLD },
        )
        assertEquals(
            "https://example.invalid/",
            prepared.blocks[2].renderedText
                .getSpans(0, prepared.blocks[2].renderedText.length, URLSpan::class.java)
                .single()
                .url,
        )
    }

    @Test
    fun `generated block identity survives unrelated insertion and disambiguates duplicate text`() {
        val original = prepare("<p>Repeated</p><p>Stable target</p><p>Repeated</p>")
        val edited = prepare("<p>Inserted</p><p>Repeated</p><p>Stable target</p><p>Repeated</p>")

        val originalTarget = original.document.blocks.single { it.plainText == "Stable target" }.id
        val editedTarget = edited.document.blocks.single { it.plainText == "Stable target" }.id
        val duplicateIds = original.document.blocks.filter { it.plainText == "Repeated" }.map { it.id }

        assertEquals(originalTarget, editedTarget)
        assertEquals(2, duplicateIds.distinct().size)
        assertNotEquals(duplicateIds[0], duplicateIds[1])
    }

    @Test
    fun `same-document anchors retain their block position and link span`() {
        val prepared = prepare(
            "<p><a href=\"#note\">See note</a></p><aside><p id=\"note\">Footnote text</p></aside>",
        )

        val position = requireNotNull(prepared.document.anchors["note"])
        val target = requireNotNull(prepared.block(position.blockId))
        val linkBlock = prepared.blocks.first()

        assertTrue(
            target.block.plainText.startsWith("Footnote text"),
            "Unexpected anchored block text: ${target.block.plainText}",
        )
        assertTrue(
            target.renderedText
                .subSequence(position.offsetWithinBlock, target.renderedText.length)
                .startsWith("Footnote text"),
        )
        assertTrue("note" in target.block.sourceFragments)
        assertEquals(
            "#note",
            linkBlock.renderedText.getSpans(0, linkBlock.renderedText.length, URLSpan::class.java).single().url,
        )
        assertTrue(prepared.combinedText.none { it == '\uE000' || it == '\uE001' })
    }

    @Test
    fun `empty and repeated-text anchors retain their exact rendered positions`() {
        val prepared = prepare(
            """
                <p><a href="#empty">Empty</a> <a href="#second">Second</a></p>
                <p>before<a id="empty"></a>after <span id="first">same</span>
                    between <span id="second">same</span></p>
            """.trimIndent(),
        )
        val target = prepared.blocks.last()
        val targetText = target.renderedText.toString()

        assertEquals(
            targetText.indexOf("after"),
            requireNotNull(prepared.document.anchors["empty"]).offsetWithinBlock,
        )
        assertEquals(
            targetText.indexOf("same"),
            requireNotNull(prepared.document.anchors["first"]).offsetWithinBlock,
        )
        assertEquals(
            targetText.lastIndexOf("same"),
            requireNotNull(prepared.document.anchors["second"]).offsetWithinBlock,
        )
    }

    @Test
    fun `inline style markers do not shift a following anchor`() {
        val prepared = prepare(
            """
                <p>Before <span data-katari-color="#ff112233"
                    data-katari-background="#ff445566">styled text</span>
                    then <a id="after-style"></a>anchored text</p>
            """.trimIndent(),
        )
        val block = prepared.blocks.single()
        val text = block.renderedText.toString()
        val inlineStyle = block.block.inlineStyles.single()

        assertEquals("styled text", text.substring(inlineStyle.start, inlineStyle.endExclusive))
        assertEquals(
            text.indexOf("anchored text"),
            requireNotNull(prepared.document.anchors["after-style"]).offsetWithinBlock,
        )
    }

    @Test
    fun `list items retain same-document return links`() {
        val prepared = prepare(
            """
                <p><a id="reference" href="#note">See note</a></p>
                <aside role="doc-endnotes">
                    <ol><li id="note">Note body <a href="#reference">Return</a></li></ol>
                </aside>
            """.trimIndent(),
        )
        val note = prepared.blocks.single { it.block.role.kind == BookDocumentBlockKind.LIST }
        val urls = note.renderedText
            .getSpans(0, note.renderedText.length, URLSpan::class.java)
            .map(URLSpan::getURL)

        assertEquals(listOf("#reference"), urls)
        assertTrue("note" in note.block.sourceFragments)
    }
}
