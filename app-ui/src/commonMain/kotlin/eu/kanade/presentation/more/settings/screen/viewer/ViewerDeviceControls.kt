package eu.kanade.presentation.more.settings.screen.viewer

import androidx.compose.runtime.Composable
import mihon.core.common.platform.PlatformFacility

/**
 * The parts of the device that reader and player settings control. Where the platform lacks one, the rows for it are
 * hidden.
 */
@PlatformFacility
interface ViewerDeviceControls {
    /** Hardware volume keys, which can turn pages. */
    val volumeKeys: Boolean

    /** The system's status and navigation bars, which viewers can show or hide. */
    val systemBars: Boolean

    /**
     * A floating toolbar over selected text, whose system actions a reader can leave out. Where the platform has none,
     * a right click on the selection offers the reader's own actions.
     */
    val textSelectionToolbar: Boolean

    /** Turning the screen to the orientation a viewer asks for. */
    val screenOrientation: Boolean

    /** Flashing the screen between pages, which clears the ghosting of e-ink panels. */
    val eInkFlashes: Boolean

    /** Moving the player into the system's picture-in-picture by itself when the app is left. */
    val pictureInPicture: Boolean

    /**
     * A setting for whether the player keeps playing out of sight. Where false the platform has no such choice to
     * make, as on desktop, where a hidden or minimised window plays on.
     */
    val backgroundPlayback: Boolean

    /** Changing the screen's brightness, as the player's gestures do. */
    val screenBrightness: Boolean

    /** Whether the window it is composed in has a display cutout that pages can be drawn under. */
    @Composable
    fun hasDisplayCutout(): Boolean
}
