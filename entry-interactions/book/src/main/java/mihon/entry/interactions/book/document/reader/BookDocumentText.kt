package mihon.entry.interactions.book.document.reader

import android.content.Context
import android.graphics.Path
import android.graphics.RectF
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.AndroidView
import mihon.entry.interactions.book.document.model.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.model.toBookDocumentLinkTarget

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
    onExternalLinkClick: (String) -> Unit = {},
    onViewChanged: (TextView?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = LocalBookDocumentTextInteraction.current
    val currentAnchorClick by rememberUpdatedState(onAnchorClick)
    val currentExternalLinkClick by rememberUpdatedState(onExternalLinkClick)
    val linkedText = remember(text) {
        text.withDocumentLinkClicks(
            onAnchorClick = { anchorId, view -> currentAnchorClick(anchorId, view) },
            onExternalLinkClick = { url -> currentExternalLinkClick(url) },
        )
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
            view.selectionInteraction = interaction
            if (view.text !== linkedText) {
                view.clearOwnedSelection()
                view.text = linkedText
            }
            if (view.isTextSelectable != interaction.enabled) {
                view.setTextIsSelectable(interaction.enabled)
            }
            view.movementMethod = LinkMovementMethod.getInstance()
            view.trimTerminalLine = trimTerminalLine
            view.applyStyle(style)
            onViewChanged(view)
        },
        onRelease = { view ->
            view.clearOwnedSelection()
            onViewChanged(null)
            view.text = null
        },
    )
}

internal class BookDocumentTextView(context: Context) : TextView(context) {
    private val selectionOwnerIdentity = "text-view-${System.identityHashCode(this)}"
    internal var selectionInteraction: BookDocumentTextInteraction? = null
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
        val interaction = selectionInteraction ?: BookDocumentTextInteraction.Disabled
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
            val buffer = text as? Spanned
            if (buffer?.clickableSpanAt(this, event) == null) {
                if (!interaction.enabled) {
                    (buffer as? Spannable)?.let(Selection::removeSelection)
                    return false
                }
                val handled = super.onTouchEvent(event)
                if (
                    event.actionMasked == MotionEvent.ACTION_UP &&
                    selectionStart == selectionEnd
                ) {
                    interaction.onNonLinkTap(event.x, width.toFloat())
                }
                return handled
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        val interaction = selectionInteraction ?: BookDocumentTextInteraction.Disabled
        if (!interaction.enabled || selStart < 0 || selEnd <= selStart) {
            interaction.onSelection(BookDocumentTextSelection.Cleared(selectionOwnerIdentity))
            return
        }
        val selectedText = text?.subSequence(selStart, selEnd)?.toString() ?: return
        if (selectedText.isBlank()) {
            interaction.onSelection(BookDocumentTextSelection.Cleared(selectionOwnerIdentity))
            return
        }
        val layout = layout ?: return
        val path = Path()
        layout.getSelectionPath(selStart, selEnd, path)
        val bounds = RectF()
        path.computeBounds(bounds, true)
        if (bounds.isEmpty) return
        val position = IntArray(2)
        getLocationInWindow(position)
        bounds.offset(
            position[0] + totalPaddingLeft - scrollX.toFloat(),
            position[1] + totalPaddingTop - scrollY.toFloat(),
        )
        val root = interaction.rootPositionInWindow
        bounds.offset(-root.x, -root.y)
        interaction.onSelection(
            BookDocumentTextSelection.Changed(
                ownerIdentity = selectionOwnerIdentity,
                identity = "$selectionOwnerIdentity:$selStart:$selEnd:${selectedText.hashCode()}",
                text = selectedText,
                boundsInReaderRoot = bounds,
            ),
        )
    }

    internal fun clearOwnedSelection() {
        selectionInteraction
            ?.onSelection
            ?.invoke(BookDocumentTextSelection.Cleared(selectionOwnerIdentity))
        (text as? Spannable)?.let(Selection::removeSelection)
    }
}

internal data class BookDocumentTextInteraction(
    val enabled: Boolean,
    val rootPositionInWindow: Offset,
    val onSelection: (BookDocumentTextSelection) -> Unit,
    val onNonLinkTap: (x: Float, width: Float) -> Unit,
) {
    companion object {
        val Disabled = BookDocumentTextInteraction(
            enabled = false,
            rootPositionInWindow = Offset.Zero,
            onSelection = {},
            onNonLinkTap = { _, _ -> },
        )
    }
}

internal sealed interface BookDocumentTextSelection {
    val ownerIdentity: String

    data class Changed(
        override val ownerIdentity: String,
        val identity: String,
        val text: String,
        val boundsInReaderRoot: RectF,
    ) : BookDocumentTextSelection

    data class Cleared(
        override val ownerIdentity: String,
    ) : BookDocumentTextSelection
}

internal val LocalBookDocumentTextInteraction = compositionLocalOf { BookDocumentTextInteraction.Disabled }

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
): Spanned = withDocumentLinkClicks(onAnchorClick, onExternalLinkClick = {})

internal fun Spanned.withDocumentLinkClicks(
    onAnchorClick: (String, TextView) -> Unit,
    onExternalLinkClick: (String) -> Unit,
): Spanned {
    val spannable = SpannableString(this)
    spannable.getSpans(0, spannable.length, URLSpan::class.java).forEach { span ->
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        val flags = spannable.getSpanFlags(span)
        spannable.removeSpan(span)
        val target = span.url.toBookDocumentLinkTarget() ?: return@forEach
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    when (target) {
                        is BookDocumentLinkTarget.Anchor ->
                            (widget as? TextView)?.let { onAnchorClick(target.fragment, it) }
                        is BookDocumentLinkTarget.External -> onExternalLinkClick(target.url)
                    }
                }
            },
            start,
            end,
            flags.takeIf { it != 0 } ?: Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    return spannable
}
