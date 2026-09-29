package eu.kanade.presentation.library.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState

/**
 * Lets the Library tab bring the visible page back to its top when the tab is reselected.
 *
 * Only the page the user is looking at binds itself; [scrollToTop] reports whether anything moved so the caller can
 * fall back to its secondary reselect action once the page is already at the top.
 */
@Stable
class LibraryScrollToTopTarget {
    private var handler: (suspend () -> Boolean)? = null

    suspend fun scrollToTop(): Boolean = handler?.invoke() ?: false

    internal fun bind(handler: suspend () -> Boolean) {
        this.handler = handler
    }

    internal fun unbind(handler: suspend () -> Boolean) {
        if (this.handler === handler) this.handler = null
    }
}

@Composable
internal fun BindScrollToTop(
    target: LibraryScrollToTopTarget?,
    state: ScrollableState,
    scrollToTop: suspend () -> Unit,
) {
    if (target == null) return
    val currentScrollToTop = rememberUpdatedState(scrollToTop)
    DisposableEffect(target, state) {
        val handler: suspend () -> Boolean = {
            if (state.canScrollBackward) {
                currentScrollToTop.value()
                true
            } else {
                false
            }
        }
        target.bind(handler)
        onDispose { target.unbind(handler) }
    }
}
