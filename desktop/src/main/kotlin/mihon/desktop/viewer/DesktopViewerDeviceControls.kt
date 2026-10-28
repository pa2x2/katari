package mihon.desktop.viewer

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.viewer.ViewerDeviceControls

/**
 * A desktop window has no volume keys, system bars, orientation, e-ink panel, picture-in-picture or brightness of its
 * own for a viewer to control, and no display cutout.
 */
@Inject
class DesktopViewerDeviceControls : ViewerDeviceControls {
    override val volumeKeys: Boolean = false
    override val systemBars: Boolean = false
    override val screenOrientation: Boolean = false
    override val eInkFlashes: Boolean = false
    override val pictureInPicture: Boolean = false
    override val screenBrightness: Boolean = false

    @Composable
    override fun hasDisplayCutout(): Boolean = false
}
