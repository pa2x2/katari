package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.view.View.MeasureSpec
import android.widget.TextView
import mihon.book.api.document.BookDocumentBlockKind
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.BookDocumentTextView
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class HtmlProsePaginationTest : HtmlProseDocumentFixture() {
    @Test
    fun `pagination produces pages that fit the rendered text viewport`() {
        val prepared = prepare(
            List(80) {
                """
                    <p>“Measured serif prose $it” fills a feature-length chapter with enough words to wrap close to
                    the page edge. Its punctuation—and glyph overhangs—must not change the rendered page count.</p>
                """.trimIndent()
            }.joinToString(""),
        )
        val chapter = chapter()
        val section = BookDocumentSection(
            key = chapter.id.toString(),
            owner = chapter,
            document = prepared,
            initialPosition = prepared.document.positionAtProgression(0f),
            resourceLoader = null,
        )
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = 42f
            typeface = Typeface.SERIF
        }
        val availableWidth = 974
        val availableHeight = 2_074

        val pages = paginateProse(
            chapter = section,
            text = prepared.combinedText,
            paint = paint,
            availableWidthPx = availableWidth,
            availableHeightPx = availableHeight,
            alignment = Layout.Alignment.ALIGN_NORMAL,
            lineSpacingMultiplier = 1.7f,
        )

        assertTrue(pages.size > 1)
        pages.forEach { page ->
            val view = BookDocumentTextView(RuntimeEnvironment.getApplication()).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, paint.textSize)
                typeface = paint.typeface
                setLineSpacing(0f, 1.7f)
                setText(page.text, TextView.BufferType.SPANNABLE)
                measure(
                    MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(availableHeight, MeasureSpec.EXACTLY),
                )
                layout(0, 0, measuredWidth, measuredHeight)
            }
            assertTrue(
                view.layout.height <= availableHeight,
                "Page ${page.index + 1} renders at ${view.layout.height}px in a ${availableHeight}px viewport",
            )
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
    fun `inline size semantics are applied exactly once during pagination`() {
        val prepared = prepare(
            """
                <p>Before <span data-katari-font-scale="1.25">scaled</span>
                    and <small>small</small> after.</p>
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

        val page = paginateStructuredProse(
            chapter = section,
            paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = 20f },
            availableWidthPx = 800,
            availableHeightPx = 800,
            alignment = Layout.Alignment.ALIGN_NORMAL,
            lineSpacingMultiplier = 1.2f,
        ).single()

        fun sizeChanges(value: String): List<Float> {
            val start = page.text.toString().indexOf(value)
            return page.text
                .getSpans(start, start + value.length, RelativeSizeSpan::class.java)
                .map(RelativeSizeSpan::getSizeChange)
        }

        assertEquals(listOf(1.25f), sizeChanges("scaled"))
        assertEquals(listOf(0.8f), sizeChanges("small"))
    }
}
