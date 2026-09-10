package mihon.entry.interactions.book.document.reader

import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlin.math.ceil
import kotlin.math.floor
import androidx.compose.ui.geometry.Rect as ComposeRect

/** Extends Android's native selection anchor around Katari-owned popup content. */
internal class BookSelectionActionModeAvoidance {
    private var boundsInWindow: ComposeRect? = null
    private var selectionBoundsInWindow: ComposeRect? = null
    private var viewportInWindow: ComposeRect? = null
    private var toolbarHeightPx: Float = 0f
    private var activeActionMode: ActionMode? = null

    fun wrap(callback: ActionMode.Callback): ActionMode.Callback = AvoidingCallback(callback)

    fun updateBounds(bounds: ComposeRect?) {
        if (boundsInWindow == bounds) return
        val previousBounds = boundsInWindow
        boundsInWindow = bounds
        val selectionBounds = selectionBoundsInWindow ?: return
        // Android briefly hides a floating action mode whenever its content rect moves. A popup
        // resize on the opposite side of the native toolbar cannot create a new collision, so
        // keep the native placement. A resize towards the toolbar must reposition, otherwise the
        // toolbar keeps its previously measured position and overlaps the grown popup. This is
        // most visible in paged mode with selections close to the top/bottom edge, where the
        // toolbar is forced onto the popup side. Compare the united content rects so a popup
        // change that leaves the toolbar edge stable (for example growing into the selection
        // without moving the union top/bottom) does not blink the native menu.
        if (
            requiresActionModeReposition(
                selectionBounds,
                previousBounds,
                bounds,
                viewportInWindow,
                toolbarHeightPx,
            )
        ) {
            activeActionMode?.invalidateContentRect()
        }
    }

    fun clear() {
        boundsInWindow = null
        selectionBoundsInWindow = null
        viewportInWindow = null
        toolbarHeightPx = 0f
        activeActionMode = null
    }

    private inner class AvoidingCallback(
        private val delegate: ActionMode.Callback,
    ) : ActionMode.Callback2() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val created = delegate.onCreateActionMode(mode, menu)
            if (created) activeActionMode = mode
            return created
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean =
            delegate.onPrepareActionMode(mode, menu)

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
            delegate.onActionItemClicked(mode, item)

        override fun onDestroyActionMode(mode: ActionMode) {
            try {
                delegate.onDestroyActionMode(mode)
            } finally {
                if (activeActionMode === mode) {
                    activeActionMode = null
                    selectionBoundsInWindow = null
                }
            }
        }

        override fun onGetContentRect(mode: ActionMode, view: View?, outRect: Rect) {
            val callback = delegate as? ActionMode.Callback2
            if (callback != null) {
                callback.onGetContentRect(mode, view, outRect)
            } else {
                super.onGetContentRect(mode, view, outRect)
            }
            val locationInWindow = IntArray(2)
            view?.getLocationInWindow(locationInWindow)
            selectionBoundsInWindow = ComposeRect(
                left = (outRect.left + locationInWindow[0]).toFloat(),
                top = (outRect.top + locationInWindow[1]).toFloat(),
                right = (outRect.right + locationInWindow[0]).toFloat(),
                bottom = (outRect.bottom + locationInWindow[1]).toFloat(),
            )
            if (view != null) {
                captureViewportInWindow(view)?.let { viewportInWindow = it }
                toolbarHeightPx = TOOLBAR_HEIGHT_DP * view.resources.displayMetrics.density
            }
            val avoidanceBounds = boundsInWindow ?: return
            if (!avoidanceBounds.hasFiniteBounds()) return
            outRect.union(
                floor(avoidanceBounds.left - locationInWindow[0]).toInt(),
                floor(avoidanceBounds.top - locationInWindow[1]).toInt(),
                ceil(avoidanceBounds.right - locationInWindow[0]).toInt(),
                ceil(avoidanceBounds.bottom - locationInWindow[1]).toInt(),
            )
        }
    }
}

internal fun requiresActionModeReposition(
    selectionBounds: ComposeRect,
    previousPopupBounds: ComposeRect?,
    popupBounds: ComposeRect?,
    viewportInWindow: ComposeRect? = null,
    toolbarHeightPx: Float = 0f,
): Boolean {
    if (!selectionBounds.hasFiniteBounds()) {
        return previousPopupBounds != popupBounds
    }
    val previousContent = selectionBounds.unionedWith(previousPopupBounds)
    val content = selectionBounds.unionedWith(popupBounds)
    if (!previousContent.hasFiniteBounds() || !content.hasFiniteBounds()) {
        return previousPopupBounds != popupBounds
    }
    // Horizontal-only changes keep the toolbar's vertical anchor stable. Ignoring them avoids
    // blinking the native menu for width growth that cannot create a new vertical collision.
    val topChanged = kotlin.math.abs(content.top - previousContent.top) > VERTICAL_TOLERANCE_PX
    val bottomChanged =
        kotlin.math.abs(content.bottom - previousContent.bottom) > VERTICAL_TOLERANCE_PX
    if (!topChanged && !bottomChanged) return false
    // A moved anchor (scroll/page change) shifts the whole content rect and must follow.
    if (topChanged && bottomChanged) return true
    // A single-edge change is a popup growth/shrink. Only growth towards the toolbar side can
    // overlap it; shrink away from the toolbar keeps the placement stable without blinking.
    // Dismissal is the exception: when the popup is gone the toolbar must return to the
    // selection anchor when it had been pushed onto the popup side, otherwise it stays
    // detached far from the selection.
    val viewport = viewportInWindow?.takeIf { it.hasFiniteBounds() } ?: return true
    val availableAbove = previousContent.top - viewport.top
    val availableBelow = viewport.bottom - previousContent.bottom
    val toolbarAbove = if (availableAbove >= toolbarHeightPx) {
        true
    } else if (availableBelow >= toolbarHeightPx) {
        false
    } else {
        true
    }
    if (popupBounds == null && previousPopupBounds?.hasFiniteBounds() == true) {
        return if (topChanged) toolbarAbove else !toolbarAbove
    }
    return if (topChanged) {
        toolbarAbove && content.top < previousContent.top - VERTICAL_TOLERANCE_PX
    } else {
        !toolbarAbove && content.bottom > previousContent.bottom + VERTICAL_TOLERANCE_PX
    }
}

private fun captureViewportInWindow(view: View): ComposeRect? {
    val frame = Rect()
    view.getWindowVisibleDisplayFrame(frame)
    if (frame.isEmpty) return null
    val locationInWindow = IntArray(2)
    view.getLocationInWindow(locationInWindow)
    val locationOnScreen = IntArray(2)
    view.getLocationOnScreen(locationOnScreen)
    val windowLeftOnScreen = locationOnScreen[0] - locationInWindow[0]
    val windowTopOnScreen = locationOnScreen[1] - locationInWindow[1]
    return ComposeRect(
        left = (frame.left - windowLeftOnScreen).toFloat(),
        top = (frame.top - windowTopOnScreen).toFloat(),
        right = (frame.right - windowLeftOnScreen).toFloat(),
        bottom = (frame.bottom - windowTopOnScreen).toFloat(),
    )
}

// Floating toolbars prefer the space above the content rect and fall back below. Mirror that
// preference with a conservative main-panel height so edge selections force a reposition while
// centered selections stay stable.
private const val TOOLBAR_HEIGHT_DP = 72f
private const val VERTICAL_TOLERANCE_PX = 1f

private fun ComposeRect.unionedWith(other: ComposeRect?): ComposeRect {
    if (other == null || !other.hasFiniteBounds()) return this
    return ComposeRect(
        left = minOf(left, other.left),
        top = minOf(top, other.top),
        right = maxOf(right, other.right),
        bottom = maxOf(bottom, other.bottom),
    )
}

private fun ComposeRect.hasFiniteBounds(): Boolean =
    left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() && !isEmpty
