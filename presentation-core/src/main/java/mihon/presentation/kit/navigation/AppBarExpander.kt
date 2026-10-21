package mihon.presentation.kit.navigation

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Grows the enclosing [Scaffold]'s collapsed large title back, for content that returns to its top from code, such as a
 * reselected tab or a back-to-top button. Nested scrolling is what moves the bar, and scrolls made from code bypass it,
 * so without this the title would stay collapsed over content that is back at its top. The call reports whether the bar
 * had to move.
 */
@Composable
fun rememberAppBarExpander(): suspend () -> Boolean {
    val state = LocalAppBarScrollBehavior.current?.state
    return remember(state) {
        val expand: suspend () -> Boolean = {
            if (state == null || state.heightOffset == 0f) {
                false
            } else {
                animate(initialValue = state.heightOffset, targetValue = 0f) { value, _ -> state.heightOffset = value }
                true
            }
        }
        expand
    }
}
