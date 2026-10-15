package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** How wide the window is, in Material's window size classes, for layouts that change with the room they have. */
enum class WindowWidth { Compact, Medium, Expanded }

@Composable
@ReadOnlyComposable
fun currentWindowWidth(): WindowWidth {
    val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return when {
        width > ExpandedWidthMin -> WindowWidth.Expanded
        width > MediumWidthMin -> WindowWidth.Medium
        else -> WindowWidth.Compact
    }
}

private val MediumWidthMin = 600.dp
private val ExpandedWidthMin = 840.dp
