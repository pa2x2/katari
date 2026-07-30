package mihon.entry.interactions.book.document.reader

import android.text.SpannableString
import android.view.MotionEvent
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.geometry.Offset
import org.robolectric.RuntimeEnvironment
internal abstract class BookDocumentTextViewFixture {
    protected fun interaction(
        rootPositionInWindow: Offset,
        onSelection: (BookDocumentTextSelection) -> Unit,
    ) = BookDocumentTextInteraction(
        observeSelections = true,
        rootPositionInWindow = rootPositionInWindow,
        onSelection = onSelection,
        isReaderTapBlocked = { false },
        onBlockedReaderTap = {},
        onNonLinkTap = { _, _ -> },
    )

    protected fun laidOutTextView(text: SpannableString): TextView {
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

    protected fun measuredTextView(
        text: SpannableString,
        trimTerminalLine: Boolean = false,
    ): BookDocumentTextView {
        return BookDocumentTextView(RuntimeEnvironment.getApplication()).apply {
            layoutParams = ViewGroup.LayoutParams(600, ViewGroup.LayoutParams.WRAP_CONTENT)
            includeFontPadding = false
            textSize = 24f
            setLineSpacing(0f, 1.5f)
            setText(
                if (trimTerminalLine) text.withoutTerminalLayoutLine() else text,
                TextView.BufferType.SPANNABLE,
            )
            applyTerminalLineSpacing(trimTerminalLine)
            measure(
                MeasureSpec.makeMeasureSpec(600, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
        }
    }

    protected fun event(
        x: Float,
        y: Float,
        action: Int,
        eventTime: Long = 0,
    ): MotionEvent = MotionEvent.obtain(0, eventTime, action, x, y, 0)
}
