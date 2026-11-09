package app.katari.desktop.window

import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Toolkit

/**
 * Where a window maximized from [floating] lies, in AWT's units: the work area of the screen its title bar is on, what
 * the screen's panels and docks leave free, which the window manager maximizes a window to. The main screen's where
 * [floating] is on none, or null.
 */
internal fun maximizedBounds(floating: Rectangle?): Rectangle {
    val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val screen = floating?.let { bounds ->
        environment.screenDevices.map { it.defaultConfiguration }.firstOrNull { isReachable(bounds, listOf(it.bounds)) }
    } ?: environment.defaultScreenDevice.defaultConfiguration
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(screen)
    return screen.bounds.apply {
        x += insets.left
        y += insets.top
        width -= insets.left + insets.right
        height -= insets.top + insets.bottom
    }
}
