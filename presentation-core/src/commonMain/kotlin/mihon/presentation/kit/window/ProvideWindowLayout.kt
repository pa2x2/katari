package mihon.presentation.kit.window

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize

/**
 * Gives [LocalWindowLayout] to [content] from its window: its size, and the fold the platform reports crossing it. It
 * also records where [content] was last touched, for [rememberFoldHalf]. Nested calls reuse the outer layout, so a
 * theme applied inside another stays cheap.
 */
@Composable
fun ProvideWindowLayout(wideLayoutsAllowed: Boolean, content: @Composable () -> Unit) {
    if (LocalWindowLayout.current != null) {
        content()
        return
    }
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val fold = rememberWindowFold()
    val layout = remember(containerSize, density, fold, wideLayoutsAllowed) {
        val size = with(density) { DpSize(containerSize.width.toDp(), containerSize.height.toDp()) }
        windowLayoutOf(size, fold, wideLayoutsAllowed)
    }
    val lastTouch = remember { LastTouch() }
    CompositionLocalProvider(LocalWindowLayout provides layout, LocalLastTouch provides lastTouch) {
        // Sized by its content, so wrapping it changes no layout
        Box(modifier = with(lastTouch) { Modifier.recordTouches() }, propagateMinConstraints = true) {
            content()
        }
    }
}

/** The fold crossing the window, or null where there is none or the platform has no folds. */
@Composable
internal expect fun rememberWindowFold(): WindowFold?
