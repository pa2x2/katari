package mihon.entry.interactions.book.document.reader

import android.content.Context
import android.graphics.Color
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
import android.view.ViewConfiguration
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
    val linkedText = remember(text, trimTerminalLine) {
        val displayText = if (trimTerminalLine) {
            text.withoutTerminalLayoutLine()
        } else {
            text
        }
        displayText.withDocumentLinkClicks(
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
                setBackgroundColor(Color.TRANSPARENT)
                movementMethod = LinkMovementMethod.getInstance()
                applyVisibleSelectionHighlight()
                onViewChanged(this)
            }
        },
        update = { view ->
            view.selectionInteraction = interaction
            if (view.text !== linkedText) {
                view.clearOwnedSelection()
                view.text = linkedText
            }
            view.movementMethod = LinkMovementMethod.getInstance()
            view.applyStyle(style)
            view.applyTerminalLineSpacing(trimTerminalLine)
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
    private val tapTouchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val basePaddingLeft = paddingLeft
    private val basePaddingTop = paddingTop
    private val basePaddingRight = paddingRight
    private val basePaddingBottom = paddingBottom
    private var trackingNonLinkTap = false
    private var readerTapBlockedAtDown = false
    private var nonLinkTapDownX = 0f
    private var nonLinkTapDownY = 0f
    internal var selectionInteraction: BookDocumentTextInteraction? = null
    internal var appliedStyle: BookDocumentTextStyle? = null

    init {
        setTextIsSelectable(true)
    }

    internal fun applyTerminalLineSpacing(trimTerminalLine: Boolean) {
        val fontHeight = paint.getFontMetricsInt(null)
        val terminalSpacing = if (trimTerminalLine) {
            (lineHeight - fontHeight).coerceAtLeast(0)
        } else {
            0
        }
        val targetBottomPadding = basePaddingBottom + terminalSpacing
        if (paddingBottom == targetBottomPadding) return
        setPadding(basePaddingLeft, basePaddingTop, basePaddingRight, targetBottomPadding)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val interaction = selectionInteraction ?: BookDocumentTextInteraction.Disabled
        val buffer = text as? Spanned
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                trackingNonLinkTap = buffer?.clickableSpanAt(this, event) == null
                readerTapBlockedAtDown = interaction.isReaderTapBlocked()
                nonLinkTapDownX = event.x
                nonLinkTapDownY = event.y
            }
            MotionEvent.ACTION_MOVE -> updateNonLinkTapTracking(event)
            MotionEvent.ACTION_UP -> {
                updateNonLinkTapTracking(event)
                val isShortTap =
                    event.eventTime - event.downTime < ViewConfiguration.getLongPressTimeout()
                if (
                    trackingNonLinkTap &&
                    isShortTap &&
                    buffer?.clickableSpanAt(this, event) == null
                ) {
                    if (readerTapBlockedAtDown) {
                        interaction.onBlockedReaderTap()
                    } else {
                        interaction.onNonLinkTap(event.x, width.toFloat())
                    }
                }
                trackingNonLinkTap = false
                readerTapBlockedAtDown = false
            }
            MotionEvent.ACTION_CANCEL -> {
                trackingNonLinkTap = false
                readerTapBlockedAtDown = false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateNonLinkTapTracking(event: MotionEvent) {
        if (!trackingNonLinkTap) return
        val deltaX = event.x - nonLinkTapDownX
        val deltaY = event.y - nonLinkTapDownY
        if (deltaX * deltaX + deltaY * deltaY > tapTouchSlop * tapTouchSlop) {
            trackingNonLinkTap = false
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        val interaction = selectionInteraction ?: BookDocumentTextInteraction.Disabled
        if (!interaction.observeSelections) return
        if (selStart < 0 || selEnd <= selStart) {
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
            ?.takeIf(BookDocumentTextInteraction::observeSelections)
            ?.onSelection
            ?.invoke(BookDocumentTextSelection.Cleared(selectionOwnerIdentity))
        (text as? Spannable)?.let(Selection::removeSelection)
    }
}

internal fun BookDocumentTextView.applyVisibleSelectionHighlight() {
    val attributes = context.obtainStyledAttributes(
        intArrayOf(
            android.R.attr.textColorHighlight,
            android.R.attr.colorAccent,
        ),
    )
    highlightColor = try {
        attributes.getColor(0, Color.TRANSPARENT)
            .takeIf { Color.alpha(it) > 0 }
            ?: attributes.getColor(1, DEFAULT_SELECTION_ACCENT).withSelectionAlpha()
    } finally {
        attributes.recycle()
    }
}

private fun Int.withSelectionAlpha(): Int = Color.argb(
    SELECTION_HIGHLIGHT_ALPHA,
    Color.red(this),
    Color.green(this),
    Color.blue(this),
)

internal data class BookDocumentTextInteraction(
    val observeSelections: Boolean,
    val rootPositionInWindow: Offset,
    val onSelection: (BookDocumentTextSelection) -> Unit,
    val isReaderTapBlocked: () -> Boolean,
    val onBlockedReaderTap: () -> Unit,
    val onNonLinkTap: (x: Float, width: Float) -> Unit,
) {
    companion object {
        val Disabled = BookDocumentTextInteraction(
            observeSelections = false,
            rootPositionInWindow = Offset.Zero,
            onSelection = {},
            isReaderTapBlocked = { false },
            onBlockedReaderTap = {},
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

private const val SELECTION_HIGHLIGHT_ALPHA = 0x66
private const val DEFAULT_SELECTION_ACCENT = 0xFF3F51B5.toInt()

internal fun Spanned.withoutTerminalLayoutLine(): Spanned {
    if (!endsWith('\n')) return this
    return SpannableString(subSequence(0, length - 1))
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
