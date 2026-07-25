package mihon.entry.interactions.book.document.reader

import android.content.Context
import android.graphics.Typeface
import android.text.Layout
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun BookDocumentText(
    text: Spanned,
    textColor: Int,
    textSizeSp: Float,
    typeface: Typeface,
    lineSpacingMultiplier: Float,
    textAlignment: Int,
    justificationMode: Int,
    trimTerminalLine: Boolean = false,
    onAnchorClick: (String, TextView) -> Unit,
    onViewChanged: (TextView?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentAnchorClick by rememberUpdatedState(onAnchorClick)
    val linkedText = remember(text) {
        text.withDocumentAnchorClicks { anchorId, view ->
            currentAnchorClick(anchorId, view)
        }
    }
    val style = BookDocumentTextStyle(
        textColor = textColor,
        textSizeSp = textSizeSp,
        typeface = typeface,
        lineSpacingMultiplier = lineSpacingMultiplier,
        textAlignment = textAlignment,
        justificationMode = justificationMode,
    )
    AndroidView(
        modifier = modifier,
        factory = { context ->
            BookDocumentTextView(context).apply {
                includeFontPadding = false
                setTextIsSelectable(false)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                movementMethod = LinkMovementMethod.getInstance()
                highlightColor = android.graphics.Color.TRANSPARENT
                onViewChanged(this)
            }
        },
        update = { view ->
            if (view.text !== linkedText) view.text = linkedText
            view.trimTerminalLine = trimTerminalLine
            view.applyStyle(style)
            onViewChanged(view)
        },
        onRelease = { view ->
            onViewChanged(null)
            view.text = null
        },
    )
}

internal class BookDocumentTextView(context: Context) : TextView(context) {
    internal var appliedStyle: BookDocumentTextStyle? = null
    internal var trimTerminalLine: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val currentLayout = layout
        if (
            trimTerminalLine &&
            text.endsWith('\n') &&
            currentLayout != null &&
            currentLayout.lineCount > 1
        ) {
            val terminalLineHeight = currentLayout.height - currentLayout.getLineTop(currentLayout.lineCount - 1)
            setMeasuredDimension(measuredWidth, (measuredHeight - terminalLineHeight).coerceAtLeast(0))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
            val buffer = text as? Spanned
            if (buffer?.clickableSpanAt(this, event) == null) {
                (buffer as? Spannable)?.let(Selection::removeSelection)
                return false
            }
        }
        return super.onTouchEvent(event)
    }
}

internal data class BookDocumentTextStyle(
    val textColor: Int,
    val textSizeSp: Float,
    val typeface: Typeface,
    val lineSpacingMultiplier: Float,
    val textAlignment: Int,
    val justificationMode: Int,
)

private fun BookDocumentTextView.applyStyle(style: BookDocumentTextStyle) {
    if (appliedStyle == style) return
    setTextColor(style.textColor)
    textSize = style.textSizeSp
    typeface = style.typeface
    setLineSpacing(0f, style.lineSpacingMultiplier)
    textAlignment = style.textAlignment
    justificationMode = style.justificationMode
    appliedStyle = style
}

private fun Spanned.clickableSpanAt(widget: TextView, event: MotionEvent): ClickableSpan? {
    val layout = widget.layout ?: return null
    val x = event.x - widget.totalPaddingLeft + widget.scrollX
    val y = event.y - widget.totalPaddingTop + widget.scrollY
    if (y < 0 || y > layout.height) return null

    val line = layout.getLineForVertical(y.toInt())
    if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) return null
    val offset = layout.getOffsetForHorizontal(line, x)
    return getSpans(offset, offset, ClickableSpan::class.java).firstOrNull()
}

internal fun Spanned.withDocumentAnchorClicks(
    onAnchorClick: (String, TextView) -> Unit,
): Spanned {
    val spannable = SpannableString(this)
    spannable.getSpans(0, spannable.length, URLSpan::class.java).forEach { span ->
        val anchorId = span.url.removePrefix("#").takeIf { span.url.startsWith("#") && it.isNotBlank() }
            ?: return@forEach
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        val flags = spannable.getSpanFlags(span)
        spannable.removeSpan(span)
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    (widget as? TextView)?.let { onAnchorClick(anchorId, it) }
                }
            },
            start,
            end,
            flags.takeIf { it != 0 } ?: Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    return spannable
}
