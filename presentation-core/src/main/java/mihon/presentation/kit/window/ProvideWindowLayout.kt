package mihon.presentation.kit.window

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/**
 * Gives [LocalWindowLayout] to [content] from the activity's window: its size, and the fold Jetpack WindowManager
 * reports crossing it. It also gives the window's last touch, for [rememberFoldHalf]. Nested calls reuse
 * the outer layout, so a theme applied inside another stays cheap.
 */
@Composable
fun ProvideWindowLayout(wideLayoutsAllowed: Boolean, content: @Composable () -> Unit) {
    if (LocalWindowLayout.current != null) {
        content()
        return
    }
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val activity = LocalActivity.current
    val fold by produceState<FoldingFeature?>(initialValue = null, activity) {
        if (activity == null) return@produceState
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { info ->
            value = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
        }
    }
    val layout = remember(containerSize, density, fold, wideLayoutsAllowed) {
        val size = with(density) { DpSize(containerSize.width.toDp(), containerSize.height.toDp()) }
        windowLayoutOf(
            size = size,
            fold = fold?.toWindowFold(density),
            wideLayoutsAllowed = wideLayoutsAllowed,
            // Entering or leaving split screen resizes the window, so the size key reads this again
            inMultiWindow = activity?.isInMultiWindowMode == true,
        )
    }
    val lastTouch = remember(activity) { activity?.window?.let(LastTouch::of) }
    CompositionLocalProvider(LocalWindowLayout provides layout, LocalLastTouch provides lastTouch, content = content)
}

private fun FoldingFeature.toWindowFold(density: Density): WindowFold = with(density) {
    val isVertical = orientation == FoldingFeature.Orientation.VERTICAL
    WindowFold(
        isVertical = isVertical,
        start = (if (isVertical) bounds.left else bounds.top).toDp(),
        end = (if (isVertical) bounds.right else bounds.bottom).toDp(),
        isHalfOpen = state == FoldingFeature.State.HALF_OPENED,
    )
}
