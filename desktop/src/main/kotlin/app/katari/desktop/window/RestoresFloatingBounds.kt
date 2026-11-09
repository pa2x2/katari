package app.katari.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.flow.first
import java.awt.GraphicsEnvironment
import java.awt.Window

/**
 * Puts Compose's [window], opened maximized, where it last floated the first time it leaves maximized for floating. It
 * opens at the bounds it takes maximized ([rememberSavedWindowState]), which the window manager keeps as the bounds to
 * restore it to, so it would restore to a window as large as the screen. A place on no screen there is now keeps the
 * size only, where the window manager puts it.
 */
@Composable
internal fun RestoresFloatingBounds(window: Window, state: WindowState, preferences: WindowPreferences) {
    // Read as the window opens, before leaving maximized saves the bounds the window manager restored it to
    val floating = remember { preferences.floatingBounds.get() }
    LaunchedEffect(window) {
        if (floating == null || state.placement != WindowPlacement.Maximized) return@LaunchedEffect
        snapshotFlow { state.placement }.first { it == WindowPlacement.Floating }
        val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            .map { it.defaultConfiguration.bounds }
        if (isReachable(floating, screens)) {
            window.bounds = floating
        } else {
            window.setSize(floating.width, floating.height)
        }
    }
}
