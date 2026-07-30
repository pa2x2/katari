package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.text.style.URLSpan
import mihon.entry.interactions.book.document.model.BookDocumentAlignment
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.model.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.model.BookDocumentWhiteSpace
import mihon.entry.interactions.book.document.model.MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class StructuredHtmlRichBlocksTest : HtmlProseDocumentFixture() {
    @Test
    fun `semantic parser models rich structures and safe style tokens`() {
        val prepared = prepare(
            """
                <hr id="scene">
                <pre id="system">line one
                  line two</pre>
                <figure id="figure">
                  <img src="image-1" alt="Blue seal" width="256" height="128">
                  <figcaption>Archive seal</figcaption>
                </figure>
                <table id="status">
                  <caption>State</caption>
                  <tr><th scope="col">Name</th><th scope="col">Value</th></tr>
                  <tr><th scope="row">Level</th><td colspan="1" rowspan="2">17</td></tr>
                </table>
                <details id="spoiler" open><summary>Warning</summary><p>Hidden body</p></details>
                <div id="styled"
                     data-katari-align="right"
                     data-katari-white-space="pre-wrap"
                     data-katari-background="#ffe9f0ff"
                     data-katari-font-resource="font-1">Styled panel</div>
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                BookDocumentBlockKind.THEMATIC_BREAK,
                BookDocumentBlockKind.PREFORMATTED,
                BookDocumentBlockKind.FIGURE,
                BookDocumentBlockKind.TABLE,
                BookDocumentBlockKind.DISCLOSURE,
                BookDocumentBlockKind.CALLOUT,
            ),
            prepared.document.blocks.map { it.role.kind },
        )
        assertIs<BookDocumentBlockContent.ThematicBreak>(prepared.document.blocks[0].content)
        assertEquals(
            BookDocumentWhiteSpace.PRE,
            prepared.document.blocks[1].style.whiteSpace,
        )
        val figure = assertIs<BookDocumentBlockContent.Figure>(prepared.document.blocks[2].content)
        assertEquals("image-1", figure.image.resourceId)
        assertEquals("Blue seal", figure.image.alternativeText)
        assertEquals("Archive seal", figure.caption)
        val table = assertIs<BookDocumentBlockContent.Table>(prepared.document.blocks[3].content)
        assertEquals(2, table.columnCount)
        assertEquals(2, table.rows[1].cells[1].rowSpan)
        val disclosure = assertIs<BookDocumentBlockContent.Disclosure>(prepared.document.blocks[4].content)
        assertTrue(disclosure.initiallyExpanded)
        assertEquals(listOf(BookDocumentBlockKind.PARAGRAPH), disclosure.body.map { it.role.kind })
        assertEquals("Hidden body", disclosure.body.single().plainText)
        assertEquals(BookDocumentAlignment.END, prepared.document.blocks[5].style.alignment)
        assertEquals(
            BookDocumentFontFamily.Resource("font-1"),
            prepared.document.blocks[5].style.fontFamily,
        )
        assertEquals(setOf("image-1", "font-1"), prepared.document.resourceIds)
    }

    @Test
    fun `table captions and cells retain same-document and external links`() {
        val prepared = prepare(
            """
                <table>
                    <caption><a href="#target">Caption link</a></caption>
                    <tr>
                        <td><a id="cell"></a><a href="#target">Cell link</a>
                            and <a href="HTTPS://example.invalid/path">external link</a></td>
                    </tr>
                </table>
                <p id="target">Target paragraph</p>
            """.trimIndent(),
        )
        val tableBlock = prepared.blocks.first()
        val table = assertIs<BookDocumentBlockContent.Table>(tableBlock.block.content)
        val cell = table.rows.single().cells.single()

        assertEquals(
            listOf(BookDocumentLinkTarget.Anchor("target")),
            table.captionLinks.map { it.target },
        )
        assertEquals(
            listOf(
                BookDocumentLinkTarget.Anchor("target"),
                BookDocumentLinkTarget.External("HTTPS://example.invalid/path"),
            ),
            cell.links.map { it.target },
        )
        assertEquals(
            "Caption link",
            table.caption?.substring(
                table.captionLinks.single().let {
                    it.start until
                        it.endExclusive
                },
            ),
        )
        assertEquals(
            listOf("Cell link", "external link"),
            cell.links.map { cell.text.substring(it.start, it.endExclusive) },
        )
        assertEquals(
            tableBlock.renderedText.toString().indexOf("Cell link"),
            requireNotNull(prepared.document.anchors["cell"]).offsetWithinBlock,
        )
        assertEquals(
            listOf("#target", "#target", "HTTPS://example.invalid/path"),
            tableBlock.renderedText
                .getSpans(0, tableBlock.renderedText.length, URLSpan::class.java)
                .map(URLSpan::getURL),
        )
    }

    @Test
    fun `disclosure body retains links emphasis styles and nested semantic blocks`() {
        val prepared = prepare(
            """
                <details id="spoiler" open>
                    <summary>Warning</summary>
                    <p>Body with <em>emphasis</em> and <a href="https://example.invalid/">a link</a>.</p>
                    <div data-katari-background="#ff112233">Styled panel</div>
                    <figure><img src="image-1" alt="Seal"></figure>
                    <ul><li>Nested item</li></ul>
                    <details><summary>Nested disclosure</summary><p>Nested body</p></details>
                </details>
            """.trimIndent(),
        )

        val disclosureBlock = prepared.blocks.single()
        val disclosure = assertIs<BookDocumentBlockContent.Disclosure>(disclosureBlock.block.content)
        assertEquals(
            listOf(
                BookDocumentBlockKind.PARAGRAPH,
                BookDocumentBlockKind.CALLOUT,
                BookDocumentBlockKind.FIGURE,
                BookDocumentBlockKind.LIST,
                BookDocumentBlockKind.DISCLOSURE,
            ),
            disclosure.body.map { it.role.kind },
        )
        assertEquals(disclosure.body, disclosureBlock.disclosureBody.map { it.block })
        val richParagraph = disclosureBlock.disclosureBody.first()
        assertTrue(
            richParagraph.renderedText
                .getSpans(0, richParagraph.renderedText.length, StyleSpan::class.java)
                .any { it.style == Typeface.ITALIC },
        )
        assertEquals(
            "https://example.invalid/",
            richParagraph.renderedText
                .getSpans(0, richParagraph.renderedText.length, URLSpan::class.java)
                .single()
                .url,
        )
        assertEquals(0xFF112233, disclosure.body[1].style.backgroundArgb)
        assertEquals(
            "Nested body",
            disclosureBlock.disclosureBody.last().disclosureBody.single().block.plainText,
        )
        assertEquals(setOf("image-1"), prepared.document.resourceIds)
    }

    @Test
    fun `table row spans are bounded by the document model limit`() {
        val prepared = prepare(
            """
                <table>
                    <tr><th>Label</th><td rowspan="25">Value</td></tr>
                </table>
            """.trimIndent(),
        )

        val table = assertIs<BookDocumentBlockContent.Table>(prepared.document.blocks.single().content)
        assertEquals(MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN, table.rows.single().cells[1].rowSpan)
    }

    @Test
    fun `table width includes columns placed after carried row spans`() {
        val prepared = prepare(
            """
                <table>
                    <tr><th>Event</th><th>Type</th><th>Requirement</th><th>Effect</th></tr>
                    <tr>
                        <th>A-17</th>
                        <td rowspan="3">Luminous</td>
                        <td>Door closed</td>
                        <td>Blue light</td>
                    </tr>
                    <tr><th>A-18</th><td>Door open</td><td>Ledger glows</td></tr>
                    <tr>
                        <th>A-19</th>
                        <td colspan="2">Combined condition</td>
                        <td>All clocks stop for one minute</td>
                    </tr>
                </table>
            """.trimIndent(),
        )

        val table = assertIs<BookDocumentBlockContent.Table>(prepared.document.blocks.single().content)
        assertEquals(5, table.columnCount)
        assertEquals(
            "All clocks stop for one minute",
            table.rows.last().cells.last().text,
        )
    }

    @Test
    fun `empty table cells remain structural columns`() {
        val prepared = prepare(
            """
                <table>
                    <tr><td>Left</td><td></td><td>Right</td></tr>
                </table>
            """.trimIndent(),
        )

        val table = assertIs<BookDocumentBlockContent.Table>(prepared.document.blocks.single().content)
        assertEquals(3, table.columnCount)
        assertEquals(listOf("Left", "", "Right"), table.rows.single().cells.map { it.text })
    }

    @Test
    fun `table anchors resolve to their exact lower row cell`() {
        val prepared = prepare(
            """
                <table>
                    <tr><td>First row</td><td>Earlier value</td></tr>
                    <tr><td>Second row</td><td>Before<a id="late-cell"></a>target</td></tr>
                </table>
            """.trimIndent(),
        )
        val block = prepared.blocks.single()
        val table = assertIs<BookDocumentBlockContent.Table>(block.block.content)
        val anchor = requireNotNull(prepared.document.anchors["late-cell"])

        assertEquals(
            ProseTableAnchorTarget.Cell(rowIndex = 1, cellIndex = 1, characterOffset = "Before".length),
            table.resolveProseTableAnchorTarget(anchor.offsetWithinBlock),
        )
    }

    @Test
    fun `nested disclosure anchor offsets resolve through each collapsed disclosure`() {
        val prepared = prepare(
            """
                <details>
                    <summary>Outer summary</summary>
                    <p>Outer body</p>
                    <details>
                        <summary>Nested summary</summary>
                        <p>Before<a id="deep"></a>deep target</p>
                    </details>
                </details>
            """.trimIndent(),
        )
        val outer = prepared.blocks.single()
        val anchor = requireNotNull(prepared.document.anchors["deep"])
        val outerTarget = requireNotNull(
            resolveProseDisclosureAnchorTarget(
                summary = assertIs<BookDocumentBlockContent.Disclosure>(outer.block.content).summary,
                body = outer.disclosureBody,
                offsetWithinDisclosure = anchor.offsetWithinBlock,
            ),
        )
        val nested = outerTarget.block
        val nestedTarget = requireNotNull(
            resolveProseDisclosureAnchorTarget(
                summary = assertIs<BookDocumentBlockContent.Disclosure>(nested.block.content).summary,
                body = nested.disclosureBody,
                offsetWithinDisclosure = outerTarget.offsetWithinBlock,
            ),
        )

        assertTrue(nestedTarget.block.block.plainText.startsWith("Beforedeep target"))
        assertEquals(
            nestedTarget.block.renderedText.toString().indexOf("deep target"),
            nestedTarget.offsetWithinBlock,
        )
    }
}
