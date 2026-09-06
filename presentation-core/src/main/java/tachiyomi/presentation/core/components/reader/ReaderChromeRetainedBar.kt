package tachiyomi.presentation.core.components.reader

import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Moves cached toolbar content outside the viewport without rebuilding it on every reader tap. */
@Composable
internal fun ReaderChromeRetainedBar(
    transition: Transition<Boolean>,
    fromTop: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var height by remember { mutableIntStateOf(0) }
    val position by transition.animateFloat(transitionSpec = { tween(200) }, label = "retainedBarPosition") {
        if (it) 0f else 1f
    }
    val opacity by transition.animateFloat(transitionSpec = { tween(150) }, label = "retainedBarOpacity") {
        if (it) 1f else 0f
    }
    Box(
        modifier = modifier
            .onSizeChanged { height = it.height }
            .graphicsLayer {
                alpha = opacity
                translationY = position * height * if (fromTop) -1f else 1f
            }
            .focusProperties { canFocus = transition.targetState }
            .then(if (transition.targetState) Modifier else Modifier.clearAndSetSemantics { }),
    ) {
        content()
    }
}
