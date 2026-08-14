package mihon.entry.interactions.book.document.reader

import android.content.Context
import android.view.ActionMode
import android.view.View
import android.widget.FrameLayout

/** Intercepts floating selection action modes before they reach Android's window decor. */
internal class BookSelectionActionModeHost(
    context: Context,
    private val avoidance: BookSelectionActionModeAvoidance,
) : FrameLayout(context) {
    override fun startActionModeForChild(
        originalView: View,
        callback: ActionMode.Callback,
        type: Int,
    ): ActionMode? {
        val resolvedCallback = if (type == ActionMode.TYPE_FLOATING) {
            avoidance.wrap(callback)
        } else {
            callback
        }
        return super.startActionModeForChild(originalView, resolvedCallback, type)
    }
}
