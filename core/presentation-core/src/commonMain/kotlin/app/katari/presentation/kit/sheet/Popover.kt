package app.katari.presentation.kit.sheet

import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.katari.presentation.kit.list.LocalRowGroupContainer
import app.katari.presentation.kit.list.ProvideRowDensity
import app.katari.presentation.kit.list.RowDensity
import app.katari.presentation.kit.navigation.DockClearance
import app.katari.presentation.kit.navigation.LocalDockClearance
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

    /** The button's bounds in the window, or null while it isn't on screen. */
    internal fun boundsInWindow(): Rect? = coordinates?.takeIf { it.isAttached }?.boundsInWindow()
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
    val spacing = KatariTheme.spacing
    val positionProvider = remember(anchorBounds, density) {
        with(density) {
            BesideAnchor(
                anchor = anchorBounds,
                gap = spacing.s.roundToPx(),
                margin = spacing.m.roundToPx(),
                sideZone = PopoverSideZone.roundToPx(),
            )
        }
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        androidx.compose.material3.Surface(
            modifier = modifier
                .width(PopoverWidth)
                .heightIn(max = window.size.height - spacing.m * 2),
            shape = KatariTheme.shapes.extraLarge,
            color = KatariTheme.colors.surfaceContainerHigh,
            contentColor = KatariTheme.colors.onSurface,
            shadowElevation = PopoverShadow,
        ) {
            // The popover is its own window over the floating dock, so a Scaffold in it has nothing to clear
            CompositionLocalProvider(
                LocalRowGroupContainer provides SurfaceContainer.Highest,
                LocalDockClearance provides DockClearance.None,
                LocalSheetForm provides SheetForm.Popover,
                LocalSheetColumns provides 1,
            ) {
                KeyboardShortcutLayer { ProvideRowDensity(RowDensity.Compact, content) }
            }
        }
    }
}

private class BesideAnchor(
    private val anchor: IntRect,
    private val gap: Int,
    private val margin: Int,
    private val sideZone: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = popoverPosition(anchor, windowSize, popupContentSize, gap, margin, sideZone)
}

/**
 * Where a popover of [popup]'s size goes for a button at [anchor] in a window of [window]'s size, all in pixels. Under a
 * button near the top, such as an app bar's; beside one at a side edge, such as the side dock's, so it covers no other
 * dock button; elsewhere under the button, or over it when there is more room above. It lines up with the button's
 * outer side and stays [margin] inside the window.
 */
internal fun popoverPosition(
    anchor: IntRect,
    window: IntSize,
    popup: IntSize,
    gap: Int,
    margin: Int,
    sideZone: Int,
): IntOffset {
    val nearTop = anchor.center.y < window.height / TOP_FRACTION
    val atLeft = anchor.center.x < sideZone
    val atRight = anchor.center.x > window.width - sideZone
    val (x, y) = when {
        !nearTop && atLeft -> anchor.right + gap to anchor.center.y - popup.height / 2
        !nearTop && atRight -> anchor.left - gap - popup.width to anchor.center.y - popup.height / 2
        else -> {
            val x = if (anchor.center.x > window.width / 2) anchor.right - popup.width else anchor.left
            val roomBelow = window.height - margin - anchor.bottom - gap
            val roomAbove = anchor.top - gap - margin
            val y = if (nearTop || popup.height <= roomBelow || roomBelow >= roomAbove) {
                anchor.bottom + gap
            } else {
                anchor.top - gap - popup.height
            }
            x to y
        }
    }
    return IntOffset(
        x = x.coerceIn(margin, (window.width - margin - popup.width).coerceAtLeast(margin)),
        y = y.coerceIn(margin, (window.height - margin - popup.height).coerceAtLeast(margin)),
    )
}

// A button in the top fifth of the window, such as an app bar's, opens its popover under it
private const val TOP_FRACTION = 5

// About the side dock's width: a button this close to a side edge opens its popover beside it
private val PopoverSideZone = 120.dp

private val PopoverWidth = 360.dp

private val PopoverShadow = 8.dp
