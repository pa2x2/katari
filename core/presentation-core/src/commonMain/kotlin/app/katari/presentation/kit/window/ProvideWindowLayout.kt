package app.katari.presentation.kit.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize

/**
 * Gives [LocalWindowLayout] to [content] from its window: its size, and the fold the platform reports crossing it. It
 * also gives the window's last touch, for [rememberFoldHalf]. Nested calls reuse the outer layout, so a theme applied
 * inside another stays cheap. [content] waits for the window to have a size: Compose on desktop composes a window's
 * content as it attaches it, before it reads the window's size, and screens laid out for no size would open in the
 * compact layout and change to the window's own on its first frames, its dock moving from the bottom to the side.
 */
@Composable
fun ProvideWindowLayout(wideLayoutsAllowed: Boolean, content: @Composable () -> Unit) {
    if (LocalWindowLayout.current != null) {
        content()
        return
    }
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    if (containerSize == IntSize.Zero) return
    val fold = rememberWindowFold()
    val inMultiWindow = rememberMultiWindowCheck()
    val layout = remember(containerSize, density, fold, wideLayoutsAllowed) {
        val size = with(density) { DpSize(containerSize.width.toDp(), containerSize.height.toDp()) }
        windowLayoutOf(
            size = size,
            fold = fold,
            wideLayoutsAllowed = wideLayoutsAllowed,
            // Entering or leaving split screen resizes the window, so the size key reads this again
            inMultiWindow = inMultiWindow(),
        )
    }
    val lastTouch = rememberWindowLastTouch()
    CompositionLocalProvider(LocalWindowLayout provides layout, LocalLastTouch provides lastTouch, content = content)
}

/** The fold crossing the window, or null where there is none or the platform has no folds. */
@Composable
internal expect fun rememberWindowFold(): WindowFold?

/** Reads whether the window shares the screen with other apps' windows in a split the platform manages. */
@Composable
internal expect fun rememberMultiWindowCheck(): () -> Boolean

@Composable
internal expect fun rememberWindowLastTouch(): LastTouch?
