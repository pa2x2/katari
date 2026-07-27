package mihon.entry.interactions.book.document.reader

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BookDocumentTextViewTest {
    @Test
    fun `terminal line trimming preserves whole-document height across separate block views`() {
        val whole = measuredTextView(SpannableString("First paragraph\n\nSecond paragraph\n\nThird paragraph"))
        val blocks = listOf(
            measuredTextView(SpannableString("First paragraph\n\n"), trimTerminalLine = true),
            measuredTextView(SpannableString("Second paragraph\n\n"), trimTerminalLine = true),
            measuredTextView(SpannableString("Third paragraph")),
        )

        assertEquals(whole.measuredHeight, blocks.sumOf(TextView::getMeasuredHeight))
    }

    @Test
    fun `same-document URL span dispatches its anchor instead of opening a URL`() {
        val source = SpannableString("See note").apply {
            setSpan(URLSpan("#note"), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        var clickedAnchor: String? = null
        val linked = source.withDocumentAnchorClicks { anchor, _ -> clickedAnchor = anchor }
        val span = linked.getSpans(0, linked.length, ClickableSpan::class.java).single()

        span.onClick(BookDocumentTextView(RuntimeEnvironment.getApplication()))

        assertEquals("note", clickedAnchor)
        assertTrue(linked.getSpans(0, linked.length, URLSpan::class.java).isEmpty())
    }

    @Test
    fun `safe external URL span dispatches an explicit external link action`() {
        val source = SpannableString("Open source").apply {
            setSpan(URLSpan("https://example.com/chapter"), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        var openedUrl: String? = null
        val linked = source.withDocumentLinkClicks(
            onAnchorClick = { _, _ -> error("External link must not dispatch an anchor") },
            onExternalLinkClick = { openedUrl = it },
        )
        val span = linked.getSpans(0, linked.length, ClickableSpan::class.java).single()

        span.onClick(BookDocumentTextView(RuntimeEnvironment.getApplication()))

        assertEquals("https://example.com/chapter", openedUrl)
        assertTrue(linked.getSpans(0, linked.length, URLSpan::class.java).isEmpty())
    }

    @Test
    fun `mixed-case external scheme dispatches explicitly and unhandled spans are removed`() {
        val source = SpannableString("Uppercase and unsafe").apply {
            setSpan(URLSpan("HTTPS://example.com/chapter"), 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(URLSpan("mailto:test@example.com"), 14, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        var openedUrl: String? = null
        val linked = source.withDocumentLinkClicks(
            onAnchorClick = { _, _ -> error("External link must not dispatch an anchor") },
            onExternalLinkClick = { openedUrl = it },
        )

        val clickable = linked.getSpans(0, linked.length, ClickableSpan::class.java).single()
        clickable.onClick(BookDocumentTextView(RuntimeEnvironment.getApplication()))

        assertEquals("HTTPS://example.com/chapter", openedUrl)
        assertTrue(linked.getSpans(0, linked.length, URLSpan::class.java).isEmpty())
        assertEquals(0, linked.getSpans(14, linked.length, ClickableSpan::class.java).size)
    }

    @Test
    fun `text view passes plain prose taps through while consuming anchor taps`() {
        var anchorClicked = false
        val text = SpannableString("Link then plain prose").apply {
            setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        anchorClicked = true
                    }
                },
                0,
                4,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        val view = laidOutTextView(text)
        val textY = (view.layout.getLineTop(0) + view.layout.getLineBottom(0)) / 2f
        val plainDown = event(view.layout.getPrimaryHorizontal(10), textY, MotionEvent.ACTION_DOWN)
        val linkDown = event(view.layout.getPrimaryHorizontal(2), textY, MotionEvent.ACTION_DOWN)
        val linkUp = event(view.layout.getPrimaryHorizontal(2), textY, MotionEvent.ACTION_UP)

        val plainHandled = view.dispatchTouchEvent(plainDown)
        val linkDownHandled = view.dispatchTouchEvent(linkDown)
        val linkUpHandled = view.dispatchTouchEvent(linkUp)

        listOf(plainDown, linkDown, linkUp).forEach(MotionEvent::recycle)
        assertFalse(plainHandled)
        assertTrue(linkDownHandled)
        assertTrue(linkUpHandled)
        assertTrue(anchorClicked)
    }

    private fun laidOutTextView(text: SpannableString): TextView {
        return BookDocumentTextView(RuntimeEnvironment.getApplication()).apply {
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

    private fun measuredTextView(
        text: SpannableString,
        trimTerminalLine: Boolean = false,
    ): BookDocumentTextView {
        return BookDocumentTextView(RuntimeEnvironment.getApplication()).apply {
            includeFontPadding = false
            textSize = 24f
            setLineSpacing(0f, 1.5f)
            setText(text, TextView.BufferType.SPANNABLE)
            this.trimTerminalLine = trimTerminalLine
            measure(
                MeasureSpec.makeMeasureSpec(600, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
        }
    }

    private fun event(x: Float, y: Float, action: Int): MotionEvent =
        MotionEvent.obtain(0, 0, action, x, y, 0)
}
