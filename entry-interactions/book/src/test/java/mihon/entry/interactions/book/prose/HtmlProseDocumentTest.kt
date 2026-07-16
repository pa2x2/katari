package mihon.entry.interactions.book.prose

import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class HtmlProseDocumentTest {
    @Test
    fun `native prose parser preserves visible chapter content`() {
        val document = parseProseHtml("<h1>Chapter 1</h1><p>Hello <strong>reader</strong>.</p>")

        assertTrue(document.text.contains("Chapter 1"))
        assertTrue(document.text.contains("Hello reader."))
    }

    @Test
    fun `native prose parser maps same-document anchor targets`() {
        val document = parseProseHtml(
            "<p><a href=\"#note\">See note</a></p><aside id=\"note\">Footnote text</aside>",
        )

        val offset = requireNotNull(document.anchors["note"])
        assertTrue(document.text.substring(offset).startsWith("Footnote text"))
        assertTrue(document.text.none { it == '\uE000' || it == '\uE001' })
    }

    @Test
    fun `same-document links dispatch their anchor id`() {
        val document = parseProseHtml("<a href=\"#note\">See note</a><p id=\"note\">Footnote</p>")
        var clickedAnchor: String? = null

        val linked = document.text.withAnchorClicks { clickedAnchor = it } as Spanned
        val span = linked.getSpans(0, linked.length, ClickableSpan::class.java).single()
        span.onClick(View(RuntimeEnvironment.getApplication()))

        assertEquals("note", clickedAnchor)
    }

    @Test
    fun `ordinary prose taps remain available to the reader chrome`() {
        val view = laidOutTextView(
            SpannableString("Link then plain prose").apply {
                setSpan(testClickableSpan(), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            },
        )
        val plainTextX = view.layout.getPrimaryHorizontal(10)
        val textY = (view.layout.getLineTop(0) + view.layout.getLineBottom(0)) / 2f
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, plainTextX, textY, 0)

        val handled = view.dispatchTouchEvent(event)

        event.recycle()
        assertEquals(false, handled)
    }

    @Test
    fun `prose links remain clickable`() {
        var clicked = false
        val view = laidOutTextView(
            SpannableString("Link then plain prose").apply {
                setSpan(testClickableSpan { clicked = true }, 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            },
        )
        val linkX = view.layout.getPrimaryHorizontal(2)
        val textY = (view.layout.getLineTop(0) + view.layout.getLineBottom(0)) / 2f
        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, linkX, textY, 0)
        val up = MotionEvent.obtain(0, 1, MotionEvent.ACTION_UP, linkX, textY, 0)

        val downHandled = view.dispatchTouchEvent(down)
        val upHandled = view.dispatchTouchEvent(up)

        down.recycle()
        up.recycle()
        assertTrue(downHandled)
        assertTrue(upHandled)
        assertTrue(clicked)
    }

    @Test
    fun `pagination fills multiple lines before advancing`() {
        val chapter = HtmlProseLoadedChapter(
            chapter = EntryChapter.create().copy(id = 1L, name = "Chapter 1"),
            resourceId = "chapter-1",
            bodyHtml = List(30) {
                "<p>Paragraph $it contains enough prose to wrap onto another line.</p>"
            }.joinToString(""),
            initialProgression = 0f,
        )
        val document = parseProseHtml(chapter.bodyHtml)
        val pages = paginateProse(
            chapter = chapter,
            text = document.text,
            paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = 20f },
            availableWidthPx = 320,
            availableHeightPx = 480,
            alignment = Layout.Alignment.ALIGN_NORMAL,
            lineSpacingMultiplier = 1.5f,
            justificationMode = Layout.JUSTIFICATION_MODE_NONE,
        )

        assertTrue(pages.size > 1)
        assertTrue(pages.first().text.lines().size > 1)
        assertEquals(pages.size, pages.last().total)
    }

    @Test
    fun `pagination only includes lines that fully fit on the page`() {
        val chapter = HtmlProseLoadedChapter(
            chapter = EntryChapter.create().copy(id = 1L, name = "Chapter 1"),
            resourceId = "chapter-1",
            bodyHtml = List(30) { "<p>Line $it with enough prose to wrap across the page width.</p>" }
                .joinToString(""),
            initialProgression = 0f,
        )
        val document = parseProseHtml(chapter.bodyHtml)
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = 20f }
        val availableWidth = 320
        val availableHeight = 97
        val pages = paginateProse(
            chapter = chapter,
            text = document.text,
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
    fun `anchor offsets resolve to their containing page`() {
        val chapter = EntryChapter.create().copy(id = 1L, name = "Chapter 1")
        val pages = listOf(
            HtmlProsePage(chapter, 0, 2, "First", sourceStart = 0, sourceEndExclusive = 5),
            HtmlProsePage(chapter, 1, 2, "Second", sourceStart = 5, sourceEndExclusive = 11),
        )

        assertEquals(0, pageIndexForAnchor(pages, 4))
        assertEquals(1, pageIndexForAnchor(pages, 5))
        assertEquals(1, pageIndexForAnchor(pages, 11))
    }

    private fun laidOutTextView(text: SpannableString): TextView {
        return ProseTextView(RuntimeEnvironment.getApplication()).apply {
            layoutParams = ViewGroup.LayoutParams(600, 100)
            movementMethod = android.text.method.LinkMovementMethod.getInstance()
            setText(text, TextView.BufferType.SPANNABLE)
            includeFontPadding = false
            setPadding(0, 0, 0, 0)
            measure(
                MeasureSpec.makeMeasureSpec(600, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
            )
            layout(0, 0, measuredWidth, measuredHeight)
        }
    }

    private fun testClickableSpan(onClick: () -> Unit = {}): ClickableSpan {
        return object : ClickableSpan() {
            override fun onClick(widget: View) = onClick()
        }
    }
}
