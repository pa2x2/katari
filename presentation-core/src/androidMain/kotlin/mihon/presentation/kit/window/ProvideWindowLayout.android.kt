package mihon.presentation.kit.window

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/** The fold Jetpack WindowManager reports crossing the activity's window. */
@Composable
internal actual fun rememberWindowFold(): WindowFold? {
    val density = LocalDensity.current
    val activity = LocalActivity.current
    val fold by produceState<FoldingFeature?>(initialValue = null, activity) {
        if (activity == null) return@produceState
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { info ->
            value = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
        }
    }
    return fold?.toWindowFold(density)
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
