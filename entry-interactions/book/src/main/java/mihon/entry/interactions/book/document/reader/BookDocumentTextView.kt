package mihon.entry.interactions.book.document.reader

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.text.Selection
import android.text.Spannable
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import mihon.entry.interactions.book.document.model.BookDocumentLinkTarget

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
    internal var documentSectionKey: String? = null
    internal var appliedDocumentTextIdentity: String? = null
    internal var onDocumentAnchorClick: ((String, TextView) -> Unit)? = null
    internal var onDocumentExternalLinkClick: ((String) -> Unit)? = null

    init {
        applyBookDocumentTextLayoutPolicy()
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
                if (trackingNonLinkTap && isShortTap && buffer?.clickableSpanAt(this, event) == null) {
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
        publishSelection(selStart, selEnd, clearWhenEmpty = true)
    }

    internal fun updateDocumentText(identity: String, linkedText: Spanned) {
        val sameContent = appliedDocumentTextIdentity == identity && text?.toString() == linkedText.toString()
        if (sameContent) return
        clearOwnedSelection()
        text = linkedText
        appliedDocumentTextIdentity = identity
    }

    internal fun dispatchDocumentLink(target: BookDocumentLinkTarget): Boolean {
        return when (target) {
            is BookDocumentLinkTarget.Anchor -> {
                val callback = onDocumentAnchorClick ?: return false
                callback(target.fragment, this)
                true
            }
            is BookDocumentLinkTarget.External -> {
                val callback = onDocumentExternalLinkClick ?: return false
                callback(target.url)
                true
            }
        }
    }

    internal fun refreshOwnedSelectionAnchor() {
        publishSelection(selectionStart, selectionEnd, clearWhenEmpty = false)
    }

    private fun publishSelection(selStart: Int, selEnd: Int, clearWhenEmpty: Boolean) {
        val interaction = selectionInteraction ?: BookDocumentTextInteraction.Disabled
        if (!interaction.observeSelections) return
        if (selStart < 0 || selEnd <= selStart) {
            if (clearWhenEmpty) interaction.onSelection(BookDocumentTextSelection.Cleared(selectionOwnerIdentity))
            return
        }
        val selectedText = text?.toString()?.substring(selStart, selEnd) ?: return
        if (selectedText.isBlank()) {
            if (clearWhenEmpty) interaction.onSelection(BookDocumentTextSelection.Cleared(selectionOwnerIdentity))
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
        intArrayOf(android.R.attr.textColorHighlight, android.R.attr.colorAccent),
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
