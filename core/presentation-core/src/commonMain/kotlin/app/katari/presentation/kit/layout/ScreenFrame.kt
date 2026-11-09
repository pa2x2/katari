package app.katari.presentation.kit.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.katari.presentation.kit.window.WindowLayout
import app.katari.presentation.kit.window.WindowWidth

/**
 * How wide a screen's content may grow, chosen by what the screen shows. The kit `Scaffold` centres a frame of at most
 * that width in the room beside the dock. The app bar, the kit layouts that hold the screen's content and the main action
 * all line up with the frame, so the title, tabs and list share one edge however wide the window is. A wider window
 * gives a screen more columns or panes, not longer rows.
 */
enum class ScreenFrame {
    /** One column of rows, such as settings, history or updates: as wide as a [RowList]. */
    Rows,

    /**
     * Two panes side by side where the window holds them ([TwoPanes]): a start pane that widens with the window beside
     * a column of rows. A single column of rows otherwise.
     */
    ListDetail,

    /** Content that fills its width with more columns, such as cover grids and card flows, up to [WideFrameMaxWidth]. */
    Wide,

    /** The whole window, for content that lays itself out, such as web pages and pictures, or a shell around screens. */
    Window,
}

/** The widest a [ScreenFrame] gets in [window] with [available] room beside the dock and system bars. */
internal fun ScreenFrame.width(window: WindowLayout, available: Dp): Dp = when (this) {
    ScreenFrame.Rows -> min(available, RowListMaxWidth)
    ScreenFrame.ListDetail -> if (window.usesTwoPanes) {
        min(available, startPaneWidth(window.width, available) + RowListMaxWidth)
    } else {
        min(available, RowListMaxWidth)
    }
    ScreenFrame.Wide -> min(available, WideFrameMaxWidth)
    ScreenFrame.Window -> available
}

/**
 * The start pane of [TwoPanes] across [available]: half of it in an Expanded window, then wider steps for Large and
 * Extra-large ones, so the pane beside it keeps a row column's width rather than taking all the extra room.
 */
internal fun startPaneWidth(width: WindowWidth, available: Dp): Dp = when (width) {
    WindowWidth.Compact, WindowWidth.Medium, WindowWidth.Expanded -> min(available / 2, ExpandedStartPaneWidth)
    WindowWidth.Large -> min(available / 2, LargeStartPaneWidth)
    WindowWidth.ExtraLarge -> min(available / 2, ExtraLargeStartPaneWidth)
}

/**
 * How far the app bar's contents sit in from each side, beyond the system bars, cutout and dock: the margins that
 * centre the screen's frame, and the room of a screen around a `Scaffold` nested in it that it hasn't applied. The
 * `Scaffold` provides it to its top bar only, so a sheet or dialog opened from the content never inherits it.
 */
@Immutable
internal data class FrameMargins(val start: Dp = 0.dp, val end: Dp = 0.dp)

internal val LocalScreenFrameMargin = staticCompositionLocalOf { FrameMargins() }

// Material's Extra-large window class starts here: past it a wider frame would only spread the content apart
private val WideFrameMaxWidth = 1600.dp

private val ExpandedStartPaneWidth = 450.dp
private val LargeStartPaneWidth = 480.dp
private val ExtraLargeStartPaneWidth = 560.dp
