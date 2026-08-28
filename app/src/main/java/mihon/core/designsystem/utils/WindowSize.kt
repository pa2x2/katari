package mihon.core.designsystem.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

@Composable
@ReadOnlyComposable
fun isMediumWidthWindow(): Boolean {
    val containerWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return containerWidth > MediumWidthWindowSize
}

@Composable
@ReadOnlyComposable
fun isExpandedWidthWindow(): Boolean {
    val containerWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return containerWidth > ExpandedWidthWindowSize
}

val MediumWidthWindowSize = 600.dp
val ExpandedWidthWindowSize = 840.dp
