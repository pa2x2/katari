package mihon.desktop.window

import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import mihon.presentation.kit.window.WindowFullScreen

/**
 * The window's full screen as its [WindowState]'s placement, entered by F11 and by screens alike, so leaving puts
 * back the placement it had before, maximized or floating, whichever asked.
 */
class PlacementFullScreen(private val state: WindowState) : WindowFullScreen {
    private var placementBefore = WindowPlacement.Floating

    override val isFullScreen: Boolean
        get() = state.placement == WindowPlacement.Fullscreen

    override fun enter() {
        if (isFullScreen) return
        placementBefore = state.placement
        state.placement = WindowPlacement.Fullscreen
    }

    override fun leave() {
        if (isFullScreen) state.placement = placementBefore
    }
}
