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
    private var activeActionMode: ActionMode? = null

    fun wrap(callback: ActionMode.Callback): ActionMode.Callback = AvoidingCallback(callback)

    fun updateBounds(bounds: ComposeRect?) {
        if (boundsInWindow == bounds) return
        val previousBounds = boundsInWindow
        boundsInWindow = bounds
        val selectionBounds = selectionBoundsInWindow ?: return
        // Android briefly hides a floating action mode whenever its content rect moves. A popup
        // resize on the same side cannot create a new collision, so keep the native placement.
        if (requiresActionModeReposition(selectionBounds, previousBounds, bounds)) {
            activeActionMode?.invalidateContentRect()
        }
    }

    fun clear() {
        boundsInWindow = null
        selectionBoundsInWindow = null
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
): Boolean = previousPopupBounds.placementRelativeTo(selectionBounds) !=
    popupBounds.placementRelativeTo(selectionBounds)

private fun ComposeRect?.placementRelativeTo(selectionBounds: ComposeRect): BookSelectionPopupPlacement? {
    if (this == null || !hasFiniteBounds() || !selectionBounds.hasFiniteBounds()) return null
    return when {
        bottom <= selectionBounds.top -> BookSelectionPopupPlacement.Above
        top >= selectionBounds.bottom -> BookSelectionPopupPlacement.Below
        else -> BookSelectionPopupPlacement.Overlapping
    }
}

private enum class BookSelectionPopupPlacement {
    Above,
    Below,
    Overlapping,
}

private fun ComposeRect.hasFiniteBounds(): Boolean =
    left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() && !isEmpty
