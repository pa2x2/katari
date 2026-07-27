package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import mihon.entry.interactions.book.document.model.BookDocumentAlignment
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.model.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.model.BookDocumentWhiteSpace
import mihon.entry.interactions.book.document.model.MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class HtmlProseDocumentTest {
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

    @Test
    fun `pagination excludes a line that does not fully fit`() {
        val prepared = prepare(
            List(30) { "<p>Line $it with enough prose to wrap across the page width.</p>" }.joinToString(""),
        )
        val chapter = chapter()
        val section = BookDocumentSection(
            key = chapter.id.toString(),
            owner = chapter,
            document = prepared,
            initialPosition = prepared.document.positionAtProgression(0f),
            resourceLoader = null,
        )
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = 20f }
        val availableWidth = 320
        val availableHeight = 97

        val pages = paginateProse(
            chapter = section,
            text = prepared.combinedText,
            paint = paint,
            availableWidthPx = availableWidth,
            availableHeightPx = availableHeight,
            alignment = Layout.Alignment.ALIGN_NORMAL,
            lineSpacingMultiplier = 1.5f,
        )

        assertTrue(pages.size > 1)
        pages.forEach { page ->
            val layout = StaticLayout.Builder.obtain(page.text, 0, page.text.length, paint, availableWidth)
                .setIncludePad(false)
                .setLineSpacing(0f, 1.5f)
                .build()
            assertTrue(layout.height <= availableHeight, "Page ${page.index + 1} is ${layout.height}px tall")
        }
    }

    @Test
    fun `anchor at a page boundary resolves to the following page`() {
        val chapter = chapter()
        val pages = listOf(
            HtmlProsePage(chapter, 0, 2, SpannableString("First"), 0f, 0, 5),
            HtmlProsePage(chapter, 1, 2, SpannableString("Second"), 1f, 5, 11),
        )

        assertEquals(0, pageIndexForAnchor(pages, 4))
        assertEquals(1, pageIndexForAnchor(pages, 5))
        assertEquals(1, pageIndexForAnchor(pages, 11))
    }

    @Test
    fun `structured pagination gives rich blocks stable dedicated pages`() {
        val prepared = prepare(
            """
                <p>Before rich content.</p>
                <hr id="scene">
                <figure id="figure"><img src="image-1" alt="Blue seal"></figure>
                <details id="spoiler"><summary>Warning</summary><p>Hidden body</p></details>
                <p>After rich content.</p>
            """.trimIndent(),
        )
        val chapter = chapter()
        val section = BookDocumentSection(
            key = chapter.id.toString(),
            owner = chapter,
            document = prepared,
            initialPosition = prepared.document.positionAtProgression(0f),
            resourceLoader = null,
        )

        val pages = paginateStructuredProse(
            chapter = section,
            paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = 20f },
            availableWidthPx = 320,
            availableHeightPx = 120,
            alignment = Layout.Alignment.ALIGN_NORMAL,
            lineSpacingMultiplier = 1.2f,
        )

        val dedicated = pages.filter { it.structuredBlock != null }
        assertEquals(
            listOf(
                BookDocumentBlockKind.THEMATIC_BREAK,
                BookDocumentBlockKind.FIGURE,
                BookDocumentBlockKind.DISCLOSURE,
            ),
            dedicated.map { it.structuredBlock?.block?.role?.kind },
        )
        dedicated.forEach { page ->
            assertEquals(page.structuredBlock?.block?.logicalStart, page.sourceStart)
            assertEquals(page.structuredBlock?.block?.logicalEndExclusive, page.sourceEndExclusive)
        }
        assertEquals(pages.indices.toList(), pages.map(HtmlProsePage::index))
        assertEquals(1f, pages.last().progression)
    }

    @Test
    fun `ordinary block styles remain in continuous text pagination`() {
        val prepared = prepare(
            """
                <p data-katari-color="#ff112233" data-katari-align="center">First styled paragraph.</p>
                <p data-katari-font-generic="serif">Second styled paragraph.</p>
                <p data-katari-font-scale="1.25">Third styled paragraph.</p>
                <p data-katari-bold="true">Fourth styled paragraph.</p>
            """.trimIndent(),
        )
        val chapter = chapter()
        val section = BookDocumentSection(
            key = chapter.id.toString(),
            owner = chapter,
            document = prepared,
            initialPosition = prepared.document.positionAtProgression(0f),
            resourceLoader = null,
        )

        val pages = paginateStructuredProse(
            chapter = section,
            paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = 20f },
            availableWidthPx = 800,
            availableHeightPx = 800,
            alignment = Layout.Alignment.ALIGN_NORMAL,
            lineSpacingMultiplier = 1.2f,
        )

        assertEquals(1, pages.size)
        assertEquals(null, pages.single().structuredBlock)
        assertTrue(pages.single().text.contains("First styled paragraph."))
        assertTrue(pages.single().text.contains("Fourth styled paragraph."))
        assertTrue(
            pages.single().text.getSpans(0, pages.single().text.length, ForegroundColorSpan::class.java).isNotEmpty(),
        )
        assertTrue(pages.single().text.getSpans(0, pages.single().text.length, AlignmentSpan::class.java).isNotEmpty())
        assertTrue(pages.single().text.getSpans(0, pages.single().text.length, TypefaceSpan::class.java).isNotEmpty())
        assertTrue(
            pages.single().text.getSpans(0, pages.single().text.length, RelativeSizeSpan::class.java).isNotEmpty(),
        )
        assertTrue(
            pages.single().text
                .getSpans(0, pages.single().text.length, StyleSpan::class.java)
                .any { it.style == Typeface.BOLD },
        )
    }

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

    private fun prepare(html: String) = prepareHtmlBookDocument(
        resourceId = "chapter-1",
        revision = "r1",
        bodyHtml = html,
    )

    private fun chapter() = EntryChapter.create().copy(id = 1L, name = "Chapter 1")
}
