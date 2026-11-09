package app.katari.presentation.kit.sheet

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.katari.presentation.kit.layout.ApartFromScreen
import app.katari.presentation.kit.list.LocalRowGroupContainer
import app.katari.presentation.kit.list.ProvideRowDensity
import app.katari.presentation.kit.list.RowDensity
import app.katari.presentation.kit.shortcut.KeyboardShortcutLayer
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.window.currentWindowLayout

/**
 * Where the button that opens a [Popover] sits, so the popover opens beside it. The button marks itself with
 * [popoverAnchor]; one anchor per button, such as one for an app bar's avatar and another for the dock's.
 */
@Stable
class PopoverAnchor internal constructor() {
    internal var coordinates: LayoutCoordinates? = null

    /**
     * The button's bounds in the window, or null while it isn't on screen: detached, or clipped away by a parent, such
     * as a pager page beside the shown one.
     */
    internal fun boundsInWindow(): Rect? =
        coordinates?.takeIf { it.isAttached }?.boundsInWindow()?.takeUnless { it.isEmpty }
}

@Composable
fun rememberPopoverAnchor(): PopoverAnchor = remember { PopoverAnchor() }

/** Marks the button that opens a [Popover] with [anchor]. */
fun Modifier.popoverAnchor(anchor: PopoverAnchor): Modifier = onPlaced { anchor.coordinates = it }

/**
 * A short choice opened from a button, such as the profile switcher or an entry's library-update choices. From a medium
 * window on it is a card beside [anchor]'s button, as tall as its content (which scrolls when the window is lower), with
 * nothing dimmed behind it, as a menu opens; a click elsewhere, back or Escape closes it. In a compact window, or when
 * [anchor] is null or its button isn't on screen, such as one in a menu that has closed, it is a [Sheet]. Its content is
 * laid out at compact row density, as in a sheet.
 */
@Composable
fun Popover(
    anchor: PopoverAnchor?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val window = currentWindowLayout()
    // Read as it opens, and again when the window changes and the button may have moved
    val anchorBounds = remember(anchor, window.size) { anchor?.boundsInWindow()?.roundToIntRect() }
    if (!window.usesSideNavigation || anchorBounds == null) {
        Sheet(onDismissRequest = onDismissRequest, modifier = modifier, capHeight = true, content = content)
        return
    }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val spacing = KatariTheme.spacing
    val insets = WindowInsets.safeDrawing
    // The area the popover stays in, in the anchor's window coordinates: the window inside its bars and cutout
    val room = with(density) {
        val margin = spacing.m.roundToPx()
        IntRect(
            left = insets.getLeft(this, layoutDirection) + margin,
            top = insets.getTop(this) + margin,
            right = window.size.width.roundToPx() - insets.getRight(this, layoutDirection) - margin,
            bottom = window.size.height.roundToPx() - insets.getBottom(this) - margin,
        )
    }
    val gap = with(density) { spacing.s.roundToPx() }
    val sideZone = with(density) { PopoverSideZone.roundToPx() }
    val positionProvider = remember(anchorBounds, room, gap, sideZone) {
        BesideAnchor(anchor = anchorBounds, room = room, gap = gap, sideZone = sideZone)
    }
    val maxHeight = with(density) { popoverMaxHeight(anchorBounds, room, gap, sideZone).toDp() }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        androidx.compose.material3.Surface(
            modifier = modifier
                .width(PopoverWidth)
                .heightIn(max = maxHeight),
            shape = KatariTheme.shapes.extraLarge,
            color = KatariTheme.colors.surfaceContainerHigh,
            contentColor = KatariTheme.colors.onSurface,
            shadowElevation = PopoverShadow,
        ) {
            // The popover is its own window over the floating dock and the screen, so a Scaffold or list in it has
            // nothing to clear
            ApartFromScreen {
                CompositionLocalProvider(
                    LocalRowGroupContainer provides SurfaceContainer.Highest,
                    LocalSheetForm provides SheetForm.Popover,
                    LocalSheetColumns provides 1,
                ) {
                    KeyboardShortcutLayer { ProvideRowDensity(RowDensity.Compact, content) }
                }
            }
        }
    }
}

/**
 * Places the popover from the kit's own window room rather than the window size the popup is handed: on Android that
 * is the visible frame, without the bars, while the anchor is in window coordinates, so the two would disagree.
 */
private class BesideAnchor(
    private val anchor: IntRect,
    private val room: IntRect,
    private val gap: Int,
    private val sideZone: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = popoverPosition(anchor, room, popupContentSize, gap, sideZone)
}

/**
 * Where a popover of [popup]'s size goes for a button at [anchor], kept inside [room], all in pixels. Under a button
 * near the top, such as an app bar's; beside one at a side edge, such as the side dock's, so it covers no other dock
 * button; elsewhere under the button, or over it when there is more room above. It lines up with the button's outer
 * side.
 */
internal fun popoverPosition(anchor: IntRect, room: IntRect, popup: IntSize, gap: Int, sideZone: Int): IntOffset {
    val placement = popoverPlacement(anchor, room, sideZone)
    val (x, y) = when (placement) {
        PopoverPlacement.RightOf -> anchor.right + gap to anchor.center.y - popup.height / 2
        PopoverPlacement.LeftOf -> anchor.left - gap - popup.width to anchor.center.y - popup.height / 2
        PopoverPlacement.Under, PopoverPlacement.UnderOrOver -> {
            val x = if (anchor.center.x > room.center.x) anchor.right - popup.width else anchor.left
            val roomBelow = room.bottom - anchor.bottom - gap
            val roomAbove = anchor.top - gap - room.top
            val under = placement == PopoverPlacement.Under || popup.height <= roomBelow || roomBelow >= roomAbove
            val y = if (under) anchor.bottom + gap else anchor.top - gap - popup.height
            x to y
        }
    }
    return IntOffset(
        x = x.coerceIn(room.left, (room.right - popup.width).coerceAtLeast(room.left)),
        y = y.coerceIn(room.top, (room.bottom - popup.height).coerceAtLeast(room.top)),
    )
}

/**
 * The tallest a popover may be, in pixels, to fit where [popoverPosition] puts it without covering its button, such as
 * under an app bar's button in a low window; its content scrolls past that.
 */
internal fun popoverMaxHeight(anchor: IntRect, room: IntRect, gap: Int, sideZone: Int): Int =
    when (popoverPlacement(anchor, room, sideZone)) {
        PopoverPlacement.RightOf, PopoverPlacement.LeftOf -> room.height
        PopoverPlacement.Under -> room.bottom - anchor.bottom - gap
        PopoverPlacement.UnderOrOver -> maxOf(room.bottom - anchor.bottom - gap, anchor.top - gap - room.top)
    }.coerceAtLeast(0)

private enum class PopoverPlacement { Under, RightOf, LeftOf, UnderOrOver }

private fun popoverPlacement(anchor: IntRect, room: IntRect, sideZone: Int): PopoverPlacement = when {
    anchor.center.y < room.top + room.height / TOP_FRACTION -> PopoverPlacement.Under
    anchor.center.x < room.left + sideZone -> PopoverPlacement.RightOf
    anchor.center.x > room.right - sideZone -> PopoverPlacement.LeftOf
    else -> PopoverPlacement.UnderOrOver
}

// A button in the top fifth of the window, such as an app bar's, opens its popover under it
private const val TOP_FRACTION = 5

// About the side dock's width: a button this close to a side edge opens its popover beside it
private val PopoverSideZone = 120.dp

private val PopoverWidth = 360.dp

private val PopoverShadow = 8.dp
