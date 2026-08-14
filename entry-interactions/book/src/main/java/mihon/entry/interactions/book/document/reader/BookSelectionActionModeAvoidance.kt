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
    private var activeActionMode: ActionMode? = null

    fun wrap(callback: ActionMode.Callback): ActionMode.Callback = AvoidingCallback(callback)

    fun updateBounds(bounds: ComposeRect?) {
        if (boundsInWindow == bounds) return
        boundsInWindow = bounds
        activeActionMode?.invalidateContentRect()
    }

    fun clear() {
        boundsInWindow = null
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
                if (activeActionMode === mode) activeActionMode = null
            }
        }

        override fun onGetContentRect(mode: ActionMode, view: View?, outRect: Rect) {
            val callback = delegate as? ActionMode.Callback2
            if (callback != null) {
                callback.onGetContentRect(mode, view, outRect)
            } else {
                super.onGetContentRect(mode, view, outRect)
            }
            val avoidanceBounds = boundsInWindow ?: return
            if (!avoidanceBounds.hasFiniteBounds()) return
            val locationInWindow = IntArray(2)
            view?.getLocationInWindow(locationInWindow)
            outRect.union(
                floor(avoidanceBounds.left - locationInWindow[0]).toInt(),
                floor(avoidanceBounds.top - locationInWindow[1]).toInt(),
                ceil(avoidanceBounds.right - locationInWindow[0]).toInt(),
                ceil(avoidanceBounds.bottom - locationInWindow[1]).toInt(),
            )
        }
    }
}

private fun ComposeRect.hasFiniteBounds(): Boolean =
    left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() && !isEmpty
