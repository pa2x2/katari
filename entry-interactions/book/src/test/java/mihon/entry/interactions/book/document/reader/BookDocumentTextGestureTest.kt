package mihon.entry.interactions.book.document.reader

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.ui.geometry.Offset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentTextGestureTest : BookDocumentTextViewFixture() {
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
            isReaderTapBlocked = { false },
            onBlockedReaderTap = {},
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
    fun `short prose tap preserves reader block captured at gesture start`() {
        val view = laidOutTextView(SpannableString("Selected prose remains tappable")) as BookDocumentTextView
        var readerTapBlocked = true
        var blockedTapCount = 0
        var readerTapForwarded = false
        view.selectionInteraction = BookDocumentTextInteraction(
            observeSelections = false,
            rootPositionInWindow = Offset.Zero,
            onSelection = {},
            isReaderTapBlocked = { readerTapBlocked },
            onBlockedReaderTap = { blockedTapCount += 1 },
            onNonLinkTap = { _, _ -> readerTapForwarded = true },
        )
        val x = view.layout.getPrimaryHorizontal(12)
        val y = (view.layout.getLineTop(0) + view.layout.getLineBottom(0)) / 2f
        val down = event(x, y, MotionEvent.ACTION_DOWN)
        val up = event(x, y, MotionEvent.ACTION_UP)

        view.dispatchTouchEvent(down)
        readerTapBlocked = false
        view.dispatchTouchEvent(up)

        down.recycle()
        up.recycle()
        assertEquals(1, blockedTapCount)
        assertFalse(readerTapForwarded)
    }

    @Test
    fun `local non-link action takes precedence over reader navigation`() {
        val view = laidOutTextView(SpannableString("Disclosure summary")) as BookDocumentTextView
        var localClickCount = 0
        var readerTapForwarded = false
        view.onDocumentNonLinkClick = { localClickCount += 1 }
        view.selectionInteraction = BookDocumentTextInteraction(
            observeSelections = false,
            rootPositionInWindow = Offset.Zero,
            onSelection = {},
            isReaderTapBlocked = { false },
            onBlockedReaderTap = {},
            onNonLinkTap = { _, _ -> readerTapForwarded = true },
        )
        val x = view.layout.getPrimaryHorizontal(5)
        val y = (view.layout.getLineTop(0) + view.layout.getLineBottom(0)) / 2f
        val down = event(x, y, MotionEvent.ACTION_DOWN)
        val up = event(x, y, MotionEvent.ACTION_UP)

        view.dispatchTouchEvent(down)
        view.dispatchTouchEvent(up)

        down.recycle()
        up.recycle()
        assertEquals(1, localClickCount)
        assertFalse(readerTapForwarded)
    }

    @Test
    fun `long press is not forwarded as reader navigation`() {
        val view = laidOutTextView(SpannableString("Long press selection")) as BookDocumentTextView
        var tapForwarded = false
        view.selectionInteraction = BookDocumentTextInteraction(
            observeSelections = false,
            rootPositionInWindow = Offset.Zero,
            onSelection = {},
            isReaderTapBlocked = { false },
            onBlockedReaderTap = {},
            onNonLinkTap = { _, _ -> tapForwarded = true },
        )
        val x = view.layout.getPrimaryHorizontal(5)
        val y = (view.layout.getLineTop(0) + view.layout.getLineBottom(0)) / 2f
        val down = event(x, y, MotionEvent.ACTION_DOWN)
        val up = event(
            x = x,
            y = y,
            action = MotionEvent.ACTION_UP,
            eventTime = ViewConfiguration.getLongPressTimeout().toLong(),
        )

        view.dispatchTouchEvent(down)
        view.dispatchTouchEvent(up)

        down.recycle()
        up.recycle()
        assertFalse(tapForwarded)
    }

    @Test
    fun `drag gesture is not forwarded as reader navigation`() {
        val view = laidOutTextView(SpannableString("Dragged selection")) as BookDocumentTextView
        var tapForwarded = false
        view.selectionInteraction = BookDocumentTextInteraction(
            observeSelections = false,
            rootPositionInWindow = Offset.Zero,
            onSelection = {},
            isReaderTapBlocked = { false },
            onBlockedReaderTap = {},
            onNonLinkTap = { _, _ -> tapForwarded = true },
        )
        val x = view.layout.getPrimaryHorizontal(5)
        val y = (view.layout.getLineTop(0) + view.layout.getLineBottom(0)) / 2f
        val dragDistance = ViewConfiguration.get(view.context).scaledTouchSlop * 2f
        val down = event(x, y, MotionEvent.ACTION_DOWN)
        val move = event(x + dragDistance, y, MotionEvent.ACTION_MOVE)
        val up = event(x + dragDistance, y, MotionEvent.ACTION_UP)

        view.dispatchTouchEvent(down)
        view.dispatchTouchEvent(move)
        view.dispatchTouchEvent(up)

        listOf(down, move, up).forEach(MotionEvent::recycle)
        assertFalse(tapForwarded)
    }
}
