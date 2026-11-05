package mihon.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

/**
 * Takes Compose's [window] out of full screen when [state] goes from it to maximized. Compose's window leaves full
 * screen only on its way to floating: asked to maximize, it maximizes under the full screen and stays in it, while
 * the state, and every full-screen button reading it, already says it left. Going through floating doesn't do either:
 * AWT sends the window manager the unmaximize but drops the maximize asked right after, as the window still counts as
 * maximized until the manager answers. So the window leaves the screen device's full screen, which is how Compose's
 * window enters it on Linux, and stays maximized as it was underneath.
 */
@Composable
internal fun LeavesFullScreenToMaximized(window: ComposeWindow, state: WindowState) {
    LaunchedEffect(window) {
        snapshotFlow { state.placement }.collect { placement ->
            val device = window.graphicsConfiguration.device
            if (placement == WindowPlacement.Maximized && device.fullScreenWindow === window) {
                device.fullScreenWindow = null
            }
        }
    }
}
