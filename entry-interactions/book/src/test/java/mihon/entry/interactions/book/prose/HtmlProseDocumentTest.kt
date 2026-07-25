package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.text.style.URLSpan
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class HtmlProseDocumentTest {
    @Test
    fun `splitting blocks preserves the legacy whole-document text layout`() {
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
        val legacy = legacyWholeDocumentText(html)
        val recombined = SpannableStringBuilder().apply {
            prepared.blocks.forEach { append(it.renderedText) }
        }

        assertEquals(legacy.toString(), prepared.combinedText.toString())
        assertEquals(legacy.toString(), recombined.toString())
        assertEquals(spanLayout(legacy), spanLayout(prepared.combinedText))
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

        assertTrue(target.block.plainText.startsWith("Footnote text"))
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

    private fun prepare(html: String) = prepareHtmlBookDocument(
        resourceId = "chapter-1",
        revision = "r1",
        bodyHtml = html,
    )

    private fun chapter() = EntryChapter.create().copy(id = 1L, name = "Chapter 1")

    private fun legacyWholeDocumentText(html: String): SpannableStringBuilder {
        val parsed = SpannableStringBuilder(
            androidx.core.text.HtmlCompat.fromHtml(
                org.jsoup.Jsoup.parseBodyFragment(html).body().html(),
                androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY,
            ),
        )
        var index = parsed.length - 1
        while (index >= 0) {
            if (parsed[index] == '\n') {
                val end = index + 1
                while (index >= 0 && parsed[index] == '\n') index--
                val start = index + 1
                if (end - start >= 2) parsed.replace(start, end, "\n\n")
            } else {
                index--
            }
        }
        return parsed
    }

    private fun spanLayout(text: android.text.Spanned): List<Triple<String, Int, Int>> =
        text.getSpans(0, text.length, Any::class.java)
            .map { span ->
                Triple(
                    span.javaClass.name,
                    text.getSpanStart(span),
                    text.getSpanEnd(span),
                )
            }
            .sortedWith(compareBy({ it.second }, { it.third }, { it.first }))
}
