package mihon.desktop.window

import java.awt.Rectangle

/**
 * Whether a window put back at [bounds] can still be seen and dragged: enough of its title bar, the top
 * [TITLE_BAR_HEIGHT] of it, lies on one of [screens]. A screen unplugged or moved since the window was there can leave
 * it on none, and the window manager then places it instead.
 */
internal fun isReachable(bounds: Rectangle, screens: List<Rectangle>): Boolean {
    val titleBar = Rectangle(bounds.x, bounds.y, bounds.width, minOf(bounds.height, TITLE_BAR_HEIGHT))
    return screens.any { screen ->
        val visible = titleBar.intersection(screen)
        !visible.isEmpty && visible.width >= MIN_GRAB_WIDTH && visible.height >= MIN_GRAB_HEIGHT
    }
}

// In AWT units, small enough for any window frame's title bar and large enough to grab with a pointer
private const val TITLE_BAR_HEIGHT = 32
private const val MIN_GRAB_WIDTH = 64
private const val MIN_GRAB_HEIGHT = 16
