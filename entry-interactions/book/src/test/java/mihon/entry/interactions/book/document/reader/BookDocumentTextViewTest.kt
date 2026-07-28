package mihon.entry.interactions.book.document.reader

import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.geometry.Offset
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
    fun `selectable text forwards plain prose taps while consuming anchor taps`() {
        var anchorClicked = false
        var plainTapFraction: Float? = null
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
        val view = laidOutTextView(text) as BookDocumentTextView
        view.selectionInteraction = BookDocumentTextInteraction(
            observeSelections = false,
            rootPositionInWindow = Offset.Zero,
            onSelection = {},
            onNonLinkTap = { x, width -> plainTapFraction = x / width },
        )
        val textY = (view.layout.getLineTop(0) + view.layout.getLineBottom(0)) / 2f
        val plainDown = event(view.layout.getPrimaryHorizontal(10), textY, MotionEvent.ACTION_DOWN)
        val plainUp = event(view.layout.getPrimaryHorizontal(10), textY, MotionEvent.ACTION_UP)
        val linkDown = event(view.layout.getPrimaryHorizontal(2), textY, MotionEvent.ACTION_DOWN)
        val linkUp = event(view.layout.getPrimaryHorizontal(2), textY, MotionEvent.ACTION_UP)

        val plainDownHandled = view.dispatchTouchEvent(plainDown)
        val plainUpHandled = view.dispatchTouchEvent(plainUp)
        val linkDownHandled = view.dispatchTouchEvent(linkDown)
        val linkUpHandled = view.dispatchTouchEvent(linkUp)

        listOf(plainDown, plainUp, linkDown, linkUp).forEach(MotionEvent::recycle)
        assertTrue(plainDownHandled)
        assertTrue(plainUpHandled)
        assertTrue(linkDownHandled)
        assertTrue(linkUpHandled)
        assertTrue(anchorClicked)
        assertTrue(plainTapFraction != null)
    }

    @Test
    fun `reader text remains selectable without translation observation`() {
        val view = laidOutTextView(SpannableString("Independent selection")) as BookDocumentTextView
        var emitted = false
        view.selectionInteraction = BookDocumentTextInteraction(
            observeSelections = false,
            rootPositionInWindow = Offset.Zero,
            onSelection = { emitted = true },
            onNonLinkTap = { _, _ -> },
        )

        Selection.setSelection(view.text as Spannable, 0, 11)

        assertTrue(view.isTextSelectable)
        assertEquals("Independent", view.text.subSequence(view.selectionStart, view.selectionEnd).toString())
        assertFalse(emitted)
    }

    @Test
    fun `selectable text emits the exact substring and reader-root bounds`() {
        val view = laidOutTextView(SpannableString("  exact selection  ")) as BookDocumentTextView
        var emitted: BookDocumentTextSelection? = null
        view.selectionInteraction = BookDocumentTextInteraction(
            observeSelections = true,
            rootPositionInWindow = Offset.Zero,
            onSelection = { emitted = it },
            onNonLinkTap = { _, _ -> },
        )
        view.setTextIsSelectable(true)

        android.text.Selection.setSelection(view.text as android.text.Spannable, 2, 17)

        val selection = emitted as BookDocumentTextSelection.Changed
        assertEquals("exact selection", selection.text)
        assertTrue(selection.boundsInReaderRoot.width() > 0f)
        assertTrue(selection.boundsInReaderRoot.height() > 0f)
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
