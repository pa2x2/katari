package mihon.presentation.kit.dialog

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.surface.HorizontalDivider

/**
 * A dialog's scrolling [content], such as a long list of choices, with a line along each edge that has more past it,
 * so a list cut off by the dialog's height reads as continuing. [state] is the scroll state of the list in [content].
 */
@Composable
fun DialogScrollEdges(state: ScrollableState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier) {
        content()
        if (state.canScrollBackward) HorizontalDivider(Modifier.align(Alignment.TopCenter))
        if (state.canScrollForward) HorizontalDivider(Modifier.align(Alignment.BottomCenter))
    }
}
