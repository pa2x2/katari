package mihon.presentation.kit.window

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * What the kit knows about the window a screen is drawn in. Screens never read the screen size, orientation or
 * configuration themselves: the kit's layouts, navigation, sheets and dialogs adapt from this, and a screen that
 * needs another arrangement gets it as a kit component.
 *
 * [width] is already [WindowWidth.Compact] when the user turned wide layouts off (Settings › Appearance › Tablet UI).
 * The window can change at any time (folding, rotating, resizing a split screen); nothing may assume it is fixed
 * for the life of a screen.
 */
@Immutable
data class WindowLayout(
    val size: DpSize,
    val width: WindowWidth,
    /** Under 480 dp tall, such as a phone or a folded foldable in landscape: headers give up their height first. */
    val isShort: Boolean,
    /** The hinge or fold crossing this window, or null when there is none. */
    val fold: WindowFold?,
    /**
     * Whether Android turns the window to an orientation the app asks for. It doesn't in split screen, nor, from
     * Android 16, on a screen at least 600 dp both ways such as a tablet or an unfolded foldable, so a rotation
     * setting can only apply elsewhere. The window's shorter side stands in for the screen's, as they match whenever
     * the window isn't split.
     */
    val honoursOrientationRequests: Boolean = true,
) {
    val usesSideNavigation: Boolean get() = width != WindowWidth.Compact
    val usesTwoPanes: Boolean get() = width == WindowWidth.Expanded
}

/** How wide the window is, in Material's window size classes. */
enum class WindowWidth { Compact, Medium, Expanded }

/**
 * A fold crossing the window, in the window's coordinates. [start] and [end] bound it along the axis it divides: x for a
 * vertical fold, y for a horizontal one. A flat fold has no width, so both are the fold line; a hinge between two
 * screens has width.
 */
@Immutable
data class WindowFold(
    val isVertical: Boolean,
    val start: Dp,
    val end: Dp,
    /** Bent part way, like a book or a laptop, rather than flat. */
    val isHalfOpen: Boolean,
)

val LocalWindowLayout = staticCompositionLocalOf<WindowLayout?> { null }

/** The window layout [ProvideWindowLayout] gives; previews and compositions without it measure the window themselves. */
@Composable
@ReadOnlyComposable
fun currentWindowLayout(): WindowLayout {
    LocalWindowLayout.current?.let { return it }
    val size =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.let { DpSize(it.width.toDp(), it.height.toDp()) }
        }
    return windowLayoutOf(size, fold = null, wideLayoutsAllowed = true, inMultiWindow = false)
}

internal fun windowLayoutOf(
    size: DpSize,
    fold: WindowFold?,
    wideLayoutsAllowed: Boolean,
    inMultiWindow: Boolean,
): WindowLayout {
    val width = when {
        !wideLayoutsAllowed -> WindowWidth.Compact
        size.width >= ExpandedWidthMin -> WindowWidth.Expanded
        size.width >= MediumWidthMin -> WindowWidth.Medium
        else -> WindowWidth.Compact
    }
    val ignoresOrientationOnLargeScreens = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    return WindowLayout(
        size = size,
        width = width,
        isShort = size.height < ShortHeightMax,
        fold = fold,
        honoursOrientationRequests = !inMultiWindow &&
            !(ignoresOrientationOnLargeScreens && minOf(size.width, size.height) >= LargeScreenShortSideMin),
    )
}

private val MediumWidthMin = 600.dp
private val ExpandedWidthMin = 840.dp
private val ShortHeightMax = 480.dp
private val LargeScreenShortSideMin = 600.dp
