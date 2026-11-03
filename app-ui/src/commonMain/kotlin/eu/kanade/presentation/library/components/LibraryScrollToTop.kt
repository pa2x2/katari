package eu.kanade.presentation.library.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState
import mihon.presentation.kit.navigation.rememberAppBarExpander

/**
 * Lets the Library tab bring the visible page back to its top when the tab is reselected.
 *
 * The visible page and anything heading it bind themselves; [scrollToTop] reports whether anything moved, the large
 * title included, so the caller can fall back to its secondary reselect action once everything is already at the top.
 */
@Stable
class LibraryScrollToTopTarget {
    private val handlers = mutableListOf<suspend () -> Boolean>()

    suspend fun scrollToTop(): Boolean {
        var moved = false
        handlers.toList().forEach { handler -> if (handler()) moved = true }
        return moved
    }

    internal fun bind(handler: suspend () -> Boolean) {
        handlers += handler
    }

    internal fun unbind(handler: suspend () -> Boolean) {
        handlers -= handler
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
    val expandAppBar = rememberAppBarExpander()
    DisposableEffect(target, state, expandAppBar) {
        val handler: suspend () -> Boolean = {
            val scrolled = state.canScrollBackward
            if (scrolled) currentScrollToTop.value()
            val expanded = expandAppBar()
            scrolled || expanded
        }
        target.bind(handler)
        onDispose { target.unbind(handler) }
    }
}
